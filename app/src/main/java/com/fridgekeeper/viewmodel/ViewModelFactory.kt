package com.fridgekeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fridgekeeper.data.repository.CatalogRepository
import com.fridgekeeper.data.repository.DigitalRepository
import com.fridgekeeper.data.repository.FridgeRepository
import com.fridgekeeper.data.repository.PetRepository
import com.fridgekeeper.data.repository.StatsRepository

class ViewModelFactory(
    private val fridgeRepository: FridgeRepository,
    private val digitalRepository: DigitalRepository,
    private val petRepository: PetRepository,
    private val statsRepository: StatsRepository,
    private val catalogRepository: CatalogRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(FridgeViewModel::class.java) ->
                FridgeViewModel(fridgeRepository, catalogRepository) as T
            modelClass.isAssignableFrom(DigitalViewModel::class.java) ->
                DigitalViewModel(digitalRepository, catalogRepository) as T
            modelClass.isAssignableFrom(PetViewModel::class.java) ->
                PetViewModel(petRepository, catalogRepository) as T
            modelClass.isAssignableFrom(StatsViewModel::class.java) ->
                StatsViewModel(statsRepository, catalogRepository, petRepository) as T
            modelClass.isAssignableFrom(OrganizationViewModel::class.java) ->
                OrganizationViewModel(catalogRepository, fridgeRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
