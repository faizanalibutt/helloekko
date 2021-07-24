package com.ekku.nfc.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.ekku.nfc.R
import com.ekku.nfc.databinding.FragmentScanBinding
import com.ekku.nfc.ui.activity.AccountActivity.Companion.ADMIN_MODE
import com.ekku.nfc.util.AppUtils
import com.ekku.nfc.util.getDefaultPreferences

class ScanFragment : Fragment() {

    private var scanBinding: FragmentScanBinding? = null
    private val adminMode by lazy {
        activity?.getDefaultPreferences()?.getString(ADMIN_MODE, "Fleet")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        scanBinding = FragmentScanBinding.inflate(inflater, container, false)
        return scanBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // pop it with action button at run time.
        // I'm coming from fleet, lets go back to fleet on action;
        scanBinding?.let { scanBinding ->
            when (adminMode) {
                "Fleet" -> scanBinding.btnSubmit.setOnClickListener(
                    Navigation.createNavigateOnClickListener(R.id.fleet_action, null)
                )
                "Assign" -> scanBinding.btnSubmit.setOnClickListener(
                    Navigation.createNavigateOnClickListener(R.id.assign_action, null)
                )
                "CheckIn" -> scanBinding.btnSubmit.setOnClickListener(
                    Navigation.createNavigateOnClickListener(R.id.check_in_action, null)
                )
                "Empty" -> scanBinding.btnSubmit.setOnClickListener(
                    Navigation.createNavigateOnClickListener(R.id.empty_action, null)
                )
                "Retired" -> scanBinding.btnSubmit.setOnClickListener(
                    Navigation.createNavigateOnClickListener(R.id.retired_action, null)
                )
            }
            scanBinding.clearContainers.setOnClickListener {}
        }
    }

}