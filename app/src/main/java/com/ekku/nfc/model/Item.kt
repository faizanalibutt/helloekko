package com.ekku.nfc.model

import com.google.gson.annotations.SerializedName

class Item(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String
)