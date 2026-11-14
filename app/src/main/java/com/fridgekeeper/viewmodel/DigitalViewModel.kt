package com.fridgekeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fridgekeeper.data.local.DigitalItemEntity
import com.fridgekeeper.data.local.UsageRecordEntity
import com.fridgekeeper.data.repository.CatalogRepository
import com.fridgekeeper.data.repository.DigitalRepository
import com.fridgekeeper.data.repository.UsageRecordRepository
import com.fridgekeeper.model.DigitalItemUiState
import com.fridgekeeper.model.DigitalUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DigitalViewModel(
    private val repository: DigitalRepository,
    private val catalogRepository: CatalogRepository,
    private val usageRecordRepository: UsageRecordRepository
) : ViewModel() {

    private val refreshTick = flow {
        while (true) {
            emit(Unit)
            delay(60 * 60 * 1000L)
        }
    }

    val uiState: StateFlow<DigitalUiState> = combine(
        repository.observeAllItems(),
        catalogRepository.observeLocations(),
        refreshTick
    ) { items, locations, _ ->
        val locMap = locations.associate { it.id to it.displayName }
        DigitalUiState(
            items = items.map {
                DigitalItemUiState.fromEntity(it, it.locationId?.let { id -> locMap[id] })
            },
            locations = locations,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DigitalUiState(isLoading = true)
    )

    fun addItem(name: String, purchasePrice: Double, purchaseDate: Long, locationId: Long?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.addItem(
                DigitalItemEntity(
                    name = name,
                    purchasePrice = purchasePrice,
                    purchaseDate = purchaseDate,
                    locationId = locationId,
                    trackDailyCost = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun sellItem(id: Long, sellPrice: Double, sellDate: Long) {
        viewModelScope.launch {
            repository.markSold(id, sellPrice, sellDate, System.currentTimeMillis())
        }
    }

    fun updateItem(id: Long, name: String, purchasePrice: Double, purchaseDate: Long, locationId: Long?) {
        viewModelScope.launch {
            val existing = repository.getById(id) ?: return@launch
            repository.updateItem(
                existing.copy(
                    name = name,
                    purchasePrice = purchasePrice,
                    purchaseDate = purchaseDate,
                    locationId = locationId,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repository.deleteItem(id)
            usageRecordRepository.deleteByItem(UsageRecordEntity.TYPE_DIGITAL, id)
        }
    }

    fun addUsageRecord(itemId: Long, itemName: String, usageText: String, usedAt: Long, scene: String) {
        viewModelScope.launch {
            usageRecordRepository.add(
                UsageRecordEntity(
                    itemType = UsageRecordEntity.TYPE_DIGITAL,
                    itemId = itemId,
                    itemName = itemName,
                    usageText = usageText,
                    usedAt = usedAt,
                    scene = scene
                )
            )
        }
    }

    fun observeUsageHistory(itemId: Long): Flow<List<UsageRecordEntity>> =
        usageRecordRepository.observeByItem(UsageRecordEntity.TYPE_DIGITAL, itemId)
}
