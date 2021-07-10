package com.ekku.nfc.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ekku.nfc.R
import com.ekku.nfc.databinding.ActivitySignInBinding

class SignInActivity : AppCompatActivity() {

    private lateinit var signInBinding: ActivitySignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        signInBinding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(signInBinding.root)


    }

}