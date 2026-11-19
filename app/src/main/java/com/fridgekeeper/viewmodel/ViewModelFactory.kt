package com.fridgekeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fridgekeeper.data.backup.DataBackupManager
import com.fridgekeeper.data.ledger.LedgerExcelManager
import com.fridgekeeper.data.remote.LedgerAiParser
import com.fridgekeeper.data.repository.CatalogRepository
import com.fridgekeeper.data.repository.DigitalRepository
import com.fridgekeeper.data.repository.FridgeRepository
import com.fridgekeeper.data.repository.LedgerRepository
import com.fridgekeeper.data.repository.MapMarkerRepository
import com.fridgekeeper.data.repository.MapPlaceRepository
import com.fridgekeeper.data.repository.MedicineRepository
import com.fridgekeeper.data.repository.PetRepository
import com.fridgekeeper.data.repository.StatsRepository
import com.fridgekeeper.data.repository.UsageRecordRepository
import com.fridgekeeper.data.repository.WelfareRepository
import com.fridgekeeper.data.settings.LedgerSettings

class ViewModelFactory(
    private val fridgeRepository: FridgeRepository,
    private val digitalRepository: DigitalRepository,
    private val petRepository: PetRepository,
    private val medicineRepository: MedicineRepository,
    private val statsRepository: StatsRepository,
    private val catalogRepository: CatalogRepository,
    private val mapMarkerRepository: MapMarkerRepository,
    private val mapPlaceRepository: MapPlaceRepository,
    private val usageRecordRepository: UsageRecordRepository,
    private val ledgerRepository: LedgerRepository,
    private val ledgerAiParser: LedgerAiParser,
    private val ledgerSettings: LedgerSettings,
    private val dataBackupManager: DataBackupManager,
    private val ledgerExcelManager: LedgerExcelManager,
    private val welfareRepository: WelfareRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(WelfareViewModel::class.java) ->
                WelfareViewModel(welfareRepository) as T
            modelClass.isAssignableFrom(LedgerViewModel::class.java) ->
                LedgerViewModel(
                    ledgerRepository,
                    ledgerAiParser,
                    ledgerSettings,
                    dataBackupManager,
                    ledgerExcelManager,
                    catalogRepository,
                    fridgeRepository
                ) as T
            modelClass.isAssignableFrom(FridgeViewModel::class.java) ->
                FridgeViewModel(fridgeRepository, catalogRepository, usageRecordRepository) as T
            modelClass.isAssignableFrom(DigitalViewModel::class.java) ->
                DigitalViewModel(digitalRepository, catalogRepository, usageRecordRepository) as T
            modelClass.isAssignableFrom(PetViewModel::class.java) ->
                PetViewModel(petRepository, catalogRepository, usageRecordRepository) as T
            modelClass.isAssignableFrom(MedicineViewModel::class.java) ->
                MedicineViewModel(medicineRepository, usageRecordRepository) as T
            modelClass.isAssignableFrom(StatsViewModel::class.java) ->
                StatsViewModel(
                    statsRepository,
                    mapMarkerRepository,
                    mapPlaceRepository,
                    catalogRepository
                ) as T
            modelClass.isAssignableFrom(OrganizationViewModel::class.java) ->
                OrganizationViewModel(catalogRepository, fridgeRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
