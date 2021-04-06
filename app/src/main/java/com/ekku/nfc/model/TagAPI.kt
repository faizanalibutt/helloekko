package com.ekku.nfc.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class TagAPI(
    @Expose
    @SerializedName("id")
    val id: Int = 0,
    @Expose
    @SerializedName("tag_uid")
    val tag_uid: String,
    @Expose
    @SerializedName("tag_data_time")
    val tag_date_time: String,
    @Expose
    @SerializedName("tag_phone_uid")
    val tag_phone_uid: String,
    @Expose
    @SerializedName("tag_sync")
    var tag_sync: Int
) : Parcelable
