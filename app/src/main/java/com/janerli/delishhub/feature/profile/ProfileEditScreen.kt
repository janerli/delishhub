package com.janerli.delishhub.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.SubcomposeAsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.janerli.delishhub.core.navigation.Routes
import com.janerli.delishhub.core.session.SessionManager
import com.janerli.delishhub.core.ui.MainScaffold
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileEditScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val session by SessionManager.session.collectAsStateWithLifecycle()
    val isGuest = session.isGuest
    val userKey = if (isGuest) "guest" else session.userId

    // ✅ важно: инициализация профиля ДЛЯ ТЕКУЩЕГО userKey
    ProfileStore.init(context, userKey)

    val avatarPath by ProfileStore.avatarPath.collectAsStateWithLifecycle()
    val guestName by ProfileStore.guestDisplayName.collectAsStateWithLifecycle()

    var name by remember {
        mutableStateOf(
            if (isGuest) (guestName ?: "") else session.name
        )
    }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    loading = true
                    error = null
                    // ✅ сохраняем аватар ПЕРЕ-пользовательски
                    ProfileStore.saveAvatarFromUri(context, userKey, uri)
                } catch (t: Throwable) {
                    error = t.message ?: "Ошибка загрузки аватара"
                } finally {
                    loading = false
                }
            }
        }
    }

    MainScaffold(
        navController = navController,
        title = "Редактирование профиля",
        showBack = true
    ) { padding: PaddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        modifier = Modifier
                            .height(72.dp)
                            .aspectRatio(1f)
                            .clip(CircleShape),
                        shape = CircleShape
                    ) {
                        if (!avatarPath.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = avatarPath,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.padding(8.dp))

                    Column {
                        Text(
                            text = "Аватар",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Нажми, чтобы выбрать фото",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                onClick = { pickImageLauncher.launch("image/*") }
            ) {
                Text("Выбрать фото")
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                label = { Text("Имя") },
                singleLine = true
            )

            if (!error.isNullOrBlank()) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                onClick = {
                    scope.launch {
                        try {
                            loading = true
                            error = null

                            val clean = name.trim()

                            if (isGuest) {
                                ProfileStore.setGuestDisplayName(context, clean)
                                navController.popBackStack()
                                return@launch
                            }

                            val user = FirebaseAuth.getInstance().currentUser
                            if (user == null) {
                                error = "Нет активного пользователя"
                                return@launch
                            }

                            val profileUpdates =
                                com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                    .setDisplayName(clean)
                                    .build()

                            user.updateProfile(profileUpdates).await()
                            navController.popBackStack()
                        } catch (t: Throwable) {
                            error = t.message ?: "Ошибка сохранения"
                        } finally {
                            loading = false
                        }
                    }
                }
            ) {
                Text(if (loading) "Сохранение..." else "Сохранить")
            }

            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                onClick = { navController.navigate(Routes.SETTINGS) }
            ) {
                Text("Настройки")
            }
        }
    }
}
