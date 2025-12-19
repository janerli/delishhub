package com.janerli.delishhub.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.SubcomposeAsyncImage
import com.janerli.delishhub.core.di.AppGraph
import com.janerli.delishhub.core.navigation.Routes
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.core.ui.MainScaffold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun ProfileScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val session by SessionManager.session.collectAsStateWithLifecycle()
    val isGuest = session.isGuest
    val isAdmin = session.isAdmin
    val userId = session.userId
    val userKey = if (isGuest) "guest" else userId

    // ✅ профиль должен инициализироваться на текущего пользователя
    ProfileStore.init(context, userKey)

    val avatarPath by ProfileStore.avatarPath.collectAsStateWithLifecycle()
    val guestName by ProfileStore.guestDisplayName.collectAsStateWithLifecycle()

    val displayName = if (isGuest) {
        guestName?.takeIf { it.isNotBlank() } ?: "Гость"
    } else session.name.ifBlank { "Пользователь" }

    val todayEpochDay = remember { LocalDate.now().toEpochDay() }

    val myRecipesCount by AppGraph.recipeDao
        .observeAllBase()
        .map { list -> list.count { it.ownerId == userId && it.syncStatus != 3 } }
        .collectAsStateWithLifecycle(initialValue = 0)

    val favoritesCount by AppGraph.favoriteDao
        .observeFavoriteRecipeIds(userId)
        .map { it.size }
        .collectAsStateWithLifecycle(initialValue = 0)

    val plannedCount by AppGraph.mealPlanDao
        .observeDay(userId, todayEpochDay)
        .map { it.size }
        .collectAsStateWithLifecycle(initialValue = 0)

    val cs = MaterialTheme.colorScheme

    MainScaffold(
        navController = navController,
        title = "Профиль",
        showBack = false
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = cs.surface)
                    ) {
                        if (!avatarPath.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = avatarPath,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isGuest) "Гостевой режим" else (session.email ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { navController.navigate(Routes.PROFILE_EDIT) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Редактировать")
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = cs.surfaceVariant,
                        contentColor = cs.onSurface
                    ),
                    onClick = { navController.navigate(Routes.MY_RECIPES) }
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, null)
                    Text(" $myRecipesCount")
                }

                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = cs.surfaceVariant,
                        contentColor = cs.onSurface
                    ),
                    onClick = { navController.navigate(Routes.FAVORITES) }
                ) {
                    Icon(Icons.Filled.Favorite, null)
                    Text(" $favoritesCount")
                }
            }

            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = cs.surfaceVariant,
                    contentColor = cs.onSurface
                ),
                onClick = { navController.navigate(Routes.PLANNER) }
            ) {
                Text("Планов сегодня: $plannedCount")
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = cs.surface,
                            contentColor = cs.onSurface
                        ),
                        onClick = { navController.navigate(Routes.EXPORT) }
                    ) {
                        Icon(Icons.Filled.FileUpload, null)
                        Text(" Экспорт")
                    }

                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = cs.surface,
                            contentColor = cs.onSurface
                        ),
                        onClick = { navController.navigate(Routes.SETTINGS) }
                    ) {
                        Icon(Icons.Filled.Settings, null)
                        Text(" Настройки")
                    }

                    if (isAdmin) {
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = cs.surface,
                                contentColor = cs.onSurface
                            ),
                            onClick = { navController.navigate(Routes.ADMIN) }
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, null)
                            Text(" Админ-панель")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.errorContainer,
                    contentColor = cs.onErrorContainer
                ),
                onClick = {
                    scope.launch {
                        if (isGuest) {
                            // ✅ очищаем только guest-данные
                            ProfileStore.clear(context, "guest")
                            SessionManager.setGuest()
                        } else {
                            SessionManager.signOut()
                        }

                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                }
            ) {
                Text(if (isGuest) "Выйти из гостевого режима" else "Выйти")
            }
        }
    }
}
