package com.ekku.nfc.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.ekku.nfc.R
import com.ekku.nfc.databinding.FragmentScanBinding
import com.ekku.nfc.ui.activity.AccountActivity.Companion.ADMIN_MODE
import com.ekku.nfc.util.AppUtils
import com.ekku.nfc.util.getDefaultPreferences
import com.google.android.material.snackbar.Snackbar

class ScanFragment : Fragment() {

    private var scanBinding: FragmentScanBinding? = null
    private val adminMode by lazy {
        activity?.getDefaultPreferences()?.getString(ADMIN_MODE, "Fleet")
    }
    private val scanFragmentArgs: ScanFragmentArgs by navArgs()

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
        // I'm coming from admin mode, lets go back to specific admin mode on action;
        scanBinding?.let { scanBinding ->

            when (adminMode) {
                getString(R.string.text_fleet) -> scanBinding.btnSubmit.setOnClickListener(
                    Navigation.createNavigateOnClickListener(R.id.fleet_action)
                )
                getString(R.string.text_assign) -> scanBinding.btnSubmit.setOnClickListener(
                    Navigation.createNavigateOnClickListener(R.id.assign_action)
                )
                getString(R.string.text_check_in) -> scanBinding.btnSubmit.setOnClickListener(
                    Navigation.createNavigateOnClickListener(R.id.check_in_action)
                )
                getString(R.string.text_empty) -> scanBinding.btnSubmit.setOnClickListener(
                    Navigation.createNavigateOnClickListener(R.id.empty_action)
                )
                getString(R.string.text_retired) -> scanBinding.btnSubmit.setOnClickListener(
                    Navigation.createNavigateOnClickListener(R.id.retired_action)
                )
            }

            Snackbar.make(
                view,
                "Coming from Fleet Fragment with this information ${scanFragmentArgs.argContainerSize} ${scanFragmentArgs.argContainerType}",
                Snackbar.LENGTH_LONG
            ).show()

        }
    }

}