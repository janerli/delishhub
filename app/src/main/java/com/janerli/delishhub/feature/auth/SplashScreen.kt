package com.janerli.delishhub.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.janerli.delishhub.core.di.AppGraph
import com.janerli.delishhub.core.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun SplashScreen(onContinue: (String) -> Unit) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            AppGraph.init(context.applicationContext)
            SessionManager.init()
        }

        delay(350)

        // ✅ берём актуальное значение напрямую после init
        val current = SessionManager.session.value
        if (current.isGuest) onContinue("onboarding") else onContinue("home")
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("DelishHub")
        Text("Загрузка...")
    }
}
