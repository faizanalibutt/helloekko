package com.ekku.nfc.model

import com.google.gson.annotations.SerializedName

data class DropBox(
    @SerializedName("id")
    val id: String,
    @SerializedName("dropboxName")
    val dropboxName: String,
    @SerializedName("region")
    val region: String,
)