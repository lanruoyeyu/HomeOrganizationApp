package com.fridgekeeper.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CouponOptionsState(
    val sources: List<String> = CouponSettings.DEFAULT_SOURCES,
    val types: List<String> = CouponSettings.DEFAULT_TYPES
)

/** 优惠券来源 / 类型的可选项配置，保存后立即生效 */
class CouponSettings(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("coupon_settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(readState())
    val state: StateFlow<CouponOptionsState> = _state.asStateFlow()

    fun addSource(value: String) {
        val clean = value.trim()
        if (clean.isEmpty() || clean in _state.value.sources) return
        updateSources(_state.value.sources + clean)
    }

    fun removeSource(value: String) {
        if (_state.value.sources.size <= 1) return
        updateSources(_state.value.sources - value)
    }

    fun addType(value: String) {
        val clean = value.trim()
        if (clean.isEmpty() || clean in _state.value.types) return
        updateTypes(_state.value.types + clean)
    }

    fun removeType(value: String) {
        if (_state.value.types.size <= 1) return
        updateTypes(_state.value.types - value)
    }

    private fun updateSources(values: List<String>) {
        prefs.edit().putString(KEY_SOURCES, values.joinToString("\n")).apply()
        _state.value = _state.value.copy(sources = values)
    }

    private fun updateTypes(values: List<String>) {
        prefs.edit().putString(KEY_TYPES, values.joinToString("\n")).apply()
        _state.value = _state.value.copy(types = values)
    }

    private fun readState(): CouponOptionsState = CouponOptionsState(
        sources = readList(KEY_SOURCES, DEFAULT_SOURCES),
        types = readList(KEY_TYPES, DEFAULT_TYPES)
    )

    private fun readList(key: String, defaults: List<String>): List<String> =
        prefs.getString(key, null)
            ?.split("\n")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.takeIf { it.isNotEmpty() }
            ?: defaults

    companion object {
        val DEFAULT_SOURCES = listOf("广发信用卡", "云闪付", "支付宝", "美团", "京东")
        val DEFAULT_TYPES = listOf("还款红包", "立减金", "满减券", "折扣券", "代金券")

        private const val KEY_SOURCES = "coupon_sources"
        private const val KEY_TYPES = "coupon_types"
    }
}
