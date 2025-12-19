package com.janerli.delishhub.feature.home

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.janerli.delishhub.core.di.AppGraph
import com.janerli.delishhub.core.notifications.NotificationsPrefs
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.core.ui.MainScaffold
import com.janerli.delishhub.data.notifications.MealPlanReminderScheduler
import com.janerli.delishhub.feature.recipes.RecipesViewModel
import com.janerli.delishhub.feature.recipes.RecipesViewModelFactory
import com.janerli.delishhub.feature.recipes.ui.RecipeCard
import com.janerli.delishhub.feature.recipes.ui.RecipeCardUi
import java.time.LocalDate
import kotlin.math.abs

private const val DEMO_OWNER_ID = "demo"

@Composable
fun HomeScreen(navController: NavHostController) {

    val context = LocalContext.current

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        // ✅ если пользователь выключил уведомления — вообще ничего не планируем
        if (!NotificationsPrefs.isEnabled(context)) return@LaunchedEffect

        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!granted) {
                notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                // даже если не дали — воркеры сами “тихо” пропустят, но планируем по prefs
            }
        }

        // ✅ новый планировщик: daily + refresh now
        MealPlanReminderScheduler.scheduleDaily(context)
        MealPlanReminderScheduler.scheduleRefreshNow(context)
    }

    val session by SessionManager.session.collectAsStateWithLifecycle()
    val isGuest = session.isGuest
    val myUid = session.userId

    var search by remember { mutableStateOf("") }

    val recipesVm: RecipesViewModel = viewModel(
        factory = RecipesViewModelFactory(
            repository = AppGraph.recipeRepository,
            isMyMode = false
        )
    )

    val allCards: List<RecipeCardUi> by recipesVm.cards.collectAsStateWithLifecycle()

    val feed = remember(allCards, myUid) {
        allCards.filter { it.isPublic || it.ownerId == myUid || it.ownerId == DEMO_OWNER_ID }
    }

    val q = search.trim()
    val filteredFeed = remember(feed, q) {
        if (q.isEmpty()) feed else feed.filter { it.title.contains(q, ignoreCase = true) }
    }

    val my = remember(feed, myUid, isGuest) {
        if (isGuest) emptyList() else feed.filter { it.ownerId == myUid }
    }

    val public = remember(feed) { feed.filter { it.isPublic || it.ownerId == DEMO_OWNER_ID } }

    val todayKey = remember { LocalDate.now().toEpochDay().toInt() }

    val recipeOfDay: RecipeCardUi? = remember(public, todayKey) {
        if (public.isEmpty()) null else {
            val idx = abs(todayKey) % public.size
            public[idx]
        }
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val rowCardWidth = screenWidth - 32.dp

    MainScaffold(
        navController = navController,
        title = "DelishHub",
        showBack = false
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Поиск по рецептам") },
                    leadingIcon = {
                        androidx.compose.material3.Icon(Icons.Filled.Search, contentDescription = null)
                    },
                    singleLine = true
                )
            }

            if (recipeOfDay != null) {
                item { SectionHeader("Рецепт дня") }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.padding(10.dp)) {
                            RecipeCard(
                                item = recipeOfDay,
                                onOpen = { id -> navController.navigate("recipe_details/$id") },
                                onToggleFavorite = if (isGuest) null else { id -> recipesVm.toggleFavorite(id) }
                            )
                        }
                    }
                }
            }

            if (!isGuest) {
                item { SectionHeader("Мои (${my.size})") }
                if (my.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(my.take(10), key = { "my-${it.id}" }) { item ->
                                Box(modifier = Modifier.width(rowCardWidth)) {
                                    RecipeCard(
                                        item = item,
                                        onOpen = { id -> navController.navigate("recipe_details/$id") },
                                        onToggleFavorite = { id -> recipesVm.toggleFavorite(id) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item { Text("Пока нет твоих рецептов. Создай первый 🙂") }
                }
            }

            item { SectionHeader("Лента (${filteredFeed.size})") }

            if (filteredFeed.isEmpty()) {
                item { Text("Ничего не найдено — попробуй другой запрос.") }
            } else {
                items(filteredFeed, key = { "feed-${it.id}" }) { item ->
                    RecipeCard(
                        item = item,
                        onOpen = { id -> navController.navigate("recipe_details/$id") },
                        onToggleFavorite = if (isGuest) null else { id -> recipesVm.toggleFavorite(id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 6.dp)
    )
}
