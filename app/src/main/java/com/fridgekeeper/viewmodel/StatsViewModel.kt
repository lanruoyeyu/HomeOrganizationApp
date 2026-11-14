package com.fridgekeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fridgekeeper.data.local.CategoryEntity
import com.fridgekeeper.data.local.MapMarkerEntity
import com.fridgekeeper.data.local.SubcategoryEntity
import com.fridgekeeper.data.repository.CatalogRepository
import com.fridgekeeper.data.repository.MapMarkerRepository
import com.fridgekeeper.data.repository.MapPlaceRepository
import com.fridgekeeper.data.repository.StatsRepository
import com.fridgekeeper.model.InventoryMath
import com.fridgekeeper.model.MapMarkerUiState
import com.fridgekeeper.model.MapPlaceUiState
import com.fridgekeeper.model.MarkerItemType
import com.fridgekeeper.model.ModuleStats
import com.fridgekeeper.model.PickerItem
import com.fridgekeeper.model.StatsUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StatsViewModel(
    private val repository: StatsRepository,
    private val mapMarkerRepository: MapMarkerRepository,
    private val mapPlaceRepository: MapPlaceRepository,
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val refreshTick = flow {
        while (true) {
            emit(Unit)
            delay(60 * 60 * 1000L)
        }
    }

    val uiState: StateFlow<StatsUiState> = combine(
        repository.observeFridgeItems(),
        repository.observeDigitalItems(),
        repository.observePetItems(),
        repository.observeMedicines(),
        refreshTick
    ) { fridge, digital, pet, medicine, _ ->
        val now = System.currentTimeMillis()

        val fridgeStats = ModuleStats(
            itemCount = fridge.sumOf { it.quantity },
            totalCost = fridge.mapNotNull { it.buyPrice }.sum(),
            dailyCost = fridge
                .filter { it.trackDailyCost && it.buyPrice != null }
                .sumOf {
                    InventoryMath.dailyCost(it.buyPrice!!, it.buyAt ?: it.storageDate, now)
                },
            hasPrice = fridge.any { it.buyPrice != null }
        )
        val digitalStats = ModuleStats(
            itemCount = digital.size,
            totalCost = digital.sumOf { it.purchasePrice },
            dailyCost = digital
                .filter { it.sellDate == null }
                .sumOf { InventoryMath.dailyCost(it.purchasePrice, it.purchaseDate, now) },
            hasPrice = true
        )
        val petStats = ModuleStats(
            itemCount = pet.size,
            totalCost = pet.sumOf { it.purchasePrice },
            dailyCost = pet
                .filter { it.trackDailyCost }
                .sumOf { InventoryMath.dailyCost(it.purchasePrice, it.purchaseDate, now) },
            hasPrice = true
        )
        val medicineStats = ModuleStats(
            itemCount = medicine.size,
            totalCost = medicine.mapNotNull { it.price }.sum(),
            dailyCost = 0.0,
            hasPrice = medicine.any { it.price != null }
        )

        StatsUiState(
            fridgeStats = fridgeStats,
            digitalStats = digitalStats,
            petStats = petStats,
            medicineStats = medicineStats,
            totalDailyCost = fridgeStats.dailyCost + digitalStats.dailyCost + petStats.dailyCost,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState(isLoading = true)
    )

    val markers: StateFlow<List<MapMarkerUiState>> = mapMarkerRepository.observeAll()
        .map { list -> list.map { MapMarkerUiState.fromEntity(it) } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val places: StateFlow<List<MapPlaceUiState>> = mapPlaceRepository.observeAll()
        .map { list -> list.map { MapPlaceUiState.fromEntity(it) } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categories: StateFlow<List<CategoryEntity>> = catalogRepository.observeCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val subcategories: StateFlow<List<SubcategoryEntity>> = catalogRepository.observeSubcategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun buildPickerItems(onReady: (List<PickerItem>) -> Unit) {
        viewModelScope.launch {
            val items = buildList {
                repository.observeFridgeItems().first().forEach {
                    add(PickerItem(MarkerItemType.FRIDGE, it.id, it.name, "食品"))
                }
                repository.observeDigitalItems().first().forEach {
                    add(PickerItem(MarkerItemType.DIGITAL, it.id, it.name, "数码产品"))
                }
                repository.observePetItems().first().forEach {
                    add(PickerItem(MarkerItemType.PET, it.id, it.name, "宠物用品"))
                }
            }
            onReady(items)
        }
    }

    fun addMarker(
        itemType: MarkerItemType,
        itemId: Long,
        displayName: String,
        posX: Float,
        posY: Float
    ) {
        viewModelScope.launch {
            mapMarkerRepository.add(
                MapMarkerEntity(
                    itemType = itemType.name,
                    itemId = itemId,
                    itemDisplayName = displayName,
                    posX = posX,
                    posY = posY
                )
            )
        }
    }

    fun deleteMarker(id: Long) {
        viewModelScope.launch {
            mapMarkerRepository.delete(id)
        }
    }

    fun addPlace(name: String, posX: Float, posY: Float, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            onResult(mapPlaceRepository.add(name, posX, posY) > 0)
        }
    }

    fun deletePlace(id: Long) {
        viewModelScope.launch {
            mapPlaceRepository.delete(id)
        }
    }

    fun addCategory(name: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            onResult(catalogRepository.addCategory(name) > 0)
        }
    }

    fun addSubcategory(categoryId: Long, name: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            onResult(catalogRepository.addSubcategory(categoryId, name) > 0)
        }
    }
}
