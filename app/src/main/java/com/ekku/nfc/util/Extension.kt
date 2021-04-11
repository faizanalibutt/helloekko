package com.ekku.nfc.util

import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.view.inputmethod.InputMethodManager


const val TAG_PREF = "tag_prefs"

fun Context.getDefaultPreferences(): SharedPreferences {
    return getSharedPreferences(TAG_PREF, MODE_PRIVATE)
}

fun Context.hideSystemKeyboard(activity: Activity?) {
    val inputManager: InputMethodManager? =
        this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
    inputManager?.hideSoftInputFromWindow(
        activity?.currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS
    )
}