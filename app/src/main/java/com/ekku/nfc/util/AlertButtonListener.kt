package com.ekku.nfc.util

import android.content.DialogInterface

interface AlertButtonListener {
    enum class ButtonType {
        LEFT, RIGHT
    }
    fun onClick(dialog: DialogInterface, type: ButtonType)
}
