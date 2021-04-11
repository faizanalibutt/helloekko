package com.ekku.nfc.util

import android.content.DialogInterface

interface AlertButtonListener {
    fun onClick(dialog: DialogInterface, type: ButtonType)
}
