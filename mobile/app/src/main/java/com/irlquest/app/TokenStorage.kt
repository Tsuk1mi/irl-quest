package com.irlquest.app

import android.content.Context

object TokenStorage {
    private const val PREFS_NAME = "irlquest_auth"
    private const val KEY_TOKEN = "access_token"

    private var ctx: Context? = null

    fun init(context: Context) {
        ctx = context.applicationContext
    }

    fun setToken(token: String?) {
        val c = ctx ?: return
        val prefs = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? {
        val c = ctx ?: return null
        val prefs = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TOKEN, null)
    }

    fun clear() {
        val c = ctx ?: return
        val prefs = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_TOKEN).apply()
    }
}
