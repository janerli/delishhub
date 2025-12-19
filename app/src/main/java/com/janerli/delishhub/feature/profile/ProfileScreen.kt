package com.janerli.delishhub.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.janerli.delishhub.core.navigation.Routes
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.core.ui.MainScaffold

@Composable
fun ProfileScreen(navController: NavHostController) {

    val session by SessionManager.session.collectAsStateWithLifecycle()
    val isGuest = session.isGuest
    val isAdmin = session.isAdmin

    val displayName = if (isGuest) "Гость" else session.name.ifBlank { "Пользователь" }
    val username = if (isGuest) "@guest" else "@${session.userId.take(8)}"
    val email = if (isGuest) "—" else "—" // подключим после Firebase Auth

    // демо-статы (подключим к Room позже)
    val myRecipesCount = if (isGuest) 0 else 0
    val favoritesCount = if (isGuest) 0 else 0
    val plannedCount = if (isGuest) 0 else 0

    var showDialog by remember { mutableStateOf(false) }
    var dialogText by remember { mutableStateOf("") }

    MainScaffold(
        navController = navController,
        title = "Профиль",
        showBack = false
    ) { padding: PaddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // --- Верхняя карточка профиля ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Card(
                                modifier = Modifier
                                    .height(56.dp)
                                    .aspectRatio(1f)
                                    .clip(CircleShape),
                                shape = CircleShape,
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = null)
                                }
                            }

                            Spacer(modifier = Modifier.padding(8.dp))

                            Column {
                                Text(displayName, style = MaterialTheme.typography.titleMedium)
                                Text(username, style = MaterialTheme.typography.bodyMedium)
                                Text(email, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        IconButton(onClick = {
                            dialogText =
                                "Редактирование профиля будет доступно после подключения Firebase Auth (шаг 6)."
                            showDialog = true
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                    }

                    if (isGuest) {
                        Text(
                            "Сейчас вы в режиме гостя. Войдите, чтобы сохранять рецепты, планы и избранное.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // --- Статистика ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(title = "Мои", value = myRecipesCount.toString())
                    StatItem(title = "Избранное", value = favoritesCount.toString())
                    StatItem(title = "Планы", value = plannedCount.toString())
                }
            }

            // --- Быстрые действия ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    if (!isGuest) {
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { navController.navigate(Routes.MY_RECIPES) }
                        ) {
                            Icon(Icons.Filled.RestaurantMenu, contentDescription = null)
                            Text(" Мои рецепты")
                        }

                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { navController.navigate(Routes.FAVORITES) }
                        ) {
                            Icon(Icons.Filled.Favorite, contentDescription = null)
                            Text(" Избранное")
                        }
                    }

                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { navController.navigate(Routes.SETTINGS) }
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                        Text(" Настройки")
                    }

                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { navController.navigate(Routes.EXPORT) }
                    ) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null)
                        Text(" Экспорт / Поделиться")
                    }

                    if (isAdmin) {
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { navController.navigate(Routes.ADMIN) }
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
                            Text(" Admin")
                        }
                    }

                    // --- ВХОД ---
                    if (isGuest) {
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { navController.navigate(Routes.LOGIN) }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                            Text(" Войти")
                        }
                    }

                    // ✅ --- ВЫХОД: ВСЕГДА ---
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            SessionManager.signOut()
                            navController.navigate(Routes.SPLASH) {
                                popUpTo(0) { inclusive = true } // полностью чистим back stack
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Text(" Выйти")
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Информация") },
                text = { Text(dialogText) },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) { Text("ОК") }
                }
            )
        }
    }
}

@Composable
private fun StatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(title, style = MaterialTheme.typography.bodyMedium)
    }
}
