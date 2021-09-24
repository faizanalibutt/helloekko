package com.ekku.nfc.ui.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navOptions
import com.ekku.nfc.R
import com.ekku.nfc.databinding.FragmentFleetBinding
import com.ekku.nfc.model.Item
import com.ekku.nfc.model.Partner
import com.ekku.nfc.ui.viewmodel.AdminViewModel
import com.ekku.nfc.util.NetworkUtils
import com.ekku.nfc.util.Status
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber


class FleetFragment : Fragment() {

    private var fleetBinding: FragmentFleetBinding? = null
    private lateinit var containerSize: String
    private lateinit var containerType: String
    private val adminViewModel: AdminViewModel by viewModels {
        AdminViewModel.AdminViewModelFactory(_context)
    }

    private var _context: Context? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment view is binding here lukakizikato
        fleetBinding = FragmentFleetBinding.inflate(inflater, container, false)
        return fleetBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _context = view.context ?: null
        // for null check this ia a good approach
        fleetBinding?.let { fleetBinding ->

            // add anims for flow
            val options = navOptions {
                anim {
                    enter = R.anim.slide_in_right
                    exit = R.anim.slide_out_left
                    popEnter = R.anim.slide_in_left
                    popExit = R.anim.slide_out_right
                }
            }

            // navigate to scan fragment code
            fleetBinding.btnScan.setOnClickListener {
                val actionScan = FleetFragmentDirections.actionFleetFragmentToScanFragment(
                    containerType, containerSize
                )
                findNavController().navigate(actionScan)
            }

            // navigate to scan fragment code using navigate action.
            // fleetBinding.btnScan.setOnClickListener(Navigation.createNavigateOnClickListener(actionScan))

            // add items to size spinner
            getItemsSize(view, fleetBinding)

            // add click listener to spinner size
            fleetBinding.spinnerSize.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        // here we will get container size.
                        containerSize = parent?.getItemAtPosition(position) as? String ?: "Large"
                        Timber.d("Container Size Sending : $containerSize")
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {

                    }
                }

            // add items to type spinner
            getItemsType(view, fleetBinding)

            // add click listener to spinner type
            fleetBinding.spinnerType.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        // here we will get container type.
                        containerType = parent?.getItemAtPosition(position) as? String ?: "Beverage"
                        Timber.d("Container Type Sending : $containerType")
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {

                    }
                }
        }
    }

    private fun getItemsType(
        view: View,
        fleetBinding: FragmentFleetBinding
    ) {
        if (!NetworkUtils.isOnline(view.context)) {
            Snackbar.make(view, getString(R.string.text_no_wifi), Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(R.string.retry)) {
                    getItemsType(view, fleetBinding)
                }.show()
            return
        }
        adminViewModel.getItemType().observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        resource.data?.let { itemData ->
                            // prepare list for items type spinner
                            val itemsType = mutableListOf<String>()
                            for (item in itemData.items)
                                itemsType.add(item.name)
                            Timber.d("Item Type Api Response: ${itemData.message}")
                            // set spinner adapter from types coming from cloud.
                            ArrayAdapter(
                                view.context,
                                android.R.layout.simple_spinner_item,
                                itemsType
                            ).also { adapter ->
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                fleetBinding.spinnerType.adapter = adapter
                            }
                            fleetBinding.btnScan.isEnabled = true
                        }
                    }
                    Status.ERROR -> {
                        Timber.d("Item Type Api Response ${resource.message}")
                    }
                    Status.LOADING -> {
                        Timber.d("Item Type Response You didn't implement it.")
                    }
                }
            }
        })
    }

    private fun getItemsSize(
        view: View,
        fleetBinding: FragmentFleetBinding
    ) {
        if (!NetworkUtils.isOnline(view.context)) {
            Snackbar.make(view, getString(R.string.text_no_wifi), Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(R.string.retry)) {
                    getItemsSize(view, fleetBinding)
                }.show()
            return
        }
        adminViewModel.getItemSize().observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        resource.data?.let { itemData ->
                            // prepare list for items type spinner
                            val itemsSize = mutableListOf<String>()
                            for (item in itemData.items)
                                itemsSize.add(item.name)
                            Timber.d("Item Size Api Response: ${itemData.message}")
                            // set spinner adapter from types coming from cloud.
                            ArrayAdapter(
                                view.context,
                                android.R.layout.simple_spinner_item,
                                itemsSize
                            ).also { adapter ->
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                fleetBinding.spinnerSize.adapter = adapter
                            }
                            fleetBinding.btnScan.isEnabled = true
                        }
                    }
                    Status.ERROR -> {
                        Timber.d("Item Size Api Response ${resource.message}")
                    }
                    Status.LOADING -> {
                        Timber.d("Item Size Response You didn't implement it.")
                    }
                }
            }
        })
    }

}