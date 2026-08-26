package com.example.ui.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

enum class WellbeingChoice(val label: String) {
    WELL("أنا بخير"),
    TIRED("متعب قليلاً"),
    PAIN("عندي ألم"),
    NEED_HELP("أحتاج مساعدة"),
    URGENT("عندي أعراض مقلقة")
}

data class CareLoopUiState(
    val lastCheckIn: WellbeingChoice? = null,
    val lastCheckInAt: Long = 0L,
    val weeklyReviewCompletedAt: Long = 0L,
    val remindersEnabled: Boolean = true
) {
    val checkedInToday: Boolean
        get() = lastCheckInAt > 0 && sameDay(lastCheckInAt, System.currentTimeMillis())

    val weeklyReviewDue: Boolean
        get() = weeklyReviewCompletedAt == 0L ||
            System.currentTimeMillis() - weeklyReviewCompletedAt >= 6L * 24L * 60L * 60L * 1000L

    companion object {
        private fun sameDay(first: Long, second: Long): Boolean {
            val a = Calendar.getInstance().apply { timeInMillis = first }
            val b = Calendar.getInstance().apply { timeInMillis = second }
            return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
        }
    }
}

class CareLoopViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("sehati_care_loop", Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(CareLoopUiState())
    val uiState: StateFlow<CareLoopUiState> = _uiState.asStateFlow()
    private var userKey = "guest"

    fun bindUser(uid: String) {
        val next = uid.ifBlank { "guest" }
        if (next == userKey && _uiState.value.lastCheckInAt != 0L) return
        userKey = next
        _uiState.value = CareLoopUiState(
            lastCheckIn = preferences.getString("${userKey}_check_in", null)?.let {
                runCatching { WellbeingChoice.valueOf(it) }.getOrNull()
            },
            lastCheckInAt = preferences.getLong("${userKey}_check_in_at", 0L),
            weeklyReviewCompletedAt = preferences.getLong("${userKey}_weekly_at", 0L),
            remindersEnabled = preferences.getBoolean("${userKey}_reminders", true)
        )
        CareReminderScheduler.update(getApplication(), _uiState.value.remindersEnabled)
    }

    fun recordCheckIn(choice: WellbeingChoice) {
        val now = System.currentTimeMillis()
        preferences.edit()
            .putString("${userKey}_check_in", choice.name)
            .putLong("${userKey}_check_in_at", now)
            .apply()
        _uiState.value = _uiState.value.copy(lastCheckIn = choice, lastCheckInAt = now)
    }

    fun completeWeeklyReview() {
        val now = System.currentTimeMillis()
        preferences.edit().putLong("${userKey}_weekly_at", now).apply()
        _uiState.value = _uiState.value.copy(weeklyReviewCompletedAt = now)
    }

    fun setRemindersEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("${userKey}_reminders", enabled).apply()
        _uiState.value = _uiState.value.copy(remindersEnabled = enabled)
        CareReminderScheduler.update(getApplication(), enabled)
    }
}

object CareReminderScheduler {
    const val EXTRA_OPEN_CARE = "open_daily_care"
    const val REQUEST_CODE = 8412

    fun update(context: Context, enabled: Boolean) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CareReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            CareReminderScheduler.REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarm.cancel(pending)
        if (!enabled) return

        val first = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, first, AlarmManager.INTERVAL_DAY, pending)
    }
}

class CareReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val manager = NotificationManagerCompat.from(context)
        val channelId = "daily_care"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName("المتابعة الصحية")
                    .setDescription("تذكيرات خفيفة للمتابعة والمراجعة الأسبوعية")
                    .build()
            )
        }

        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val (title, body) = when {
            day == Calendar.FRIDAY -> "مراجعتك الأسبوعية" to "خلينا نراجع أسبوعك بهدوء ونرتب الخطوة الجاية."
            day == Calendar.SUNDAY -> "ابدأ أسبوعك بلطف" to "هل عندك موعد أو متابعة صحية محتاج ترتبها؟"
            else -> "كيف حالك اليوم؟" to "اطمئن على نفسك في أقل من دقيقة."
        }
        val openApp = Intent(context, MainActivity::class.java).apply {
            putExtra(CareReminderScheduler.EXTRA_OPEN_CARE, true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            CareReminderScheduler.REQUEST_CODE,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(CareReminderScheduler.REQUEST_CODE, notification)
        } catch (_: SecurityException) {
            // Android 13+ users may decline notification permission; in-app care remains available.
        }
    }
}
