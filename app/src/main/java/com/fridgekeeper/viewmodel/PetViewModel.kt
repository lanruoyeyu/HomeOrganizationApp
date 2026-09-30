package com.fridgekeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fridgekeeper.data.local.PetItemEntity
import com.fridgekeeper.data.repository.CatalogRepository
import com.fridgekeeper.data.repository.PetRepository
import com.fridgekeeper.model.PetCategoryUiState
import com.fridgekeeper.model.PetItemUiState
import com.fridgekeeper.model.PetUiState
import com.fridgekeeper.model.QuantityType
import com.fridgekeeper.model.WeightUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PetViewModel(
    private val repository: PetRepository,
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    val uiState: StateFlow<PetUiState> = combine(
        repository.observeItems(),
        repository.observeCategories(),
        catalogRepository.observeLocations()
    ) { items, categories, locations ->
        val categoryNames = categories.associate { it.id to it.name }
        val locMap = locations.associate { it.id to it.displayName }
        PetUiState(
            items = items.map {
                PetItemUiState.fromEntity(
                    it,
                    categoryNames[it.categoryId] ?: "未分类",
                    it.locationId?.let { id -> locMap[id] }
                )
            },
            categories = categories.map { PetCategoryUiState.fromEntity(it) },
            locations = locations,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PetUiState(isLoading = true)
    )

    fun addCategory(name: String, onResult: (Boolean, Long) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val id = repository.addCategory(name)
            onResult(id > 0, id)
        }
    }

    fun addItem(
        name: String,
        categoryId: Long,
        purchasePrice: Double,
        purchaseDate: Long,
        quantityType: QuantityType,
        weightUnit: WeightUnit,
        quantity: Double,
        tags: String,
        locationId: Long?,
        trackDailyCost: Boolean
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.addItem(
                PetItemEntity(
                    name = name,
                    categoryId = categoryId,
                    purchasePrice = purchasePrice,
                    purchaseDate = purchaseDate,
                    quantityType = quantityType.name,
                    weightUnit = if (quantityType == QuantityType.WEIGHT) weightUnit.name else null,
                    quantity = quantity,
                    tags = tags,
                    locationId = locationId,
                    trackDailyCost = trackDailyCost,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }
}
