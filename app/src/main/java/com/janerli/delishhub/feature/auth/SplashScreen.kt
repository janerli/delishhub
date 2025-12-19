package com.janerli.delishhub.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.janerli.delishhub.R
import com.janerli.delishhub.core.di.AppGraph
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.core.ui.theme.ThemeStore
import com.janerli.delishhub.data.notifications.MealPlanReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun SplashScreen(onContinue: (String) -> Unit) {
    val context = LocalContext.current.applicationContext

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            AppGraph.init(context)
            SessionManager.init()
            ThemeStore.init(context)

            MealPlanReminderScheduler.scheduleDaily(context)
            MealPlanReminderScheduler.scheduleRefreshNow(context)
        }

        delay(450)

        val current = SessionManager.session.value
        if (current.isGuest) onContinue("onboarding") else onContinue("home")
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ✅ ЛОГО вместо "DH"
            Image(
                painter = painterResource(R.drawable.delishhub_logo),
                contentDescription = "DelishHub",
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .padding(bottom = 18.dp)
            )

            CircularProgressIndicator()
            Text(
                text = "Загрузка…",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}
