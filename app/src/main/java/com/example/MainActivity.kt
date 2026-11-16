package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.preferences.AppThemeMode
import com.example.ui.AppScreen
import com.example.ui.ShopViewModel
import com.example.ui.components.WhatsAppPreviewDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.JobDetailScreen
import com.example.ui.screens.KhataBookScreen
import com.example.ui.screens.KhataDetailScreen
import com.example.ui.screens.NewJobScreen
import com.example.ui.screens.ReceiptPreviewScreen
import com.example.ui.screens.SettingsHomeScreen
import com.example.ui.screens.ShopSettingsScreen
import com.example.ui.screens.SyncManagerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {

    private var pendingNotificationJobId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannels(this)
        pendingNotificationJobId = intent?.getLongExtra(NotificationHelper.EXTRA_OPEN_JOB_ID, -1L) ?: -1L

        setContent {
            val viewModel: ShopViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            val context = LocalContext.current
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                viewModel.setNotificationsEnabled(granted)
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionStatus = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                    if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                if (pendingNotificationJobId > 0L) {
                    viewModel.openJobFromNotification(pendingNotificationJobId)
                    pendingNotificationJobId = -1L
                }
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner, viewModel) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> viewModel.setAppForeground(true)
                            Lifecycle.Event.ON_PAUSE -> viewModel.setAppForeground(false)
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        viewModel.setAppForeground(false)
                    }
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (viewModel.currentScreen) {
                        AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                        AppScreen.NEW_JOB -> NewJobScreen(viewModel = viewModel)
                        AppScreen.JOB_DETAIL -> JobDetailScreen(viewModel = viewModel)
                        AppScreen.KHATA_BOOK -> KhataBookScreen(viewModel = viewModel)
                        AppScreen.KHATA_DETAIL -> KhataDetailScreen(viewModel = viewModel)
                        AppScreen.RECEIPT_PREVIEW -> ReceiptPreviewScreen(viewModel = viewModel)
                        AppScreen.SETTINGS_HOME -> SettingsHomeScreen(viewModel = viewModel)
                        AppScreen.SHOP_SETTINGS -> ShopSettingsScreen(viewModel = viewModel)
                        AppScreen.SYNC_MANAGER -> SyncManagerScreen(viewModel = viewModel)
                    }
                }

                if (viewModel.showWhatsAppPreviewDialog) {
                    WhatsAppPreviewDialog(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val jobId = intent.getLongExtra(NotificationHelper.EXTRA_OPEN_JOB_ID, -1L)
        if (jobId > 0L) {
            pendingNotificationJobId = jobId
        }
    }
}
