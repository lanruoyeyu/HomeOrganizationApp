package com.fridgekeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fridgekeeper.data.local.DigitalItemEntity
import com.fridgekeeper.data.repository.CatalogRepository
import com.fridgekeeper.data.repository.DigitalRepository
import com.fridgekeeper.model.DigitalItemUiState
import com.fridgekeeper.model.DigitalUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DigitalViewModel(
    private val repository: DigitalRepository,
    private val catalogRepository: CatalogRepository
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
}
