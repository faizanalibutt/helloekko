package com.ekku.nfc.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

const val TAG_PREF = "tag_prefs"

fun Context.getDefaultPreferences(): SharedPreferences {
    return getSharedPreferences(TAG_PREF, MODE_PRIVATE)
}