package com.ekku.nfc.model

import com.google.gson.annotations.SerializedName

data class Account(
    @SerializedName("token")
    var token: String = "",
    @SerializedName("error")
    val error: Boolean,
    @SerializedName("message")
    val message: String,
)