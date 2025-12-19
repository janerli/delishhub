package com.janerli.delishhub.feature.profile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.work.WorkManager
import com.janerli.delishhub.core.notifications.NotificationsPrefs
import com.janerli.delishhub.core.notifications.NotificationsScheduler
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.core.ui.MainScaffold
import com.janerli.delishhub.core.ui.theme.ThemeMode
import com.janerli.delishhub.core.ui.theme.ThemeStore
import com.janerli.delishhub.data.notifications.MealPlanReminderScheduler
import java.time.LocalDate

@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val mode by ThemeStore.mode.collectAsState()
    val session by SessionManager.session.collectAsState()

    val isAdmin = session.isAdmin

    var notifEnabled by remember { mutableStateOf(NotificationsPrefs.isEnabled(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            notifEnabled = false
            NotificationsPrefs.setEnabled(context, false)
            cancelAllMealPlanNotifications(context)
        } else {
            enableMealPlanNotifications(context)
        }
    }

    MainScaffold(
        navController = navController,
        title = "Настройки",
        showBack = true,
        onBack = { navController.popBackStack() }
    ) { padding: PaddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ---------- ТЕМА ----------
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Тема")

                    ThemeRow(
                        title = "Как в системе",
                        selected = mode == ThemeMode.SYSTEM,
                        onSelect = { ThemeStore.setMode(ThemeMode.SYSTEM) }
                    )
                    ThemeRow(
                        title = "Светлая",
                        selected = mode == ThemeMode.LIGHT,
                        onSelect = { ThemeStore.setMode(ThemeMode.LIGHT) }
                    )
                    ThemeRow(
                        title = "Тёмная",
                        selected = mode == ThemeMode.DARK,
                        onSelect = { ThemeStore.setMode(ThemeMode.DARK) }
                    )
                }
            }

            // ---------- УВЕДОМЛЕНИЯ ----------
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Уведомления")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Напоминания о плане питания")
                        Switch(
                            checked = notifEnabled,
                            onCheckedChange = { enabled ->
                                notifEnabled = enabled
                                NotificationsPrefs.setEnabled(context, enabled)

                                if (enabled) {
                                    ensurePermissionThenEnable(
                                        context = context,
                                        launcher = permissionLauncher,
                                        onDenied = {
                                            notifEnabled = false
                                            NotificationsPrefs.setEnabled(context, false)
                                        }
                                    )
                                } else {
                                    cancelAllMealPlanNotifications(context)
                                }
                            }
                        )
                    }

                    // ✅ КНОПКА ТЕСТА — ТОЛЬКО ДЛЯ АДМИНА
                    if (isAdmin) {
                        Button(
                            onClick = {
                                NotificationsScheduler.enqueueTestNow(context)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Тест уведомления (admin)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeRow(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title)
        RadioButton(selected = selected, onClick = onSelect)
    }
}

private fun ensurePermissionThenEnable(
    context: Context,
    launcher: ActivityResultLauncher<String>,
    onDenied: () -> Unit
) {
    if (Build.VERSION.SDK_INT >= 33) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
    }
    enableMealPlanNotifications(context)
}

private fun enableMealPlanNotifications(context: Context) {
    MealPlanReminderScheduler.scheduleDaily(context)
    MealPlanReminderScheduler.scheduleRefreshNow(context)
}

private fun cancelAllMealPlanNotifications(context: Context) {
    MealPlanReminderScheduler.cancelDaily(context)

    val wm = WorkManager.getInstance(context)
    wm.cancelUniqueWork("mealplan_reminder_refresh_now")

    val today = LocalDate.now()
    val days = listOf(today, today.plusDays(1))
    val types = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")
    for (d in days) {
        val epochDay = d.toEpochDay()
        for (t in types) {
            MealPlanReminderScheduler.cancelSlot(context, epochDay, t)
        }
    }

    wm.cancelUniqueWork("mealplan_reminder_daily")
}
