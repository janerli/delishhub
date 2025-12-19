package com.janerli.delishhub.core.notifications

import android.content.Context

object NotificationsPrefs {
    private const val PREFS = "delishhub_prefs"
    private const val KEY_ENABLED = "notifications_enabled"

    fun isEnabled(context: Context): Boolean {
        val sp = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_ENABLED, true) // по умолчанию ВКЛ
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val sp = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
}
