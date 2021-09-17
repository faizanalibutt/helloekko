package com.ekku.nfc.model

import com.google.gson.annotations.SerializedName

data class ItemShell(
    @SerializedName("error")
    val error: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val items: List<Item>
)