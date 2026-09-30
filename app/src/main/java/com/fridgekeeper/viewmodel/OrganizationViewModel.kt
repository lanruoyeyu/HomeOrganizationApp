package com.fridgekeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fridgekeeper.data.local.CategoryEntity
import com.fridgekeeper.data.local.ItemEntity
import com.fridgekeeper.data.local.LocationEntity
import com.fridgekeeper.data.local.SubcategoryEntity
import com.fridgekeeper.data.repository.CatalogRepository
import com.fridgekeeper.data.repository.FridgeRepository
import com.fridgekeeper.model.CategoryKind
import com.fridgekeeper.model.QuantityType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OrganizationUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val subcategories: List<SubcategoryEntity> = emptyList(),
    val locations: List<LocationEntity> = emptyList(),
    val isLoading: Boolean = true
)

class OrganizationViewModel(
    private val catalogRepository: CatalogRepository,
    private val fridgeRepository: FridgeRepository
) : ViewModel() {

    val uiState: StateFlow<OrganizationUiState> = combine(
        catalogRepository.observeCategories(),
        catalogRepository.observeSubcategories(),
        catalogRepository.observeLocations()
    ) { categories, subcategories, locations ->
        OrganizationUiState(categories, subcategories, locations, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OrganizationUiState(isLoading = true)
    )

    fun addCategory(name: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            onResult(catalogRepository.addCategory(name) > 0)
        }
    }

    fun addSubcategory(
        categoryId: Long,
        name: String,
        trackDailyCostDefault: Boolean,
        quantityMode: QuantityType,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            onResult(
                catalogRepository.addSubcategory(
                    categoryId,
                    name,
                    quantityMode,
                    trackDailyCostDefault
                ) > 0
            )
        }
    }

    fun addLocation(room: String, spot: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            onResult(catalogRepository.addLocation(room, spot) > 0)
        }
    }

    fun deleteLocation(location: LocationEntity) {
        viewModelScope.launch {
            catalogRepository.deleteLocation(location)
        }
    }

    fun addCustomItem(
        subcategoryId: Long,
        name: String,
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
            fridgeRepository.addItem(
                ItemEntity(
                    name = name,
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

    fun kindOf(category: CategoryEntity): CategoryKind = CategoryKind.fromName(category.kind)
}
