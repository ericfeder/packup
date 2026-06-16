package com.packup.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.packup.data.local.entity.FamilyMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members ORDER BY sortOrder ASC")
    fun getAllMembers(): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members ORDER BY sortOrder ASC")
    suspend fun getAllMembersSync(): List<FamilyMemberEntity>

    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getById(id: String): FamilyMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: FamilyMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<FamilyMemberEntity>)

    /** Updates a member in place. Use this when applying remote updates so we don't trigger FK CASCADE on packing_items. */
    @Query("""
        UPDATE family_members SET
            name = :name, avatar = :avatar, iconKey = :iconKey, photoUri = :photoUri,
            sortOrder = :sortOrder, createdAt = :createdAt, updatedAt = :updatedAt, updatedByDeviceId = :updatedByDeviceId
        WHERE id = :id
    """)
    suspend fun update(
        id: String,
        name: String,
        avatar: String,
        iconKey: String,
        photoUri: String,
        sortOrder: Int,
        createdAt: Long,
        updatedAt: Long,
        updatedByDeviceId: String
    )

    @Query("UPDATE family_members SET name = :name, avatar = :avatar, updatedAt = :now, updatedByDeviceId = :deviceId WHERE id = :id")
    suspend fun updateName(id: String, name: String, avatar: String, now: Long = System.currentTimeMillis(), deviceId: String = "")

    @Query("UPDATE family_members SET iconKey = :iconKey, updatedAt = :now, updatedByDeviceId = :deviceId WHERE id = :id")
    suspend fun updateIcon(id: String, iconKey: String, now: Long = System.currentTimeMillis(), deviceId: String = "")

    @Query("UPDATE family_members SET photoUri = :photoUri, updatedAt = :now, updatedByDeviceId = :deviceId WHERE id = :id")
    suspend fun updatePhotoUri(id: String, photoUri: String, now: Long = System.currentTimeMillis(), deviceId: String = "")

    @Query("UPDATE family_members SET sortOrder = :sortOrder, updatedAt = :now, updatedByDeviceId = :deviceId WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int, now: Long = System.currentTimeMillis(), deviceId: String = "")

    @Query("DELETE FROM family_members WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM family_members")
    suspend fun deleteAll()
}
