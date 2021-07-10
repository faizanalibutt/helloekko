package com.ekku.nfc.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ekku.nfc.R
import com.ekku.nfc.databinding.ActivityAdminBinding

class AdminActivity : AppCompatActivity() {

    private lateinit var adminBinding: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adminBinding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(adminBinding.root)

    }
}