package com.ekku.nfc.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.ekku.nfc.R
import com.ekku.nfc.app.BaseActivity
import com.ekku.nfc.app.UserActivity
import com.ekku.nfc.databinding.ActivityAdminBinding
import com.ekku.nfc.ui.activity.AccountActivity.Companion.ADMIN_MODE
import com.ekku.nfc.ui.activity.AccountActivity.Companion.LOGIN_TOKEN
import com.ekku.nfc.ui.viewmodel.AdminViewModel
import com.ekku.nfc.util.AppUtils.startActivity
import com.ekku.nfc.util.getDefaultPreferences
import com.ekku.nfc.util.savePrefs

class AdminActivity : AppCompatActivity() {

    private lateinit var adminBinding: ActivityAdminBinding
    private val adminViewModel: AdminViewModel by viewModels() {
        AdminViewModel.AdminViewModelFactory()
    }

    private val adminMode by lazy {
        getDefaultPreferences().getString(ADMIN_MODE, getString(R.string.text_fleet))
    }
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val adminToken by lazy {
        getDefaultPreferences().getString(LOGIN_TOKEN, "put-your-login-token-here")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adminBinding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(adminBinding.root)
        setNavigationGraph()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)
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
            // move to this fragment
            R.id.fleet_action -> { setNavigationGraph(R.id.fleetFragment); true }
            R.id.assign_action -> { setNavigationGraph(R.id.assignFragment); true }
            R.id.empty_action -> { setNavigationGraph(R.id.emptyFragment); true }
            R.id.check_in_action -> { setNavigationGraph(R.id.checkInFragment); true }
            R.id.retired_action -> { setNavigationGraph(R.id.retiredFragment);true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.admin_nav_host_fragment).navigateUp(appBarConfiguration)
    }

    private fun setNavigationGraph() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.admin_nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.admin_navigation)
        navGraph.startDestination = when (adminMode) {
            getString(R.string.text_assign) -> R.id.assignFragment
            getString(R.string.text_check_in) -> R.id.checkInFragment
            getString(R.string.text_empty) -> R.id.emptyFragment
            getString(R.string.text_retired) -> R.id.retiredFragment
            else -> R.id.fleetFragment
        }
        navController.graph = navGraph
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBar(navController, appBarConfiguration)
    }

    private fun setNavigationGraph(destinationFragmentId: Int) {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.admin_nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.admin_navigation)
        navGraph.startDestination = destinationFragmentId
        navController.graph = navGraph
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBar(navController, appBarConfiguration)
    }

    private fun setupActionBar(navController: NavController, appBarConfig: AppBarConfiguration) {
        setupActionBarWithNavController(navController, appBarConfig)
    }

}