package jp.yuya.ozawa.subscriptionapp

import android.content.Context
import android.content.SharedPreferences

object PreferenceHelper {
    private const val PREFS_NAME = "my_app_prefs"
    private var prefs: SharedPreferences? = null

    fun setUp(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    fun getBoolean(key: String?, def: Boolean): Boolean {
        return prefs?.getBoolean(key, def) ?: def
    }
    fun setBoolean(key: String?, value: Boolean) {
        prefs?.edit()?.apply {
            putBoolean(key, value)
            apply()
        }
    }
}