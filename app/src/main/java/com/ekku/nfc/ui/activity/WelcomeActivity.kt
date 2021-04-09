package com.ekku.nfc.ui.activity

import android.os.Bundle
import androidx.activity.viewModels
import com.ekku.nfc.app.BaseActivity
import com.ekku.nfc.databinding.ActivityWelcomeBinding
import com.ekku.nfc.ui.viewmodel.WelcomeViewModel
import com.ekku.nfc.util.getDefaultPreferences

class WelcomeActivity : BaseActivity() {

    private var welcomeBinding: ActivityWelcomeBinding? = null
    private val welcomeViewModel: WelcomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        welcomeBinding = ActivityWelcomeBinding.inflate(layoutInflater)
        val view = welcomeBinding?.root
        setContentView(view)
        welcomeBinding?.btnRestaurant?.setOnClickListener {
            getDefaultPreferences().edit().putBoolean("FIRST_TIME", true).apply()
            getDefaultPreferences().edit().putInt("APP_TYPE", 0).apply()
            welcomeViewModel.handleButtonAction(this@WelcomeActivity)
        }
        welcomeBinding?.btnConsumer?.setOnClickListener {
            getDefaultPreferences().edit().putBoolean("FIRST_TIME", true).apply()
            getDefaultPreferences().edit().putInt("APP_TYPE", 1).apply()
            welcomeViewModel.handleButtonAction(this@WelcomeActivity)
        }
    }
}