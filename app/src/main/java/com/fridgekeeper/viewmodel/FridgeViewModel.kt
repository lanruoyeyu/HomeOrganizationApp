package com.fridgekeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fridgekeeper.data.local.ItemEntity
import com.fridgekeeper.data.repository.CatalogRepository
import com.fridgekeeper.data.repository.FridgeRepository
import com.fridgekeeper.model.CategoryKind
import com.fridgekeeper.model.FridgeUiState
import com.fridgekeeper.model.ItemUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FridgeViewModel(
    private val repository: FridgeRepository,
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val selectedSubcategoryId = MutableStateFlow(0L)

    val uiState: StateFlow<FridgeUiState> = combine(
        catalogRepository.observeCategories(),
        catalogRepository.observeSubcategories(),
        catalogRepository.observeLocations(),
        repository.observeActiveItems(),
        selectedSubcategoryId
    ) { categories, subcategories, locations, items, filterId ->
        val foodId = categories.firstOrNull { it.kind == CategoryKind.FOOD.name }?.id ?: 0L
        val foodSubs = subcategories.filter { it.categoryId == foodId }
        val locMap = locations.associate { it.id to it.displayName }
        val subMap = foodSubs.associate { it.id to it.name }
        val foodItems = items.filter { it.subcategoryId in subMap.keys }
            .map {
                ItemUiState.fromEntity(
                    it,
                    subMap[it.subcategoryId] ?: "未分类",
                    it.locationId?.let { id -> locMap[id] }
                )
            }
            .sortedWith(compareBy<ItemUiState> { it.remainingDays }.thenBy { it.storageDate })
        val shown = if (filterId == 0L) foodItems else foodItems.filter { it.subcategoryId == filterId }
        FridgeUiState(
            items = shown,
            allItems = foodItems,
            subcategoryCount = foodSubs.size,
            itemCount = foodItems.size,
            subcategories = foodSubs,
            locations = locations,
            selectedSubcategoryId = filterId,
            foodCategoryId = foodId,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FridgeUiState(isLoading = true)
    )

    fun selectSubcategory(id: Long) {
        selectedSubcategoryId.value = id
    }

    fun addSubcategory(name: String, onResult: (Boolean, Long) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val id = catalogRepository.addSubcategory(uiState.value.foodCategoryId, name)
            onResult(id > 0, id)
        }
    }

    fun addItem(
        name: String,
        subcategoryId: Long,
        quantity: Int,
        storageDate: Long,
        expirationDate: Long,
        locationId: Long?,
        trackDailyCost: Boolean,
        buyPrice: Double?,
        buyAt: Long?
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.addItem(
                ItemEntity(
                    name = name,
                    category = "",
                    subcategoryId = subcategoryId,
                    quantity = quantity,
                    storageDate = storageDate,
                    expirationDate = expirationDate,
                    locationId = locationId,
                    trackDailyCost = trackDailyCost,
                    buyPrice = buyPrice,
                    buyAt = buyAt,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun decreaseQuantity(id: Long, currentQuantity: Int) {
        if (currentQuantity <= 0) return
        viewModelScope.launch {
            repository.updateQuantity(id, currentQuantity - 1)
        }
    }

    fun increaseQuantity(id: Long, currentQuantity: Int) {
        viewModelScope.launch {
            repository.updateQuantity(id, currentQuantity + 1)
        }
    }

    fun clearItem(id: Long) {
        viewModelScope.launch {
            repository.clearItem(id)
        }
    }
}
