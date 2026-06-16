package com.packup.data.repository

import androidx.room.withTransaction
import com.packup.data.local.DevicePreferences
import com.packup.data.local.PackUpDatabase
import com.packup.data.local.dao.CategoryDao
import com.packup.data.local.dao.FamilyMemberDao
import com.packup.data.local.dao.MorningItemDao
import com.packup.data.local.dao.PackingItemDao
import com.packup.data.local.entity.CategoryEntity
import com.packup.data.local.entity.FamilyMemberEntity
import com.packup.data.local.entity.ItemStatus
import com.packup.data.local.entity.MorningItemEntity
import com.packup.data.local.entity.MorningItemStatus
import com.packup.data.local.entity.PackingItemEntity
import com.packup.data.seed.SeedData
import com.packup.data.sync.FirestoreSyncManager
import com.packup.data.sync.PhotoStorageManager
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackingRepository @Inject constructor(
    private val db: PackUpDatabase,
    private val familyMemberDao: FamilyMemberDao,
    private val packingItemDao: PackingItemDao,
    private val morningItemDao: MorningItemDao,
    private val categoryDao: CategoryDao,
    private val syncManager: FirestoreSyncManager,
    private val photoStorageManager: PhotoStorageManager,
    private val devicePreferences: DevicePreferences,
) {
    private var deviceId: String = ""

    private suspend fun ensureDeviceId(): String {
        if (deviceId.isEmpty()) deviceId = devicePreferences.getDeviceId()
        return deviceId
    }

    val allMembers: Flow<List<FamilyMemberEntity>> = familyMemberDao.getAllMembers()
    val allPackingItems: Flow<List<PackingItemEntity>> = packingItemDao.getAllItems()
    val snoozedItems: Flow<List<PackingItemEntity>> = packingItemDao.getSnoozedItems()
    val allMorningItems: Flow<List<MorningItemEntity>> = morningItemDao.getAllItems()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    fun itemsForMember(memberId: String): Flow<List<PackingItemEntity>> =
        packingItemDao.getItemsForMember(memberId)

    // --- Packing item operations ---

    suspend fun toggleItemDone(itemId: String, currentStatus: ItemStatus) {
        val did = ensureDeviceId()
        val newStatus = if (currentStatus == ItemStatus.DONE) ItemStatus.TODO else ItemStatus.DONE
        val now = System.currentTimeMillis()
        packingItemDao.updateStatus(itemId, newStatus, now, did)
        packingItemDao.getById(itemId)?.let { syncManager.pushPackingItem(it) }
    }

    suspend fun snoozeItem(itemId: String) {
        val did = ensureDeviceId()
        val now = System.currentTimeMillis()
        packingItemDao.updateStatus(itemId, ItemStatus.SNOOZED, now, did)
        packingItemDao.getById(itemId)?.let { syncManager.pushPackingItem(it) }
    }

    suspend fun unsnoozeItem(itemId: String) {
        val did = ensureDeviceId()
        val now = System.currentTimeMillis()
        packingItemDao.updateStatus(itemId, ItemStatus.TODO, now, did)
        packingItemDao.getById(itemId)?.let { syncManager.pushPackingItem(it) }
    }

    suspend fun toggleSnoozedDone(itemId: String, currentStatus: ItemStatus) {
        val did = ensureDeviceId()
        val newStatus = if (currentStatus == ItemStatus.DONE) ItemStatus.SNOOZED else ItemStatus.DONE
        val now = System.currentTimeMillis()
        packingItemDao.updateStatus(itemId, newStatus, now, did)
        packingItemDao.getById(itemId)?.let { syncManager.pushPackingItem(it) }
    }

    suspend fun addItem(name: String, category: String, memberId: String) {
        val did = ensureDeviceId()
        val now = System.currentTimeMillis()
        val item = PackingItemEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            category = category,
            memberId = memberId,
            sortOrder = (now % Int.MAX_VALUE).toInt(),
            createdAt = now,
            updatedAt = now,
            updatedByDeviceId = did,
        )
        packingItemDao.insert(item)
        syncManager.pushPackingItem(item)
    }

    suspend fun editItem(itemId: String, newName: String) {
        val did = ensureDeviceId()
        val now = System.currentTimeMillis()
        packingItemDao.updateName(itemId, newName, now, did)
        packingItemDao.getById(itemId)?.let { syncManager.pushPackingItem(it) }
    }

    suspend fun deleteItem(itemId: String) {
        packingItemDao.delete(itemId)
        syncManager.pushDelete("packing_items", itemId)
    }

    suspend fun markAllDoneInCategory(memberId: String, category: String) {
        val did = ensureDeviceId()
        val now = System.currentTimeMillis()
        packingItemDao.markAllDoneInCategory(memberId, category, ItemStatus.DONE, now, did)
        packingItemDao.getItemsForMemberAndCategory(memberId, category).forEach {
            syncManager.pushPackingItem(it)
        }
    }

    // --- Morning item operations ---

    suspend fun toggleMorningItemDone(itemId: String, currentStatus: MorningItemStatus) {
        val did = ensureDeviceId()
        val newStatus = if (currentStatus == MorningItemStatus.DONE) MorningItemStatus.TODO else MorningItemStatus.DONE
        val now = System.currentTimeMillis()
        morningItemDao.updateStatus(itemId, newStatus, now, did)
        morningItemDao.getById(itemId)?.let { syncManager.pushMorningItem(it) }
    }

    suspend fun addMorningItem(name: String, category: String = "General") {
        val did = ensureDeviceId()
        val now = System.currentTimeMillis()
        val item = MorningItemEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            category = category,
            sortOrder = (now % Int.MAX_VALUE).toInt(),
            createdAt = now,
            updatedAt = now,
            updatedByDeviceId = did,
        )
        morningItemDao.insert(item)
        syncManager.pushMorningItem(item)
    }

    suspend fun editMorningItem(itemId: String, newName: String) {
        val did = ensureDeviceId()
        val now = System.currentTimeMillis()
        morningItemDao.updateName(itemId, newName, now, did)
        morningItemDao.getById(itemId)?.let { syncManager.pushMorningItem(it) }
    }

    suspend fun deleteMorningItem(itemId: String) {
        morningItemDao.delete(itemId)
        syncManager.pushDelete("morning_items", itemId)
    }

    // --- Family member operations ---

    suspend fun addMember(name: String, iconKey: String) {
        val did = ensureDeviceId()
        val now = System.currentTimeMillis()
        val avatar = name.take(1).uppercase()
        val member = FamilyMemberEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            avatar = avatar,
            iconKey = iconKey,
            sortOrder = (now % Int.MAX_VALUE).toInt(),
            createdAt = now,
            updatedAt = now,
            updatedByDeviceId = did,
        )
        familyMemberDao.insert(member)
        syncManager.pushFamilyMember(member)
    }

    suspend fun renameMember(id: String, name: String) {
        val did = ensureDeviceId()
        val now = System.currentTimeMillis()
        val avatar = name.take(1).uppercase()
        familyMemberDao.updateName(id, name, avatar, now, did)
        familyMemberDao.getById(id)?.let { syncManager.pushFamilyMember(it) }
    }

    suspend fun setMemberIcon(id: String, iconKey: String) {
        val did = ensureDeviceId()
        val now = System.currentTimeMillis()
        familyMemberDao.updateIcon(id, iconKey, now, did)
        familyMemberDao.getById(id)?.let { syncManager.pushFamilyMember(it) }
    }

    suspend fun setMemberPhoto(id: String, photoUri: String) {
        val did = ensureDeviceId()
        val now = System.currentTimeMillis()
        val urlOrPath = photoStorageManager.uploadMemberPhoto(id, photoUri)
        familyMemberDao.updatePhotoUri(id, urlOrPath, now, did)
        if (urlOrPath.startsWith("http")) {
            familyMemberDao.getById(id)?.let { syncManager.pushFamilyMemberAndAwait(it) }
        }
    }

    suspend fun moveMemberUp(id: String) {
        val members = familyMemberDao.getAllMembersSync()
        val index = members.indexOfFirst { it.id == id }
        if (index > 0) {
            val did = ensureDeviceId()
            val now = System.currentTimeMillis()
            val current = members[index]
            val previous = members[index - 1]

            val currentOrder = current.sortOrder
            val prevOrder = previous.sortOrder
            
            // If they happen to have the exact same sort order, we need to artificially adjust
            val newCurrentOrder = if (currentOrder == prevOrder) currentOrder - 1 else prevOrder
            val newPrevOrder = if (currentOrder == prevOrder) currentOrder else currentOrder

            familyMemberDao.updateSortOrder(current.id, newCurrentOrder, now, did)
            familyMemberDao.updateSortOrder(previous.id, newPrevOrder, now, did)

            familyMemberDao.getById(current.id)?.let { syncManager.pushFamilyMember(it) }
            familyMemberDao.getById(previous.id)?.let { syncManager.pushFamilyMember(it) }
        }
    }

    suspend fun moveMemberDown(id: String) {
        val members = familyMemberDao.getAllMembersSync()
        val index = members.indexOfFirst { it.id == id }
        if (index != -1 && index < members.size - 1) {
            val did = ensureDeviceId()
            val now = System.currentTimeMillis()
            val current = members[index]
            val next = members[index + 1]

            val currentOrder = current.sortOrder
            val nextOrder = next.sortOrder

            val newCurrentOrder = if (currentOrder == nextOrder) currentOrder + 1 else nextOrder
            val newNextOrder = if (currentOrder == nextOrder) currentOrder else currentOrder

            familyMemberDao.updateSortOrder(current.id, newCurrentOrder, now, did)
            familyMemberDao.updateSortOrder(next.id, newNextOrder, now, did)

            familyMemberDao.getById(current.id)?.let { syncManager.pushFamilyMember(it) }
            familyMemberDao.getById(next.id)?.let { syncManager.pushFamilyMember(it) }
        }
    }

    suspend fun deleteMember(id: String) {
        val itemsToDelete = packingItemDao.getAllItemsSync().filter { it.memberId == id }
        db.withTransaction {
            familyMemberDao.delete(id)
            packingItemDao.deleteByMember(id)
        }
        syncManager.pushDelete("family_members", id)
        itemsToDelete.forEach { syncManager.pushDelete("packing_items", it.id) }
    }

    // --- Category operations ---

    suspend fun addCategory(name: String, iconKey: String) {
        val did = ensureDeviceId()
        val now = System.currentTimeMillis()
        val entity = CategoryEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            iconKey = iconKey,
            sortOrder = (now % Int.MAX_VALUE).toInt(),
            createdAt = now,
            updatedAt = now,
            updatedByDeviceId = did,
        )
        categoryDao.insert(entity)
        syncManager.pushCategory(entity)
    }

    suspend fun renameCategory(oldName: String, newName: String) {
        val did = ensureDeviceId()
        val now = System.currentTimeMillis()
        db.withTransaction {
            categoryDao.rename(oldName, newName, now, did)
            packingItemDao.updateCategory(oldName, newName, now, did)
            morningItemDao.updateCategory(oldName, newName, now, did)
        }

        categoryDao.getByName(newName)?.let { syncManager.pushCategory(it) }
        packingItemDao.getItemsByCategory(newName).forEach { syncManager.pushPackingItem(it) }
        morningItemDao.getItemsByCategory(newName).forEach { syncManager.pushMorningItem(it) }
    }

    suspend fun deleteCategory(name: String) {
        val did = ensureDeviceId()
        val now = System.currentTimeMillis()
        val catEntity = categoryDao.getByName(name)
        db.withTransaction {
            categoryDao.delete(name)
            packingItemDao.updateCategory(name, "General", now, did)
            morningItemDao.updateCategory(name, "General", now, did)
        }

        if (catEntity != null) syncManager.pushDelete("categories", catEntity.id)
        packingItemDao.getItemsByCategory("General").forEach { syncManager.pushPackingItem(it) }
        morningItemDao.getItemsByCategory("General").forEach { syncManager.pushMorningItem(it) }
    }

    suspend fun setCategoryIcon(name: String, iconKey: String) {
        val did = ensureDeviceId()
        val now = System.currentTimeMillis()
        categoryDao.setIcon(name, iconKey, now, did)
        categoryDao.getByName(name)?.let { syncManager.pushCategory(it) }
    }

    // --- Seed / reset ---

    suspend fun seedIfEmpty() {
        if (categoryDao.count() == 0) {
            db.withTransaction {
                categoryDao.insertAll(SeedData.categories)
                familyMemberDao.insertAll(SeedData.familyMembers)
                packingItemDao.insertAll(SeedData.packingItems)
                morningItemDao.insertAll(SeedData.morningItems)
            }
        }
    }

    suspend fun seedAndPush() {
        seedIfEmpty()
        syncManager.pushAllSeedData(
            categories = categoryDao.getAllCategoriesSync(),
            familyMembers = familyMemberDao.getAllMembersSync(),
            packingItems = packingItemDao.getAllItemsSync(),
            morningItems = morningItemDao.getAllItemsSync(),
        )
    }

    suspend fun resetAll() {
        val did = ensureDeviceId()
        val now = System.currentTimeMillis()
        db.withTransaction {
            packingItemDao.resetAllStatuses(now, did)
            morningItemDao.resetAllStatuses(now, did)
        }
        packingItemDao.getAllItemsSync().forEach { syncManager.pushPackingItem(it) }
        morningItemDao.getAllItemsSync().forEach { syncManager.pushMorningItem(it) }
    }
}
