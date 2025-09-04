package com.irlquest.app

import android.content.Context

object TokenStorage {
    private const val PREFS_NAME = "irl_quest_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private lateinit var prefs: android.content.SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun clear() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).apply()
    }
}

