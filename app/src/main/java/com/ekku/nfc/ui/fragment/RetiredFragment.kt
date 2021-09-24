package com.ekku.nfc.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.ekku.nfc.R
import com.ekku.nfc.databinding.FragmentRetiredBinding
import com.ekku.nfc.util.NetworkUtils
import com.google.android.material.snackbar.Snackbar

class RetiredFragment : Fragment() {

    private var retiredBinding: FragmentRetiredBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        retiredBinding = FragmentRetiredBinding.inflate(inflater, container, false)
        return retiredBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // navigate to scan fragment code using navigate action.
        retiredBinding?.btnScan?.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_retiredFragment_to_scanFragment)
        )
        if (!NetworkUtils.isOnline(view.context))
            Snackbar.make(view, getString(R.string.text_no_wifi), Snackbar.LENGTH_LONG).show()

    }

}