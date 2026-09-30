package com.fridgekeeper.data.repository

import com.fridgekeeper.data.local.DigitalItemDao
import com.fridgekeeper.data.local.ItemDao
import com.fridgekeeper.data.local.PetCategoryDao
import com.fridgekeeper.data.local.PetItemDao

class StatsRepository(
    private val itemDao: ItemDao,
    private val digitalItemDao: DigitalItemDao,
    private val petCategoryDao: PetCategoryDao,
    private val petItemDao: PetItemDao
) {
    fun observeFridgeItems() = itemDao.observeActiveItems()
    fun observeDigitalItems() = digitalItemDao.observeAllItems()
    fun observePetItems() = petItemDao.observeAll()
    fun observePetCategories() = petCategoryDao.observeAll()
}
