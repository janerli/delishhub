package com.janerli.delishhub.feature.profile

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Локальные данные профиля (без Room):
 * - avatarPath: путь file://... на аватар (внутри filesDir/profile/)
 * - guestDisplayName: имя гостя (SharedPreferences)
 *
 * ВАЖНО:
 * Аватар хранится ПЕРЕ-пользовательски (по userKey = uid или "guest"),
 * иначе смена аватара у одного меняет у всех.
 */
object ProfileStore {

    private const val PREFS = "profile_prefs"
    private const val KEY_AVATAR_PATH_PREFIX = "avatar_path_" // + userKey
    private const val KEY_GUEST_NAME = "guest_name"

    private var currentUserKey: String? = null

    private val _avatarPath = MutableStateFlow<String?>(null)
    val avatarPath: StateFlow<String?> = _avatarPath

    private val _guestDisplayName = MutableStateFlow<String?>(null)
    val guestDisplayName: StateFlow<String?> = _guestDisplayName

    /**
     * Инициализация ДЛЯ КОНКРЕТНОГО пользователя.
     * userKey: uid (для авторизованных) или "guest".
     */
    fun init(context: Context, userKey: String) {
        val app = context.applicationContext
        if (currentUserKey == userKey) return

        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        currentUserKey = userKey

        _avatarPath.value = prefs.getString(avatarKey(userKey), null)

        // guest name хранится только для гостя
        _guestDisplayName.value = if (userKey == "guest") {
            prefs.getString(KEY_GUEST_NAME, null)
        } else {
            null
        }
    }

    private fun avatarKey(userKey: String): String = KEY_AVATAR_PATH_PREFIX + userKey

    private fun avatarFileName(userKey: String): String =
        if (userKey == "guest") "avatar_guest.jpg" else "avatar_$userKey.jpg"

    /**
     * Сохраняем имя гостя (и в память, и в prefs)
     */
    suspend fun setGuestDisplayName(context: Context, name: String?) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val value = name?.trim()?.takeIf { it.isNotBlank() }
        prefs.edit().putString(KEY_GUEST_NAME, value).apply()
        _guestDisplayName.value = value
    }

    /**
     * Сохраняем аватар ИМЕННО для userKey (uid или "guest")
     */
    suspend fun saveAvatarFromUri(context: Context, userKey: String, uri: Uri) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val targetDir = File(app.filesDir, "profile")
        if (!targetDir.exists()) targetDir.mkdirs()

        val targetFile = File(targetDir, avatarFileName(userKey))

        withContext(Dispatchers.IO) {
            app.contentResolver.openInputStream(uri).use { input ->
                if (input == null) throw IllegalStateException("Не удалось открыть изображение")
                targetFile.outputStream().use { out -> input.copyTo(out) }
            }
        }

        val path = targetFile.toURI().toString() // file:/...
        prefs.edit().putString(avatarKey(userKey), path).apply()

        // если сейчас открыт именно этот пользователь — обновляем flow
        if (currentUserKey == userKey) {
            _avatarPath.value = path
        }
    }

    /**
     * Очистка локальных данных для userKey:
     * - удаляем файл аватара этого пользователя
     * - удаляем ключ avatar_path_userKey
     * - для guest также удаляем имя
     */
    suspend fun clear(context: Context, userKey: String) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        withContext(Dispatchers.IO) {
            try {
                val dir = File(app.filesDir, "profile")
                val file = File(dir, avatarFileName(userKey))
                if (file.exists()) file.delete()
            } catch (_: Throwable) {
                // не критично
            }
        }

        val editor = prefs.edit().remove(avatarKey(userKey))
        if (userKey == "guest") editor.remove(KEY_GUEST_NAME)
        editor.apply()

        if (currentUserKey == userKey) {
            _avatarPath.value = null
            if (userKey == "guest") _guestDisplayName.value = null
        }
    }
}
