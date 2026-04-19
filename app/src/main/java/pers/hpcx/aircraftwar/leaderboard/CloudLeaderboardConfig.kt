package pers.hpcx.aircraftwar.leaderboard

import android.content.Context
import androidx.core.content.edit

object CloudLeaderboardConfig {
    
    private const val PREFS_NAME = "cloud_leaderboard"
    private const val KEY_BASE_URL = "base_url"
    private const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"
    
    fun getBaseUrl(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BASE_URL, DEFAULT_BASE_URL)
            ?.trim()
            .orEmpty()
            .ifBlank { DEFAULT_BASE_URL }
    }
    
    fun saveBaseUrl(context: Context, baseUrl: String) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_BASE_URL, baseUrl.trim().trimEnd('/')) }
    }
}
