package com.ekku.nfc.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.core.view.get
import com.ekku.nfc.R
import com.ekku.nfc.databinding.ActivityAccountBinding
import com.ekku.nfc.repository.AccountRepository
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.ADMIN
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.APP_MODE
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.CONSUMER
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.RESTAURANT
import com.ekku.nfc.ui.viewmodel.AccountViewModel
import com.ekku.nfc.util.AppUtils.startActivity
import com.ekku.nfc.util.getDefaultPreferences
import com.ekku.nfc.util.savePrefs
import timber.log.Timber

class AccountActivity : AppCompatActivity() {

    private lateinit var accountBinding: ActivityAccountBinding
    private val accountViewModel: AccountViewModel by viewModels {
        AccountViewModel.AccountViewModelFactory(AccountRepository())
    }

    // it will be replaced with live data but for now use it.
    var adminMode: String = "Fleet"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountBinding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(accountBinding.root)

        // add items to admin mode spinner
        ArrayAdapter.createFromResource(
            this@AccountActivity,
            R.array.admin_modes_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            accountBinding.spinnerMode.adapter = adapter
        }

        accountBinding.spinnerMode.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    // here we will get admin mode.
                    adminMode = parent?.getItemAtPosition(position).toString()
                    Timber.d("Admin Mode is : $adminMode")
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }

        // set up view model for showing hiding admin mode...
        val appMode = getDefaultPreferences().getInt(APP_MODE, -1)
        accountBinding.adminModeGroup.visibility = if (appMode == ADMIN) View.VISIBLE else View.GONE

        accountBinding.appMode.setOnClickListener {
            savePrefs(-1, false)
            startActivity<WelcomeActivity>()
            finish()
        }

        accountBinding.loginButton.setOnClickListener {
            // after sending login things move to respective screen
            when (appMode) {
                ADMIN -> {
                    accountBinding.progressBar.visibility = View.VISIBLE
                    Handler(Looper.getMainLooper()).postDelayed({
                        accountBinding.progressBar.visibility = View.GONE
                        getDefaultPreferences().edit().putString(ADMIN_MODE, adminMode).apply()
                        startActivity<AdminActivity>()
                        finish()
                        savePrefs(true, ADMIN, "coming-home")
                    }, 3000)
                }
                RESTAURANT -> {
                    startActivity<RestaurantActivity>()
                }
                CONSUMER -> {
                    startActivity<ConsumerActivity>()
                }
            }
        }

    }

    companion object {
        const val LOGIN_PREF = "logged_in"
        const val LOGIN_MODE = "login_mode"
        const val LOGIN_TOKEN = "login_token"
        const val ADMIN_MODE = "admin_mode"
    }

}