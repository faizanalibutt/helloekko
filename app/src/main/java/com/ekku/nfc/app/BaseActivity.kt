package com.ekku.nfc.app

import androidx.appcompat.app.AppCompatActivity
import com.ekku.nfc.ui.activity.ConsumerActivity
import com.ekku.nfc.ui.activity.MainActivity
import com.ekku.nfc.ui.activity.RestaurantActivity
import com.ekku.nfc.util.AppUtils.startActivity
import com.ekku.nfc.util.getDefaultPreferences
import timber.log.Timber

open class BaseActivity : AppCompatActivity() {

    private var isFirstLaunch = false
    private var appType = 0

    override fun onResume() {
        super.onResume()
        // TODO: 4/8/21 implement set instead for clean code
        isFirstLaunch = getDefaultPreferences().getBoolean("FIRST_TIME", false)
        appType = getDefaultPreferences().getInt("APP_TYPE", -1)
        when {
            !isFirstLaunch -> Timber.d("it feels sad you haven't selected yet.")
            appType == 0 -> {
                startActivity<RestaurantActivity>()
                finish()
            }
            appType == 1 -> {
                startActivity<MainActivity>()
                finish()
            }
        }
    }

}