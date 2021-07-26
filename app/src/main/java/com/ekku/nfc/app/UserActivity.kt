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

open class UserActivity : AppCompatActivity() {

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