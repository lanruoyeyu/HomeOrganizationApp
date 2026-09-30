package com.fridgekeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fridgekeeper.data.repository.CatalogRepository
import com.fridgekeeper.data.repository.PetRepository
import com.fridgekeeper.data.repository.StatsRepository
import com.fridgekeeper.model.InventoryMath
import com.fridgekeeper.model.QuantityType
import com.fridgekeeper.model.StatsRow
import com.fridgekeeper.model.StatsUiState
import com.fridgekeeper.model.WeightUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(
    private val repository: StatsRepository,
    private val catalogRepository: CatalogRepository,
    private val petRepository: PetRepository
) : ViewModel() {

    private val refreshTick = flow {
        while (true) {
            emit(Unit)
            delay(60 * 60 * 1000L)
        }
    }

    val uiState: StateFlow<StatsUiState> = combine(
        combine(
            repository.observeFridgeItems(),
            repository.observeDigitalItems(),
            repository.observePetItems(),
            petRepository.observeCategories()
        ) { fridge, digital, pet, petCats -> Quad(fridge, digital, pet, petCats) },
        combine(
            catalogRepository.observeSubcategories(),
            catalogRepository.observeLocations(),
            refreshTick
        ) { subs, locs, tick -> Triple(subs, locs, tick) }
    ) { items, catalog ->
        val now = System.currentTimeMillis()
        val subMap = catalog.first.associate { it.id to it.name }
        val locMap = catalog.second.associate { it.id to it.displayName }
        val petCatMap = items.fourth.associate { it.id to it.name }
        val daily = mutableListOf<StatsRow>()
        val qty = mutableListOf<StatsRow>()

        items.first.forEach { item ->
            val subName = subMap[item.subcategoryId] ?: "未分类"
            val loc = item.locationId?.let { locMap[it] }
            val remaining = item.quantity.toString()
            if (item.trackDailyCost && item.buyPrice != null) {
                daily += StatsRow(
                    id = "food-${item.id}",
                    name = item.name,
                    moduleLabel = "食品",
                    subcategoryName = subName,
                    locationLabel = loc,
                    dailyCost = InventoryMath.dailyCost(
                        item.buyPrice,
                        item.buyAt ?: item.storageDate,
                        now
                    ),
                    remainingQuantity = remaining
                )
            } else {
                qty += StatsRow(
                    id = "food-${item.id}",
                    name = item.name,
                    moduleLabel = "食品",
                    subcategoryName = subName,
                    locationLabel = loc,
                    dailyCost = null,
                    remainingQuantity = remaining
                )
            }
        }

        items.second.forEach { item ->
            val loc = item.locationId?.let { locMap[it] }
            val end = if (item.sellDate != null) item.sellDate else now
            val row = StatsRow(
                id = "digital-${item.id}",
                name = item.name,
                moduleLabel = "数码",
                subcategoryName = "数码产品",
                locationLabel = loc,
                dailyCost = InventoryMath.dailyCost(item.purchasePrice, item.purchaseDate, end),
                remainingQuantity = if (item.sellDate == null) "持有中" else "已卖出"
            )
            if (item.trackDailyCost) daily += row else qty += row.copy(dailyCost = null)
        }

        items.third.forEach { item ->
            val loc = item.locationId?.let { locMap[it] }
            val cat = petCatMap[item.categoryId] ?: "未分类"
            val unit = if (item.quantityType == QuantityType.WEIGHT.name) {
                "${InventoryMath.formatQuantity(item.quantity)}${WeightUnit.fromName(item.weightUnit).label}"
            } else {
                "${InventoryMath.formatQuantity(item.quantity)}个"
            }
            if (item.trackDailyCost) {
                daily += StatsRow(
                    id = "pet-${item.id}",
                    name = item.name,
                    moduleLabel = "宠物",
                    subcategoryName = cat,
                    locationLabel = loc,
                    dailyCost = InventoryMath.dailyCost(item.purchasePrice, item.purchaseDate, now),
                    remainingQuantity = unit
                )
            } else {
                qty += StatsRow(
                    id = "pet-${item.id}",
                    name = item.name,
                    moduleLabel = "宠物",
                    subcategoryName = cat,
                    locationLabel = loc,
                    dailyCost = null,
                    remainingQuantity = unit
                )
            }
        }

        StatsUiState(dailyCostRows = daily, quantityRows = qty, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState(isLoading = true)
    )

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}

private data class Triple<A, B, C>(val first: A, val second: B, val third: C)
