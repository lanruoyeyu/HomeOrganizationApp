package com.fridgekeeper.model

enum class CategoryKind(val label: String) {
    FOOD("食品"),
    DIGITAL("数码产品"),
    PET("宠物用品"),
    CUSTOM("自定义");

    companion object {
        fun fromName(name: String): CategoryKind =
            entries.firstOrNull { it.name == name } ?: CUSTOM
    }
}
