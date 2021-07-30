package com.ekku.nfc.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.auth0.android.jwt.Claim
import com.auth0.android.jwt.JWT
import com.ekku.nfc.ui.activity.AccountActivity.Companion.LOGIN_MODE
import com.ekku.nfc.ui.activity.AccountActivity.Companion.LOGIN_PREF
import com.ekku.nfc.ui.activity.AccountActivity.Companion.LOGIN_TOKEN
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.APP_MODE
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.FIRST_TIME
import timber.log.Timber


const val TAG_PREF = "tag_prefs"

fun Context.getDefaultPreferences(): SharedPreferences {
    return getSharedPreferences(TAG_PREF, MODE_PRIVATE)
}

fun Context.savePrefs(appType: Int, isFirst: Boolean) {
    getDefaultPreferences().edit().putBoolean(FIRST_TIME, isFirst).apply()
    getDefaultPreferences().edit().putInt(APP_MODE, appType).apply()
}

fun Activity.savePrefs(login_status: Boolean, app_mode: Int, token_string: String) {
    getDefaultPreferences().edit().putBoolean(LOGIN_PREF, login_status).apply()
    getDefaultPreferences().edit().putInt(LOGIN_MODE, app_mode).apply()
    getDefaultPreferences().edit().putString(LOGIN_TOKEN, token_string).apply()
}

fun Context.hideSystemKeyboard(activity: Activity?) {
    val inputManager: InputMethodManager? =
        this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
    inputManager?.hideSoftInputFromWindow(
        activity?.currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS
    )
}

fun Context.myToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.getDataFromToken(tokenName: String, authToken: String?): Claim? {
    // get information from token.
    val jwtTokenDecoder = authToken?.let { JWT(it) }
    return jwtTokenDecoder?.getClaim(tokenName)
}

fun Activity.askForPermission() {
    ActivityCompat.requestPermissions(
        this, arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        ), 1001
    )
}