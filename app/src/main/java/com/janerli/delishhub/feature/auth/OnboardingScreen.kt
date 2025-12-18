package com.janerli.delishhub.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.janerli.delishhub.core.session.SessionManager

@Composable
fun OnboardingScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onGuest: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Добро пожаловать в DelishHub!", style = MaterialTheme.typography.headlineSmall)

            Button(onClick = onLogin) { Text("Войти") }
            Button(onClick = onRegister) { Text("Регистрация") }

            Button(onClick = {
                SessionManager.setGuest()
                onGuest()
            }) { Text("Продолжить как гость") }
        }
    }
}
