package com.example.inventariolpy

import android.content.Context

object PasswordManager {
    private const val PREFS_NAME = "AppPrefs"
    private const val KEY_PASSWORD = "EditPassword"

    fun isPasswordSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_PASSWORD)
    }

    fun setEditPassword(context: Context, password: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PASSWORD, password).apply()
    }

    fun isAdminPasswordValid(password: String): Boolean {
        return password == "admin123" // o lo que uses como clave maestra
    }

    fun getEditPassword(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PASSWORD, null)
    }
}