package com.fridgekeeper

import android.app.Application
import com.fridgekeeper.data.local.AppDatabase
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
import com.fridgekeeper.data.settings.LedgerSettings

class FridgeApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: FridgeRepository by lazy { FridgeRepository.getRepository(database) }
    val digitalRepository: DigitalRepository by lazy { DigitalRepository(database.digitalItemDao()) }
    val petRepository: PetRepository by lazy {
        PetRepository(database.petCategoryDao(), database.petItemDao())
    }
    val medicineRepository: MedicineRepository by lazy {
        MedicineRepository(database.medicineDao())
    }
    val mapMarkerRepository: MapMarkerRepository by lazy {
        MapMarkerRepository(database.mapMarkerDao())
    }
    val mapPlaceRepository: MapPlaceRepository by lazy {
        MapPlaceRepository(database.mapPlaceDao())
    }
    val catalogRepository: CatalogRepository by lazy {
        CatalogRepository(
            database.categoryDao(),
            database.subcategoryDao(),
            database.locationDao()
        )
    }
    val statsRepository: StatsRepository by lazy {
        StatsRepository(
            database.itemDao(),
            database.digitalItemDao(),
            database.petCategoryDao(),
            database.petItemDao(),
            database.medicineDao()
        )
    }
    val usageRecordRepository: UsageRecordRepository by lazy {
        UsageRecordRepository(database.usageRecordDao())
    }
    val ledgerRepository: LedgerRepository by lazy {
        LedgerRepository(database.ledgerDao())
    }
    val ledgerSettings: LedgerSettings by lazy { LedgerSettings(this) }
    val ledgerAiParser: LedgerAiParser by lazy { LedgerAiParser(ledgerSettings) }
}
