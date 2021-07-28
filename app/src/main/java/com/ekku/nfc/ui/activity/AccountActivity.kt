package com.ekku.nfc.ui.activity

import android.net.Network
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isEmpty
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.ekku.nfc.R
import com.ekku.nfc.databinding.ActivityAccountBinding
import com.ekku.nfc.model.Account
import com.ekku.nfc.repository.AccountRepository
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.ADMIN
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.APP_MODE
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.DROPBOX
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.PARTNER
import com.ekku.nfc.ui.viewmodel.AccountViewModel
import com.ekku.nfc.util.*
import com.ekku.nfc.util.AppUtils.startActivity
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

class AccountActivity : AppCompatActivity() {

    private lateinit var accountBinding: ActivityAccountBinding

    // for api members we will use view model and other things might come in future.
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
            if (isReady && NetworkUtils.isOnline(this)) {
                when (appMode) {
                    ADMIN -> {
                        accountBinding.progressBar.visibility = View.VISIBLE
                        // api calling
                        accountViewModel.postAdminCredentials(
                            usernameField.editText?.text.toString(),
                            passwordField.editText?.text.toString()
                        ).observe(this, Observer {
                            it?.let { result ->
                                when (result.status) {
                                    Status.SUCCESS -> {
                                        accountBinding.progressBar.visibility = View.GONE
                                        result.data?.let { account ->
                                            Timber.d("admin token came $account")
                                            getDefaultPreferences().edit()
                                                .putString(ADMIN_MODE, adminMode).apply()
                                            savePrefs(login_status = true, ADMIN, account.token)
                                            startActivity<AdminActivity>()
                                            finish()
                                        }
                                    }
                                    Status.ERROR -> {
                                        Timber.d("issue came ${result.message}")
                                        accountBinding.progressBar.visibility = View.GONE
                                        Snackbar.make(
                                            accountBinding.root, "${result.message}",
                                            Snackbar.LENGTH_LONG
                                        ).show()
                                    }
                                    Status.LOADING -> {}
                                }
                            }
                        })
                    }
                    PARTNER -> {
                        accountBinding.progressBar.visibility = View.VISIBLE
                        // api calling
                        accountViewModel.postPartnerCredentials(
                            usernameField.editText?.text.toString(),
                            passwordField.editText?.text.toString()
                        ).observe(this, {
                            it?.let { result ->
                                when (result.status) {
                                    Status.SUCCESS -> {
                                        accountBinding.progressBar.visibility = View.GONE
                                        result.data?.let { account ->
                                            Timber.d("partner token came $account")
                                            savePrefs(login_status = true, PARTNER, account.token)
                                            startActivity<PartnerActivity>()
                                            finish()
                                        }
                                    }
                                    Status.ERROR -> {
                                        Timber.d("issue came ${result.message}")
                                        Snackbar.make(
                                            accountBinding.root, "${result.message}",
                                            Snackbar.LENGTH_LONG
                                        ).show()
                                        accountBinding.progressBar.visibility = View.GONE
                                    }
                                    Status.LOADING -> {}
                                }
                            }
                        })
                    }
                    DROPBOX -> {
                        accountBinding.progressBar.visibility = View.GONE
                        // api calling
                        accountViewModel.postDropBoxCredentials(
                            usernameField.editText?.text.toString()
                        ).observe(this, {
                            it?.let { result ->
                                when (result.status) {
                                    Status.SUCCESS -> {
                                        accountBinding.progressBar.visibility = View.GONE
                                        result.data?.let { account ->
                                            Timber.d("dropbox token came $account")
                                            savePrefs(login_status = true, DROPBOX, account.token)
                                            startActivity<DropBoxActivity>()
                                            finish()
                                        }
                                    }
                                    Status.ERROR -> {
                                        Timber.d("issue came ${result.message}")
                                        accountBinding.progressBar.visibility = View.GONE
                                        Snackbar.make(
                                            accountBinding.root, "${result.message}",
                                            Snackbar.LENGTH_LONG
                                        ).show()
                                    }
                                    Status.LOADING -> {}
                                }
                            }
                        })
                    }
                }
                hideSystemKeyboard(this@AccountActivity)
            } else
                Snackbar.make(accountBinding.root,
                    getString(R.string.text_no_wifi), Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showViews(appMode: Int) {
        accountBinding.adminModeGroup.visibility =
            if (appMode == ADMIN) View.VISIBLE else View.GONE
        accountBinding.passwordField.visibility =
            if (appMode == DROPBOX) View.GONE else View.VISIBLE
    }

    companion object {
        // TODO: 7/28/21 handle message coming from server on wrong username/password
        const val LOGIN_PREF = "logged_in"
        const val LOGIN_MODE = "login_mode"
        const val LOGIN_TOKEN = "login_token"
        const val ADMIN_MODE = "admin_mode"
    }
}