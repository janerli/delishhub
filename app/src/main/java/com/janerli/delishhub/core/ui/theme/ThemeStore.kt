package com.janerli.delishhub.core.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

object ThemeStore {

    private const val PREFS = "delishhub_theme_prefs"
    private const val KEY_MODE = "theme_mode"

    private var prefs: SharedPreferences? = null

    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p

        val saved = p.getString(KEY_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        _mode.value = runCatching { ThemeMode.valueOf(saved) }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun setMode(value: ThemeMode) {
        _mode.value = value
        prefs?.edit()?.putString(KEY_MODE, value.name)?.apply()
    }
}
