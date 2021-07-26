package com.ekku.nfc.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.core.view.contains
import androidx.core.view.isEmpty
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import com.ekku.nfc.R
import com.ekku.nfc.databinding.ActivityAccountBinding
import com.ekku.nfc.repository.AccountRepository
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.ADMIN
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.APP_MODE
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.DROPBOX
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.PARTNER
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
    // set up view model for showing hiding admin mode...
    private val appMode by lazy { getDefaultPreferences().getInt(APP_MODE, -1) }
    // it will be replaced with live data but for now use it.
    private lateinit var adminMode: String

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
                    adminMode = parent?.getItemAtPosition(position) as? String
                        ?: getString(R.string.text_fleet)
                    Timber.d("Admin Mode is : $adminMode")
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }

        // adjust login fragment according to app mode
        showViews(appMode)
        val usernameField = accountBinding.usernameField
        val passwordField = accountBinding.passwordField

        accountBinding.appMode.setOnClickListener {
            savePrefs(-1, false)
            startActivity<WelcomeActivity>()
            finish()
        }

        usernameField.editText?.doOnTextChanged { inputText, start, before, count ->
            when {
                inputText?.isEmpty() == true ->
                    usernameField.error = getString(R.string.text_email_empty)
                inputText?.contains("@") != true || !inputText.contains(".com") ->
                    usernameField.error = getString(R.string.text_invalid_email)
                else -> usernameField.error = null
            }
        }

        passwordField.editText?.doOnTextChanged { inputText, start, before, count ->
            inputText?.let {
                when {
                    inputText.isEmpty() ->
                        passwordField.error = getString(R.string.text_pwd_empty)
                    inputText.length < 6 ->
                        passwordField.error =
                            getString(R.string.text_pwd_limit)
                    else -> passwordField.error = null
                }
            }
        }

        accountBinding.loginButton.setOnClickListener {
            // look for anything that's remained and ready to go
            var isReady = true
            if ((passwordField.isEmpty() || passwordField.editText?.text.toString().length < 6)
                && passwordField.visibility == View.VISIBLE
            ) {
                passwordField.error = getString(R.string.text_pwd_empty)
                isReady = false
            }
            if (usernameField.isEmpty()
                || usernameField.editText?.text?.contains("@") != true
                || usernameField.editText?.text?.contains(".com") != true
            ) {
                usernameField.error = getString(R.string.text_email_empty)
                isReady = false
            }
            // after sending login things move to respective screen, call api
            if (isReady)
                when (appMode) {
                    ADMIN -> {
                        accountBinding.progressBar.visibility = View.VISIBLE
                        Handler(Looper.getMainLooper()).postDelayed({
                            accountBinding.progressBar.visibility = View.GONE
                            getDefaultPreferences().edit().putString(ADMIN_MODE, adminMode).apply()
                            savePrefs(true, ADMIN, "put-your-login-token-here")
                            startActivity<AdminActivity>()
                            finish()
                        }, 3000)
                    }
                    PARTNER -> {
                        accountBinding.progressBar.visibility = View.VISIBLE
                        Handler(Looper.getMainLooper()).postDelayed({
                            accountBinding.progressBar.visibility = View.GONE
                            savePrefs(true, PARTNER, "put-your-login-token-here")
                            startActivity<RestaurantActivity>()
                            finish()
                        }, 3000)
                    }
                    DROPBOX -> {
                        Handler(Looper.getMainLooper()).postDelayed({
                            accountBinding.progressBar.visibility = View.GONE
                            savePrefs(true, DROPBOX, "put-your-login-token-here")
                            startActivity<ConsumerActivity>()
                            finish()
                        }, 3000)
                    }
                }
        }
    }

    private fun showViews(appMode: Int) {
        accountBinding.adminModeGroup.visibility =
            if (appMode == ADMIN) View.VISIBLE else View.GONE
        accountBinding.passwordField.visibility =
            if (appMode == DROPBOX) View.GONE else View.VISIBLE
    }

    companion object {
        const val LOGIN_PREF = "logged_in"
        const val LOGIN_MODE = "login_mode"
        const val LOGIN_TOKEN = "login_token"
        const val ADMIN_MODE = "admin_mode"
    }
}