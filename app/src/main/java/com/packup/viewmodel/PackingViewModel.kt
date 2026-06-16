package com.packup.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.packup.data.local.DevicePreferences
import com.packup.data.local.entity.CategoryEntity
import com.packup.data.local.entity.FamilyMemberEntity
import com.packup.data.local.entity.ItemStatus
import com.packup.data.local.entity.MorningItemEntity
import com.packup.data.local.entity.MorningItemStatus
import com.packup.data.local.entity.PackingItemEntity
import com.packup.data.repository.PackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemberWithItems(
    val member: FamilyMemberEntity,
    val items: List<PackingItemEntity>
) {
    val totalCount get() = items.size
    val doneCount get() = items.count { it.status == ItemStatus.DONE }
    val snoozedCount get() = items.count { it.status == ItemStatus.SNOOZED }
    val handledCount get() = doneCount + snoozedCount
    val remaining get() = totalCount - handledCount
    val allDone get() = handledCount == totalCount && totalCount > 0
}

@HiltViewModel
class PackingViewModel @Inject constructor(
    private val repository: PackingRepository,
    private val devicePreferences: DevicePreferences,
) : ViewModel() {

    private val _activeMemberId = MutableStateFlow("mom")

    val familyId: StateFlow<String?> = devicePreferences.familyIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val activeMemberId: StateFlow<String> = _activeMemberId.asStateFlow()

    val isMorningView: StateFlow<Boolean> = combine(_activeMemberId) { ids ->
        ids[0] == "morning"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val membersWithItems: StateFlow<List<MemberWithItems>> = combine(
        repository.allMembers,
        repository.allPackingItems
    ) { members, items ->
        val grouped = items.groupBy { it.memberId }
        members.map { member ->
            MemberWithItems(member, grouped[member.id] ?: emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val morningItems: StateFlow<List<MorningItemEntity>> =
        repository.allMorningItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val snoozedItems: StateFlow<List<PackingItemEntity>> =
        repository.snoozedItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> =
        repository.allCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMemberWithItems: StateFlow<MemberWithItems?> = combine(
        _activeMemberId,
        membersWithItems
    ) { id, members ->
        members.find { it.member.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        var hasSetInitialSelection = false
        membersWithItems
            .onEach { members ->
                if (members.isEmpty()) return@onEach
                val firstInCarousel = members
                    .filter { !it.allDone }
                    .firstOrNull()?.member?.id ?: "morning"
                val currentId = _activeMemberId.value
                val currentValid = currentId == "morning" || members.any { it.member.id == currentId }
                if (!hasSetInitialSelection) {
                    _activeMemberId.value = firstInCarousel
                    hasSetInitialSelection = true
                } else if (!currentValid) {
                    _activeMemberId.value = firstInCarousel
                }
            }
            .launchIn(viewModelScope)
    }

    val totalMorningCount: StateFlow<Int> = combine(
        snoozedItems,
        morningItems
    ) { snoozed, morning ->
        snoozed.size + morning.count { it.status == MorningItemStatus.TODO }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun selectMember(id: String) {
        _activeMemberId.value = id
    }

    // --- Packing item actions ---

    fun toggleDone(item: PackingItemEntity) {
        viewModelScope.launch { repository.toggleItemDone(item.id, item.status) }
    }

    fun snoozeItem(itemId: String) {
        viewModelScope.launch { repository.snoozeItem(itemId) }
    }

    fun unsnoozeItem(itemId: String) {
        viewModelScope.launch { repository.unsnoozeItem(itemId) }
    }

    fun toggleSnoozedDone(item: PackingItemEntity) {
        viewModelScope.launch { repository.toggleSnoozedDone(item.id, item.status) }
    }

    fun addItem(name: String, category: String) {
        val memberId = _activeMemberId.value
        if (memberId == "morning") return
        viewModelScope.launch { repository.addItem(name, category, memberId) }
    }

    fun editItem(itemId: String, newName: String) {
        viewModelScope.launch { repository.editItem(itemId, newName) }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch { repository.deleteItem(itemId) }
    }

    fun markAllDoneInCategory(category: String) {
        val memberId = _activeMemberId.value
        if (memberId == "morning") return
        viewModelScope.launch { repository.markAllDoneInCategory(memberId, category) }
    }

    // --- Morning item actions ---

    fun toggleMorningItemDone(item: MorningItemEntity) {
        viewModelScope.launch { repository.toggleMorningItemDone(item.id, item.status) }
    }

    fun addMorningItem(name: String) {
        viewModelScope.launch { repository.addMorningItem(name) }
    }

    fun editMorningItem(itemId: String, newName: String) {
        viewModelScope.launch { repository.editMorningItem(itemId, newName) }
    }

    fun deleteMorningItem(itemId: String) {
        viewModelScope.launch { repository.deleteMorningItem(itemId) }
    }

    // --- Family member actions ---

    fun addMember(name: String, iconKey: String) {
        viewModelScope.launch { repository.addMember(name, iconKey) }
    }

    fun renameMember(id: String, name: String) {
        viewModelScope.launch { repository.renameMember(id, name) }
    }

    fun setMemberIcon(id: String, iconKey: String) {
        viewModelScope.launch { repository.setMemberIcon(id, iconKey) }
    }

    fun setMemberPhoto(id: String, photoUri: String) {
        viewModelScope.launch { repository.setMemberPhoto(id, photoUri) }
    }

    fun moveMemberUp(id: String) {
        viewModelScope.launch { repository.moveMemberUp(id) }
    }

    fun moveMemberDown(id: String) {
        viewModelScope.launch { repository.moveMemberDown(id) }
    }

    fun deleteMember(id: String) {
        if (_activeMemberId.value == id) {
            val fallback = membersWithItems.value
                .firstOrNull { it.member.id != id }?.member?.id ?: "morning"
            _activeMemberId.value = fallback
        }
        viewModelScope.launch { repository.deleteMember(id) }
    }

    // --- Category actions ---

    fun addCategory(name: String, iconKey: String) {
        viewModelScope.launch { repository.addCategory(name, iconKey) }
    }

    fun renameCategory(oldName: String, newName: String) {
        viewModelScope.launch { repository.renameCategory(oldName, newName) }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch { repository.deleteCategory(name) }
    }

    fun setCategoryIcon(name: String, iconKey: String) {
        viewModelScope.launch { repository.setCategoryIcon(name, iconKey) }
    }

    // --- Reset ---

    fun resetAll() {
        viewModelScope.launch { repository.resetAll() }
    }
}
