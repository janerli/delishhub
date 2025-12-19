package com.janerli.delishhub.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.janerli.delishhub.core.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    navController: NavHostController,
    title: String,
    showBack: Boolean,
    onBack: (() -> Unit)? = null,
    fab: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val currentRoute =
        navController.currentBackStackEntryAsState().value?.destination?.route.orEmpty()

    val showBottomBar = currentRoute in setOf(
        Routes.HOME,
        Routes.RECIPES,
        Routes.PLANNER,
        Routes.SHOPPING,
        Routes.PROFILE,
        Routes.FAVORITES
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (showBack && onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (showBottomBar) BottomBar(navController)
        },
        floatingActionButton = { fab?.invoke() },
        content = content
    )
}
