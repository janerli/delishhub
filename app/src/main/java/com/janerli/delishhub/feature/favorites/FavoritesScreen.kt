package com.janerli.delishhub.feature.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.janerli.delishhub.core.di.AppGraph
import com.janerli.delishhub.core.navigation.Routes
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.core.ui.MainScaffold
import com.janerli.delishhub.feature.recipes.ui.RecipeCard
import com.janerli.delishhub.feature.recipes.ui.RecipeCardUi

@Composable
fun FavoritesScreen(navController: NavHostController) {
    val session by SessionManager.session.collectAsStateWithLifecycle()
    val isGuest = session.isGuest

    val vm: FavoritesViewModel = viewModel(
        factory = FavoritesViewModelFactory(AppGraph.recipeRepository)
    )
    val favorites: List<RecipeCardUi> by vm.cards.collectAsStateWithLifecycle()

    MainScaffold(
        navController = navController,
        title = "Избранное",
        showBack = true,
        onBack = { navController.popBackStack() }
    ) { padding: PaddingValues ->

        if (isGuest) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Избранное скрыто в гостевом режиме.")
            }
        } else {
            if (favorites.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Пока нет избранных рецептов.")
                    Text("Открой рецепт и нажми сердечко — он появится здесь.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = favorites,
                        key = { item: RecipeCardUi -> item.id }
                    ) { item: RecipeCardUi ->
                        RecipeCard(
                            item = item,
                            onOpen = { id -> navController.navigate(Routes.recipeDetails(id)) },
                            onToggleFavorite = { id -> vm.toggleFavorite(id) }
                        )
                    }
                }
            }
        }
    }
}
