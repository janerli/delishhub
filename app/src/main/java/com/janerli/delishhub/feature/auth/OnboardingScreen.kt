package com.janerli.delishhub.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.janerli.delishhub.R
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

            Card(
                modifier = Modifier.widthIn(max = 420.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // ✅ логотип
                    Image(
                        painter = painterResource(R.drawable.delishhub_logo),
                        contentDescription = "DelishHub",
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .padding(bottom = 6.dp)
                    )

                    Text(
                        text = "Добро пожаловать!",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = "Рецепты, план питания и список покупок — даже без интернета.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = onLogin,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Войти")
                    }

                    FilledTonalButton(
                        onClick = onRegister,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Регистрация")
                    }

                    FilledTonalButton(
                        onClick = {
                            SessionManager.setGuest()
                            onGuest()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Продолжить как гость")
                    }
                }
            }
        }
    }
}
