package com.ekku.nfc.model

import com.google.gson.annotations.SerializedName

data class GenericResponse(
    @SerializedName("error")
    val error: Boolean,
    @SerializedName("message")
    val message: String
)