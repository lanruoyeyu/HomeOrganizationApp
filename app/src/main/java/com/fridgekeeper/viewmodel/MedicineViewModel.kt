package com.fridgekeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fridgekeeper.data.local.MedicineEntity
import com.fridgekeeper.data.local.UsageRecordEntity
import com.fridgekeeper.data.repository.MedicineRepository
import com.fridgekeeper.data.repository.UsageRecordRepository
import com.fridgekeeper.model.MedicineItemUiState
import com.fridgekeeper.model.MedicineUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MedicineViewModel(
    private val repository: MedicineRepository,
    private val usageRecordRepository: UsageRecordRepository
) : ViewModel() {

    private val refreshTick = flow {
        while (true) {
            emit(Unit)
            delay(60 * 60 * 1000L)
        }
    }

    val uiState: StateFlow<MedicineUiState> = combine(
        repository.observeAll(),
        refreshTick
    ) { items, _ ->
        MedicineUiState(
            items = items.map { MedicineItemUiState.fromEntity(it) },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MedicineUiState(isLoading = true)
    )

    fun addItem(name: String, expiryDate: Long, price: Double?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.add(
                MedicineEntity(
                    name = name,
                    expiryDate = expiryDate,
                    price = price,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
            usageRecordRepository.deleteByItem(UsageRecordEntity.TYPE_MEDICINE, id)
        }
    }

    fun updateItem(id: Long, name: String, expiryDate: Long, price: Double?) {
        viewModelScope.launch {
            val existing = repository.getById(id) ?: return@launch
            repository.update(
                existing.copy(
                    name = name,
                    expiryDate = expiryDate,
                    price = price,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun addUsageRecord(itemId: Long, itemName: String, usageText: String, usedAt: Long, scene: String) {
        viewModelScope.launch {
            usageRecordRepository.add(
                UsageRecordEntity(
                    itemType = UsageRecordEntity.TYPE_MEDICINE,
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
        usageRecordRepository.observeByItem(UsageRecordEntity.TYPE_MEDICINE, itemId)
}
