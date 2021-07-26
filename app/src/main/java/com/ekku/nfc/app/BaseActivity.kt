package com.ekku.nfc.app

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.ekku.nfc.R
import com.ekku.nfc.ui.activity.*
import com.ekku.nfc.ui.activity.AccountActivity.Companion.LOGIN_PREF
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.ADMIN
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.APP_MODE
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.DROPBOX
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.FIRST_TIME
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.PARTNER
import com.ekku.nfc.util.AppUtils.startActivity
import com.ekku.nfc.util.getDefaultPreferences
import com.ekku.nfc.util.savePrefs
import timber.log.Timber

open class BaseActivity : AppCompatActivity() {

    private var isFirstLaunch = false
    private var appType = -1
    private var isLoggedIn = false

    override fun onResume() {
        super.onResume()
        // TODO: 4/8/21 implement set instead for clean code, wait for admin app completion
        isFirstLaunch = getDefaultPreferences().getBoolean(FIRST_TIME, false)
        appType = getDefaultPreferences().getInt(APP_MODE, -1)
        isLoggedIn = getDefaultPreferences().getBoolean(LOGIN_PREF, false)
        when {
            !isFirstLaunch -> Timber.d("it feels sad you haven't selected yet.")
            appType == PARTNER -> {
                if (isLoggedIn)
                    startActivity<RestaurantActivity>()
                else
                    startActivity<AccountActivity>()
                finish()
            }
            appType == DROPBOX -> {
                if (isLoggedIn)
                    startActivity<ConsumerActivity>()
                else
                    startActivity<AccountActivity>()
                finish()
            }
            appType == ADMIN -> {
                if (isLoggedIn)
                    startActivity<AdminActivity>()
                else
                    startActivity<AccountActivity>()
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.user_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out -> {
                finish()
                startActivity<WelcomeActivity>()
                savePrefs(-1, false)
                savePrefs(false, -1, "put-your-login-token-here")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}