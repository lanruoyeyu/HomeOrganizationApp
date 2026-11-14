package com.fridgekeeper.data.settings

import android.content.Context

/**
 * 大模型接入配置（OpenAI 兼容接口）
 */
class LedgerSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ledger_settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL).orEmpty()
            .ifBlank { DEFAULT_BASE_URL }
        set(value) = prefs.edit()
            .putString(KEY_BASE_URL, value.trim().ifBlank { DEFAULT_BASE_URL })
            .apply()

    var model: String
        get() = prefs.getString(KEY_MODEL, DEFAULT_MODEL).orEmpty()
            .ifBlank { DEFAULT_MODEL }
        set(value) = prefs.edit()
            .putString(KEY_MODEL, value.trim().ifBlank { DEFAULT_MODEL })
            .apply()

    val isConfigured: Boolean get() = apiKey.isNotBlank()

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL = "model"

        const val DEFAULT_BASE_URL = "https://api.deepseek.com"
        const val DEFAULT_MODEL = "deepseek-chat"
    }
}
