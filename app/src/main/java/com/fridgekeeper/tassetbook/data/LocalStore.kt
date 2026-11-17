package com.fridgekeeper.tassetbook.data

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * 本地持久化，对应小程序 wx.setStorageSync 的三个键：assets / trades / settings。
 * 以 JSON 文件存储在应用内部目录。
 */
class LocalStore(private val dir: File) {
    private val gson = Gson()

    init {
        dir.mkdirs()
    }

    private fun file(name: String) = File(dir, name)

    fun loadAssets(): MutableList<Asset> {
        return try {
            val f = file("assets.json")
            if (!f.exists()) return mutableListOf()
            val text = f.readText(Charsets.UTF_8)
            if (text.isBlank()) mutableListOf()
            else gson.fromJson(text, object : TypeToken<MutableList<Asset>>() {}.type)
                ?: mutableListOf()
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    fun loadTrades(): MutableList<Trade> {
        return try {
            val f = file("trades.json")
            if (!f.exists()) return mutableListOf()
            val text = f.readText(Charsets.UTF_8)
            if (text.isBlank()) mutableListOf()
            else {
                // 归档功能已移除：丢弃旧版本遗留的已归档交易，其数据一并清除
                val kept = JsonParser.parseString(text).asJsonArray.filterNot { e ->
                    e.isJsonObject && e.asJsonObject.get("isArchived")?.asBoolean == true
                }
                gson.fromJson(gson.toJson(kept), object : TypeToken<MutableList<Trade>>() {}.type)
                    ?: mutableListOf()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    fun loadSettings(): Settings {
        return try {
            val f = file("settings.json")
            if (!f.exists()) return Settings()
            val text = f.readText(Charsets.UTF_8)
            if (text.isBlank()) Settings()
            else gson.fromJson(text, Settings::class.java) ?: Settings()
        } catch (e: Exception) {
            e.printStackTrace()
            Settings()
        }
    }

    fun loadAccounts(): MutableList<Account> {
        return try {
            val f = file("accounts.json")
            if (!f.exists()) return mutableListOf()
            val text = f.readText(Charsets.UTF_8)
            if (text.isBlank()) mutableListOf()
            else gson.fromJson(text, object : TypeToken<MutableList<Account>>() {}.type)
                ?: mutableListOf()
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    fun saveAccounts(accounts: List<Account>) {
        writeFile("accounts.json", gson.toJson(accounts))
    }

    fun saveAssets(assets: List<Asset>) {
        writeFile("assets.json", gson.toJson(assets))
    }

    fun saveTrades(trades: List<Trade>) {
        writeFile("trades.json", gson.toJson(trades))
    }

    fun saveSettings(settings: Settings) {
        writeFile("settings.json", gson.toJson(settings))
    }

    private fun writeFile(name: String, content: String) {
        try {
            file(name).writeText(content, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
