package com.janerli.delishhub.feature.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.core.ui.MainScaffold
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AdminScreen(
    navController: NavHostController,
    onBack: () -> Unit
) {
    val session by SessionManager.session.collectAsStateWithLifecycle()

    // 🔒 защита от не-админов
    if (!session.isAdmin) {
        MainScaffold(
            navController = navController,
            title = "Доступ запрещён",
            showBack = true,
            onBack = onBack
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("У вас нет прав администратора")
            }
        }
        return
    }

    val adminScheme = MaterialTheme.colorScheme.copy(
        primary = Color(0xFF5C6BC0),    // indigo
        secondary = Color(0xFF26A69A),  // teal
        tertiary = Color(0xFF7E57C2)    // purple
    )

    // Реализация интерфейса
    MaterialTheme(colorScheme = adminScheme, typography = MaterialTheme.typography) {
        var tabIndex by remember { mutableIntStateOf(0) }
        val tabs = listOf("Рецепты", "Теги", "Пользователи")

        MainScaffold(
            navController = navController,
            title = "Админ-панель",
            showBack = true,
            onBack = onBack
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                TabRow(selectedTabIndex = tabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(selected = tabIndex == index, onClick = { tabIndex = index }, text = { Text(title) })
                    }
                }

                when (tabIndex) {
                    0 -> AdminRecipesScreen()
                    1 -> AdminTagsScreen()
                }
            }
        }
    }
}
