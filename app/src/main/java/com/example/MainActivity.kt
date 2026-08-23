package com.example

import android.graphics.Color
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.navigation.SehatiApp
import com.example.ui.theme.SehatiTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.SehatiViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SehatiViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val openDailyCare = mutableStateOf(false)
    private val openOrderId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.ui.viewmodel.OrderNotificationHelper.ensureChannel(this)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        openDailyCare.value = intent.getBooleanExtra(
            com.example.ui.viewmodel.CareReminderScheduler.EXTRA_OPEN_CARE,
            false
        )
        openOrderId.value = intent.getStringExtra(
            com.example.ui.viewmodel.OrderNotificationHelper.EXTRA_ORDER_ID
        )
        intent.dataString?.let { authViewModel.handleEmailLink(it, this) }
        // enableEdgeToEdge() defaults to `auto`, which picks icon colours from the system night
        // setting. SehatiTheme is light-only, so on a phone in dark mode that painted white
        // status-bar icons onto our white background and the clock and signal bars vanished.
        // light() pins dark icons to match the surface we actually draw.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        setContent {
            SehatiTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    SehatiApp(
                        authViewModel = authViewModel,
                        viewModel = viewModel,
                        activity = this,
                        openDailyCare = openDailyCare.value,
                        onDailyCareOpened = { openDailyCare.value = false },
                        openOrderId = openOrderId.value,
                        onOrderIdOpened = { openOrderId.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(com.example.ui.viewmodel.CareReminderScheduler.EXTRA_OPEN_CARE, false)) {
            openDailyCare.value = true
        }
        intent.getStringExtra(com.example.ui.viewmodel.OrderNotificationHelper.EXTRA_ORDER_ID)?.let {
            openOrderId.value = it
        }
        intent.dataString?.let { authViewModel.handleEmailLink(it, this) }
    }
}
