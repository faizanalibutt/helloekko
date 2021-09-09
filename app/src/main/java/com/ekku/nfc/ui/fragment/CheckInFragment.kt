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
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.ekku.nfc.R
import com.ekku.nfc.databinding.FragmentCheckInBinding
import com.ekku.nfc.databinding.FragmentRetiredBinding
import com.ekku.nfc.model.DropBox
import com.ekku.nfc.ui.activity.AccountActivity
import com.ekku.nfc.ui.viewmodel.AdminViewModel
import com.ekku.nfc.util.Status
import com.ekku.nfc.util.getDefaultPreferences
import timber.log.Timber

class CheckInFragment : Fragment() {

    private var checkInBinding: FragmentCheckInBinding? = null
    private val adminViewModel: AdminViewModel by viewModels {
        AdminViewModel.AdminViewModelFactory(_context)
    }
    private val adminToken by lazy {
        _context?.getDefaultPreferences()
            ?.getString(AccountActivity.LOGIN_TOKEN, "put-your-login-token-here")
    }

    /**
     * context is needed for various tasks.
     */
    private var _context: Context? = null
    // partner name argument to be passed to post data to make drop box empty data list.
    private lateinit var boxName: String
    private lateinit var boxes: List<DropBox>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        checkInBinding = FragmentCheckInBinding.inflate(inflater, container, false)
        return checkInBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _context = view.context ?: null

        // navigate to scan fragment code using navigate action.
        checkInBinding?.btnScan?.setOnClickListener {
            // going to scan fragment with dropbox from check_in.
            var dropBoxId = "any_id"
            for (box in boxes)
                if (box.dropboxName == boxName)
                    dropBoxId = box.id

            // going to make drop box empty.
            val actionScan = CheckInFragmentDirections.actionCheckInFragmentToScanFragment(
                argCheckInDropbox = dropBoxId, argCheckInDropboxName = boxName
            )
            findNavController().navigate(actionScan)
        }

        // call api first to get list then set box spinner
        adminViewModel.collectDropBoxes().observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        resource.data?.let { dropBoxData ->
                            Timber.d("Drop Box Api Response: ${dropBoxData.message}")

                            // prepare list for boxes spinner
                            val dropBoxNames = mutableListOf<String>()
                            for (box in dropBoxData.dropBoxes)
                                dropBoxNames.add(box.dropboxName)

                            // set spinner adapter from boxes coming from cloud.
                            ArrayAdapter(
                                view.context,
                                android.R.layout.simple_spinner_item,
                                dropBoxNames
                            ).also { adapter ->
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                checkInBinding?.spinnerDropbox?.adapter = adapter
                            }

                            // get data to global partners list for id.
                            boxes = dropBoxData.dropBoxes
                            checkInBinding?.btnScan?.isEnabled = true
                        }
                    }
                    Status.ERROR -> {
                        Timber.d("Drop Box Api Response ${resource.message}")
                        checkInBinding?.btnScan?.isEnabled = true
                    }
                    Status.LOADING -> {
                        Timber.d("Check In Response You didn't implement it.")
                    }
                }
            }
        })

        // add click listener to spinner size
        checkInBinding?.spinnerDropbox?.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    // here we will get drop box name.
                    boxName =
                        parent?.getItemAtPosition(position) as? String ?: "ANY_DROPBOX"
                    Timber.d("Box Name Selected : $boxName")
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }
    }

}