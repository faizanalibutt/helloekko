package com.ekku.nfc.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.ekku.nfc.R
import com.ekku.nfc.databinding.FragmentCheckInBinding
import com.ekku.nfc.databinding.FragmentRetiredBinding

class CheckInFragment : Fragment() {

    private var checkInBinding: FragmentCheckInBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        checkInBinding = FragmentCheckInBinding.inflate(inflater, container, false)
        return checkInBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // navigate to scan fragment code using navigate action.
        checkInBinding?.btnScan?.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_checkInFragment_to_scanFragment)
        )
    }

}