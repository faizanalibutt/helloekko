package com.ekku.nfc.model

import com.google.gson.annotations.SerializedName

data class DropBoxShell(
    @SerializedName("error")
    val error: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val dropBoxes: List<DropBox>
)