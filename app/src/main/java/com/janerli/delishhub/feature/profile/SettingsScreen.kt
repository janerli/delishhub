package com.janerli.delishhub.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.janerli.delishhub.core.ui.MainScaffold
import com.janerli.delishhub.core.ui.theme.ThemeMode
import com.janerli.delishhub.core.ui.theme.ThemeStore

@Composable
fun SettingsScreen(navController: NavHostController) {
    val mode by ThemeStore.mode.collectAsState()

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

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Тема", modifier = Modifier.padding(bottom = 4.dp))

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

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Уведомления")
                    Text("Локальные уведомления работают через WorkManager. FCM подключим на шаге 6 (в планах).")
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
