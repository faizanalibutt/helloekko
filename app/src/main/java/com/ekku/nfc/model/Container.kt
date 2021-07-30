package com.ekku.nfc.model

import com.google.gson.annotations.SerializedName

data class Container(
    @SerializedName("containerId")
    val containerId: String,
    @SerializedName("containerSource")
    val containerSource: String,
    @SerializedName("containerType")
    val containerType: String,
    @SerializedName("containerSize")
    val containerSize: String,
    @SerializedName("region")
    val region: String,
)