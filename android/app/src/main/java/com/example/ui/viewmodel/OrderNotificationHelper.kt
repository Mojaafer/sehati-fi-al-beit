package com.example.ui.viewmodel

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object OrderNotificationHelper {
    const val CHANNEL_ID = "sehati_order_updates"
    const val EXTRA_ORDER_ID = "open_order_id"

    fun ensureChannel(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannelCompat.Builder(
                CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_HIGH
            )
                .setName("تحديثات الطلبات والزيارات")
                .setDescription("إشعارات فورية عند وصول طلب جديد أو قبول الزيارة أو اعتماد الدفع")
                .setVibrationEnabled(true)
                .setShowBadge(true)
                .build()
            manager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        orderId: String = ""
    ) {
        ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (orderId.isNotEmpty()) {
                putExtra(EXTRA_ORDER_ID, orderId)
            }
        }

        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            manager.notify(notificationId, notification)
        } catch (_: SecurityException) {
            // User may have declined notification permission on Android 13+
        }
    }
}
