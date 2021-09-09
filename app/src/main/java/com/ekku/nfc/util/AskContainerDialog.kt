package com.ekku.nfc.util

import android.app.Activity
import android.view.LayoutInflater
import android.widget.EditText
import com.ekku.nfc.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AskContainerDialog(context: Activity) : MaterialAlertDialogBuilder(context) {
    var containerNo: Int = 0
    init {
        kotlin.runCatching {
            val mRootView =
                LayoutInflater.from(context).inflate(R.layout.layout_container_dialog, null, false)
            setView(mRootView)
            val fieldContainer = mRootView.findViewById<EditText>(R.id.field_container_number)
            setPositiveButton(context.getString(R.string.text_set)
            ) { dialog, _ ->
                containerNo = fieldContainer.text.toString().toInt()
            }
            setNegativeButton(context.getString(R.string.text_cancel), null)
            return@runCatching
        }.onFailure {

        }
    }
}