package com.janerli.delishhub.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.janerli.delishhub.core.navigation.Routes
import com.janerli.delishhub.core.session.SessionManager

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
fun BottomBar(navController: NavHostController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route.orEmpty()
    val session = SessionManager.session.collectAsStateWithLifecycle().value
    val isGuest = session.isGuest

    val items = buildList {
        add(BottomItem(Routes.HOME, "Главная") { Icon(Icons.Filled.Home, contentDescription = null) })
        add(BottomItem(Routes.RECIPES, "Рецепты") { Icon(Icons.Filled.RestaurantMenu, contentDescription = null) })

        if (!isGuest) {
            add(BottomItem(Routes.PLANNER, "План") { Icon(Icons.Filled.CalendarMonth, contentDescription = null) })
            add(BottomItem(Routes.SHOPPING, "Покупки") { Icon(Icons.Filled.ShoppingCart, contentDescription = null) })
        }

        add(BottomItem(Routes.PROFILE, "Профиль") { Icon(Icons.Filled.Person, contentDescription = null) })
    }

    val cs = MaterialTheme.colorScheme

    NavigationBar(
        containerColor = cs.surface
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(Routes.HOME) { saveState = true }
                    }
                },
                icon = item.icon,
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = cs.primary,
                    selectedTextColor = cs.primary,
                    indicatorColor = cs.secondaryContainer, // ✅ было surfaceVariant (фиолетило)
                    unselectedIconColor = cs.onSurfaceVariant,
                    unselectedTextColor = cs.onSurfaceVariant
                )
            )
        }
    }
}
