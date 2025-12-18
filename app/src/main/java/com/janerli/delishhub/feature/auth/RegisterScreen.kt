package com.janerli.delishhub.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.data.sync.FavoriteSyncScheduler
import com.janerli.delishhub.data.sync.MealPlanSyncScheduler
import com.janerli.delishhub.data.sync.RecipeSyncScheduler
import com.janerli.delishhub.data.sync.ShoppingSyncScheduler
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

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
            Text("Регистрация", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = null
                },
                label = { Text("Имя (опционально)") },
                singleLine = true
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    error = null
                },
                label = { Text("Email") },
                singleLine = true
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    error = null
                },
                label = { Text("Пароль") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                enabled = !loading,
                onClick = {
                    val e = email.trim()
                    val p = password

                    if (e.isEmpty() || !e.contains("@")) {
                        error = "Введите корректный email"
                        return@Button
                    }
                    if (p.length < 6) {
                        error = "Пароль минимум 6 символов"
                        return@Button
                    }

                    loading = true
                    scope.launch {
                        try {
                            FirebaseAuth.getInstance()
                                .createUserWithEmailAndPassword(e, p)
                                .await()

                            val fbUser = FirebaseAuth.getInstance().currentUser

                            val cleanName = name.trim()
                            if (fbUser != null && cleanName.isNotEmpty()) {
                                try {
                                    fbUser.updateProfile(
                                        UserProfileChangeRequest.Builder()
                                            .setDisplayName(cleanName)
                                            .build()
                                    ).await()
                                } catch (_: Throwable) {
                                    // не критично
                                }
                            }

                            if (fbUser != null) {
                                // ✅ КРИТИЧНО: обновляем session сразу
                                SessionManager.setFromFirebase(fbUser)
                            } else {
                                SessionManager.setGuest()
                            }

                            // ✅ запускаем ВСЕ синки
                            FavoriteSyncScheduler.enqueueOneTime(context)
                            FavoriteSyncScheduler.schedulePeriodic(context)

                            ShoppingSyncScheduler.enqueueOneTime(context)
                            ShoppingSyncScheduler.schedulePeriodic(context)

                            MealPlanSyncScheduler.enqueueOneTime(context)
                            MealPlanSyncScheduler.schedulePeriodic(context)

                            RecipeSyncScheduler.enqueueOneTime(context)
                            RecipeSyncScheduler.schedulePeriodic(context)

                            onRegistered()
                        } catch (t: Throwable) {
                            error = t.message ?: "Ошибка регистрации"
                        } finally {
                            loading = false
                        }
                    }
                }
            ) {
                Text(if (loading) "Создаём..." else "Зарегистрироваться")
            }

            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onBack, enabled = !loading) { Text("Назад") }
        }
    }
}
