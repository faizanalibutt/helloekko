package com.ekku.nfc.model

import com.google.gson.annotations.SerializedName

data class Partner(
    @SerializedName("id")
    val id: String,
    @SerializedName("partnerName")
    val partnerName: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("phoneNo")
    val phoneNo: String,
    @SerializedName("region")
    val region: String,
    @SerializedName("pLogo")
    val pLogo: String,
)