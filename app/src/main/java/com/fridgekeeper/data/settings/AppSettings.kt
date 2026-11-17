package com.fridgekeeper.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppPage(val label: String) {
    LEDGER("记账"),
    ITEMS("物品"),
    STATS("统计"),
    WELFARE("福利"),
    CHECKIN("打卡"),
    TASSET("做T")
}

enum class CardDensity(val label: String, val description: String, val spacingScale: Float) {
    COMPACT("紧凑型", "缩小卡片和列表之间的间距", 0.55f),
    NORMAL("正常型", "保持当前页面间距", 1f),
    ROOMY("宽松型", "增大卡片和列表之间的间距", 1.6f)
}

enum class CheckinColor(val label: String, val argb: Int) {
    GREEN("墨绿", 0xFF1B5E20.toInt()),
    TEAL("深青", 0xFF004D40.toInt()),
    BLUE("深蓝", 0xFF0D47A1.toInt()),
    PURPLE("深紫", 0xFF4A148C.toInt()),
    ORANGE("深橙", 0xFFBF360C.toInt()),
    SLATE("深灰", 0xFF263238.toInt())
}

data class AppSettingsState(
    val visiblePages: Set<AppPage> = AppPage.values().toSet(),
    val cardDensity: CardDensity = CardDensity.NORMAL,
    val checkinColor: CheckinColor = CheckinColor.GREEN,
    val checkinAsList: Boolean = false
)

/** 应用级界面设置，保存后立即生效 */
class AppSettings(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(readState())
    val state: StateFlow<AppSettingsState> = _state.asStateFlow()

    fun setPageVisible(page: AppPage, visible: Boolean) {
        val updated = _state.value.visiblePages.toMutableSet().apply {
            if (visible) add(page) else remove(page)
        }
        if (updated.isEmpty()) return

        prefs.edit().putStringSet(KEY_VISIBLE_PAGES, updated.map { it.name }.toSet()).apply()
        _state.value = _state.value.copy(visiblePages = updated)
    }

    fun setCardDensity(density: CardDensity) {
        prefs.edit().putString(KEY_CARD_DENSITY, density.name).apply()
        _state.value = _state.value.copy(cardDensity = density)
    }

    fun setCheckinColor(color: CheckinColor) {
        prefs.edit().putString(KEY_CHECKIN_COLOR, color.name).apply()
        _state.value = _state.value.copy(checkinColor = color)
    }

    fun setCheckinAsList(asList: Boolean) {
        prefs.edit().putBoolean(KEY_CHECKIN_AS_LIST, asList).apply()
        _state.value = _state.value.copy(checkinAsList = asList)
    }

    private fun readState(): AppSettingsState {
        val defaultPages = AppPage.values().toSet()
        val savedPages = prefs.getStringSet(KEY_VISIBLE_PAGES, null)
        var visiblePages = savedPages
            ?.mapNotNull { id -> AppPage.values().firstOrNull { it.name == id } }
            ?.toSet()
            ?.ifEmpty { defaultPages }
            ?: defaultPages

        // 新增「做T」页面后，为老用户默认显示一次
        if (prefs.getInt(KEY_PAGE_VERSION, 1) < 2) {
            visiblePages = visiblePages + AppPage.TASSET
            prefs.edit()
                .putStringSet(KEY_VISIBLE_PAGES, visiblePages.map { it.name }.toSet())
                .putInt(KEY_PAGE_VERSION, 2)
                .apply()
        }

        val densityName = prefs.getString(KEY_CARD_DENSITY, CardDensity.NORMAL.name)
        val density = CardDensity.values().firstOrNull { it.name == densityName } ?: CardDensity.NORMAL
        val colorName = prefs.getString(KEY_CHECKIN_COLOR, CheckinColor.GREEN.name)
        val checkinColor = CheckinColor.values().firstOrNull { it.name == colorName }
            ?: CheckinColor.GREEN
        return AppSettingsState(
            visiblePages = visiblePages,
            cardDensity = density,
            checkinColor = checkinColor,
            checkinAsList = prefs.getBoolean(KEY_CHECKIN_AS_LIST, false)
        )
    }

    companion object {
        private const val KEY_VISIBLE_PAGES = "visible_pages"
        private const val KEY_CARD_DENSITY = "card_density"
        private const val KEY_CHECKIN_COLOR = "checkin_color"
        private const val KEY_CHECKIN_AS_LIST = "checkin_as_list"
        private const val KEY_PAGE_VERSION = "page_version"
    }
}
