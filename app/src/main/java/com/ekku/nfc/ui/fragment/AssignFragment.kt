package com.ekku.nfc.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.ekku.nfc.R
import com.ekku.nfc.databinding.FragmentAssignBinding

class AssignFragment : Fragment() {

    private lateinit var assignBinding: FragmentAssignBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        assignBinding = FragmentAssignBinding.inflate(inflater, container,false)
        return assignBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        assignBinding?.let { assignBinding ->
            assignBinding.btnScan.setOnClickListener(
                Navigation.createNavigateOnClickListener(R.id.action_assignFragment_to_scanFragment)
            )
        }
    }

}