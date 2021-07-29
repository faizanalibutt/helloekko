package com.ekku.nfc.model

import com.google.gson.annotations.SerializedName

data class Consumer(
    @SerializedName("id")
    val id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("region")
    val region: String,
    @SerializedName("phoneNo")
    val phoneNo: String
)