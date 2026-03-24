package com.forexbot.api

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREFS = "forex_prefs"
    private const val KEY_LOGGED_IN = "logged_in"
    private const val KEY_NAME = "user_name"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_BALANCE = "user_balance"

    fun saveLogin(ctx: Context, name: String, email: String) {
        prefs(ctx).edit().putBoolean(KEY_LOGGED_IN, true).putString(KEY_NAME, name).putString(KEY_EMAIL, email).apply()
    }

    fun isLoggedIn(ctx: Context) = prefs(ctx).getBoolean(KEY_LOGGED_IN, false)
    fun getName(ctx: Context) = prefs(ctx).getString(KEY_NAME, "Trader") ?: "Trader"
    fun getEmail(ctx: Context) = prefs(ctx).getString(KEY_EMAIL, "") ?: ""

    fun logout(ctx: Context) = prefs(ctx).edit().clear().apply()

    private fun prefs(ctx: Context): SharedPreferences = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
