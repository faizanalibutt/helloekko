package com.ekku.nfc.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tag_table")
data class Tag(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int = 0,
    val tag_uid: String,
    val tag_time: Long,
    val tag_date: String,
    val tag_date_time: String,
    val tag_phone_uid: String,
    var tag_sync: Int,
    var tag_orderId: String = "random_order_id"
)