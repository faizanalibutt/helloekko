package com.ekku.nfc.ui.fragment

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.ekku.nfc.R
import com.ekku.nfc.databinding.FragmentAssignBinding
import com.ekku.nfc.model.Partner
import com.ekku.nfc.ui.activity.AccountActivity
import com.ekku.nfc.ui.viewmodel.AdminViewModel
import com.ekku.nfc.util.AlertButtonListener
import com.ekku.nfc.util.AppUtils.createConfirmationAlert
import com.ekku.nfc.util.ButtonType
import com.ekku.nfc.util.NfcUtils.showNFCSettings
import com.ekku.nfc.util.Status
import com.ekku.nfc.util.getDefaultPreferences
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

class AssignFragment : Fragment() {

    private var assignBinding: FragmentAssignBinding? = null
    private val adminViewModel: AdminViewModel by viewModels {
        AdminViewModel.AdminViewModelFactory(_context)
    }
    private val adminToken by lazy {
        _context?.getDefaultPreferences()
            ?.getString(AccountActivity.LOGIN_TOKEN, "put-your-login-token-here")
    }

    // dialog to tell if something is wrong
    private var dialog: AlertDialog? = null
    private var _context: Context? = null

    // partner name argument to be passed to scan fragment
    private lateinit var partnerName: String
    private lateinit var partners: List<Partner>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        assignBinding = FragmentAssignBinding.inflate(inflater, container, false)
        return assignBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        assignBinding?.let { assignBinding ->

            _context = view.context ?: null

            // call api first to get list then set partner spinner
            adminViewModel.fetchPartners().observe(viewLifecycleOwner, {
                it?.let { resource ->
                    when (resource.status) {
                        Status.SUCCESS -> {
                            resource.data?.let { partnerData ->
                                // prepare list for partners spinner
                                val partnersName = mutableListOf<String>()
                                for (partner in partnerData.partners)
                                    partnersName.add(partner.partnerName)
                                Timber.d("Assign Partner Api Response: ${partnerData.message}")

                                // set spinner adapter from partners coming from cloud.
                                ArrayAdapter(
                                    view.context,
                                    android.R.layout.simple_spinner_item,
                                    partnersName
                                ).also { adapter ->
                                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                    assignBinding.spinnerPartner.adapter = adapter
                                }

                                // get data to global partners list for id.
                                partners = partnerData.partners
                                assignBinding.btnScan.isEnabled = true
                            }
                        }
                        Status.ERROR -> {
                            Timber.d("Assign Partner Api Response ${resource.message}")
                            showDialog(
                                title = getString(R.string.text_admin_response),
                                desc = resource.message
                                    ?: getString(R.string.text_order_detail),
                                right = getString(R.string.okay),
                                dialogType = 104,
                                context = _context
                            )
                        }
                        Status.LOADING -> {
                            Timber.d("Fleet Api Response You didn't implement it.")
                        }
                    }
                }
            })

            // add click listener to spinner size
            assignBinding.spinnerPartner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        // here we will get partner name.
                        partnerName =
                            parent?.getItemAtPosition(position) as? String ?: "ANY_PARTNER"
                        Timber.d("Partner Name Selected : $partnerName")
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {

                    }
                }

            // navigate to scan fragment code
            assignBinding.btnScan.setOnClickListener {
                var partnerId = "any_id"
                for (partner in partners)
                    if (partner.partnerName == partnerName)
                        partnerId = partner.id

                // going to scan fragment with partner id.
                val actionScan = AssignFragmentDirections.actionAssignFragmentToScanFragment(
                    partnerId
                )
                findNavController().navigate(actionScan)
            }
        }
    }

    private fun showDialog(
        title: String,
        desc: String,
        right: String = "",
        left: String = "",
        dialogType: Int,
        context: Context? = null
    ) {
        val isShowing = dialog?.isShowing ?: false
        if (isShowing)
            return
        dialog = context?.createConfirmationAlert(
            title, desc,
            right = right,
            left = left,
            cancelable = false,
            listener = object : AlertButtonListener {
                override fun onClick(dialog: DialogInterface, type: ButtonType) {
                    when {
                        type == ButtonType.RIGHT && dialogType == 101 -> context.showNFCSettings()
                        type == ButtonType.RIGHT && dialogType == 102 -> {
                        }//allowWritePermission()
                        type == ButtonType.RIGHT && dialogType == 103 -> {
                        }//askForPermission()
                        type == ButtonType.RIGHT && dialogType == 104 -> dialog.dismiss()
                    }
                }
            })
        if ((context as? AppCompatActivity)?.isFinishing == false)
            dialog?.show()
    }

}