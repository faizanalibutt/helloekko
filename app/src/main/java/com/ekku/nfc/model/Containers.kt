package com.ekku.nfc.model

import com.google.gson.annotations.SerializedName

data class Containers(
    @SerializedName("containers")
    var containers: MutableList<Container>
)