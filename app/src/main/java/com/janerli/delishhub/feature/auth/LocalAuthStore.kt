package com.janerli.delishhub.feature.auth

import android.content.Context

private const val PREFS_NAME = "delishhub_local_auth"
private const val KEY_USERS = "users_v1"

/**
 * Локальная модель пользователя (ВРЕМЕННО, до Firebase).
 */
data class LocalUser(
    val name: String,
    val email: String,
    val password: String
)

/**
 * Простейшее локальное хранилище пользователей.
 * Используется ТОЛЬКО до Firebase Auth.
 */
object LocalAuthStore {

    fun loadUsers(context: Context): List<LocalUser> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_USERS, emptySet()).orEmpty()

        return set.mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size != 3) null
            else LocalUser(
                name = parts[0],
                email = parts[1],
                password = parts[2]
            )
        }
    }

    fun saveUser(context: Context, user: LocalUser) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = (prefs.getStringSet(KEY_USERS, emptySet()) ?: emptySet()).toMutableSet()
        current.add("${user.name}|${user.email}|${user.password}")
        prefs.edit().putStringSet(KEY_USERS, current).apply()
    }
}
