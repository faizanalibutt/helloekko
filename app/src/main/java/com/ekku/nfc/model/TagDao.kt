package com.ekku.nfc.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT id, tag_uid, tag_date_time, tag_phone_uid, tag_sync FROM tag_table ORDER BY Id ASC")
    fun getTagList(): Flow<List<TagAPI>>

    @Query("SELECT id, tag_uid, tag_date_time, tag_phone_uid, tag_sync FROM tag_table where tag_date = :endDate ORDER BY Id ASC")
    fun getTodayTagList(endDate: String): Flow<List<TagAPI>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tag: Tag)

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(tag: Tag)

    @Update(entity = Tag::class)
    suspend fun update(obj: TagUpdate)

    @Entity
    data class TagUpdate(
        val id: Int,
        val tag_sync: Int
    )

}