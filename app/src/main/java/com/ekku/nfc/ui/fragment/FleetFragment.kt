package com.ekku.nfc.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.ekku.nfc.R
import com.ekku.nfc.databinding.FragmentFleetBinding


class FleetFragment : Fragment() {

    private var fleetBinding: FragmentFleetBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fleetBinding = FragmentFleetBinding.inflate(inflater, container, false)
        return fleetBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        fleetBinding?.btnScan?.setOnClickListener {
            //findNavController().navigate(R.id.scanFragment, null, options)
        }
        // navigate to scan fragment code using navigate action.
        fleetBinding?.btnScan?.setOnClickListener(
            Navigation.createNavigateOnClickListener(
                R.id.action_fleetFragment_to_scanFragment, null
            )
        )
    }

}