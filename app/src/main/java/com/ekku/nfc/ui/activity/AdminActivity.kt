package com.ekku.nfc.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.ekku.nfc.R
import com.ekku.nfc.app.BaseActivity
import com.ekku.nfc.app.UserActivity
import com.ekku.nfc.databinding.ActivityAdminBinding
import com.ekku.nfc.ui.activity.AccountActivity.Companion.ADMIN_MODE
import com.ekku.nfc.ui.viewmodel.AdminViewModel
import com.ekku.nfc.util.AppUtils.startActivity
import com.ekku.nfc.util.getDefaultPreferences
import com.ekku.nfc.util.savePrefs

class AdminActivity : UserActivity() {

    private lateinit var adminBinding: ActivityAdminBinding
    private val adminViewModel: AdminViewModel by viewModels()

    private val adminMode by lazy {
        getDefaultPreferences().getString(ADMIN_MODE, "Fleet")
    }
    private lateinit var appBarConfiguration : AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adminBinding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(adminBinding.root)
        setNavigationGraph()
    }

    private fun setNavigationGraph() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.admin_nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.admin_navigation)
        navGraph.startDestination = when (adminMode) {
            "Assign" -> R.id.assignFragment
            "CheckIn" -> R.id.checkInFragment
            "Empty" -> R.id.emptyFragment
            "Retired" -> R.id.retiredFragment
            else -> R.id.fleetFragment
        }
        navController.graph = navGraph
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBar(navController, appBarConfiguration)
    }

    private fun setupActionBar(navController: NavController,
                               appBarConfig : AppBarConfiguration) {
        setupActionBarWithNavController(navController, appBarConfig)
    }

}