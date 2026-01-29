package com.converso.utils

import android.content.Context
import org.json.JSONObject

object Config {
    private const val PREFS_NAME = "converso_config"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_REG_TOKEN = "registration_token"
    private const val KEY_PAYMENT_STATUS = "payment_status"
    
    fun saveFromQR(context: Context, json: String): Boolean {
        return try {
            val obj = JSONObject(json)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            
            prefs.putString(KEY_SERVER_URL, obj.getString("serverUrl"))
            prefs.putString(KEY_USER_ID, obj.getString("userId"))
            prefs.putString(KEY_USER_NAME, obj.getString("userName"))
            prefs.putString(KEY_REG_TOKEN, obj.getString("registrationToken"))
            
            prefs.apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun saveSubscriptionInfo(context: Context, status: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_PAYMENT_STATUS, status)
            .apply()
    }

    fun getServerUrl(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_SERVER_URL, null)
    }

    fun getUserName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_USER_NAME, "Unregistered") ?: "Unregistered"
    }

    fun getPaymentStatus(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_PAYMENT_STATUS, "Active") ?: "Active"
    }

    fun getWebSocketUrl(context: Context): String {
        val baseUrl = getServerUrl(context) ?: "https://backend.as.conversoempire.world"
        return baseUrl.replace("http://", "ws://").replace("https://", "wss://")
    }
    
    fun getRegistrationToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_REG_TOKEN, null)
    }
}
