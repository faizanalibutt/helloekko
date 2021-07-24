package com.ekku.nfc.ui.activity

import android.os.Bundle
import androidx.activity.viewModels
import com.ekku.nfc.app.BaseActivity
import com.ekku.nfc.databinding.ActivityWelcomeBinding
import com.ekku.nfc.ui.viewmodel.WelcomeViewModel
import com.ekku.nfc.util.AppUtils.startActivity
import com.ekku.nfc.util.savePrefs

class WelcomeActivity : BaseActivity() {

    private lateinit var welcomeBinding: ActivityWelcomeBinding
    private val welcomeViewModel: WelcomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        welcomeBinding = ActivityWelcomeBinding.inflate(layoutInflater)
        val view = welcomeBinding.root
        setContentView(view)
        welcomeBinding.btnRestaurant.setOnClickListener {
            handleButtonAction()
            savePrefs( RESTAURANT, true)
        }
        welcomeBinding.btnConsumer.setOnClickListener {
            handleButtonAction()
            savePrefs(CONSUMER, true)
        }
        welcomeBinding.btnAdmin.setOnClickListener {
            handleButtonAction()
            savePrefs(ADMIN, true)
        }
    }

    private fun handleButtonAction() {
        startActivity<AccountActivity>()
        finish()
    }

    companion object {
        const val RESTAURANT = 0
        const val CONSUMER = 1
        const val ADMIN = 2
        const val APP_MODE = "app_mode"
        const val FIRST_TIME = "first_time"
    }
}