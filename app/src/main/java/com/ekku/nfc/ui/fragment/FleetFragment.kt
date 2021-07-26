package com.ekku.nfc.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navOptions
import com.ekku.nfc.R
import com.ekku.nfc.databinding.FragmentFleetBinding
import timber.log.Timber


class FleetFragment : Fragment() {

    private var fleetBinding: FragmentFleetBinding? = null
    private lateinit var containerSize: String
    private lateinit var containerType: String

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
                    containerSize, containerType
                )
                findNavController().navigate(actionScan)
            }

            // navigate to scan fragment code using navigate action.
            // fleetBinding.btnScan.setOnClickListener(Navigation.createNavigateOnClickListener(actionScan))

            // add items to size spinner
            ArrayAdapter.createFromResource(
                view.context,
                R.array.container_size_array,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                fleetBinding.spinnerSize.adapter = adapter
            }

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
            ArrayAdapter.createFromResource(
                view.context,
                R.array.container_type_array,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                fleetBinding.spinnerType.adapter = adapter
            }

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
                        containerType = parent?.getItemAtPosition(position) as? String ?: "Large"
                        Timber.d("Container Size Sending : $containerType")
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {

                    }
                }
        }
    }

}