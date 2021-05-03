package com.ekku.nfc.model

import androidx.annotation.WorkerThread
import com.ekku.nfc.util.TimeUtils
import kotlinx.coroutines.flow.Flow

class TagRepository(private val tagDao: TagDao) {

    private var previousDay = ""

    constructor(tagDao: TagDao, previousDay: String): this(tagDao) {
        this.previousDay = previousDay
    }

    val allTags: Flow<List<TagAPI>> = tagDao.getTagList()
    val syncTags: Flow<List<TagAPI>> = tagDao.getSyncedTagList()
    val todayTags: Flow<List<TagAPI>> = tagDao.getTodayTagList(
        TimeUtils.getFormatDate(TimeUtils.getPreviousDay())
    )

    @WorkerThread
    suspend fun insert(tag: Tag) {
        tagDao.insert(tag)
    }

    suspend fun update(tag: Tag) {
        tagDao.update(tag)
    }

    suspend fun update(tagUpdate: TagDao.TagUpdate) {
        tagDao.update(tagUpdate)
    }

}