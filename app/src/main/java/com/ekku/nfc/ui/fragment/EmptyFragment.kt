package com.ekku.nfc.ui.fragment

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.ekku.nfc.R
import com.ekku.nfc.databinding.FragmentEmptyBinding
import com.ekku.nfc.model.DropBox
import com.ekku.nfc.ui.activity.AccountActivity
import com.ekku.nfc.ui.viewmodel.AdminViewModel
import com.ekku.nfc.util.*
import com.ekku.nfc.util.AppUtils.createConfirmationAlert
import com.ekku.nfc.util.NfcUtils.showNFCSettings
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

class EmptyFragment : Fragment(), CurrentLocation.LocationResultListener {

    private var boxBinding: FragmentEmptyBinding? = null
    private val adminViewModel: AdminViewModel by viewModels {
        AdminViewModel.AdminViewModelFactory(_context)
    }
    private val adminToken by lazy {
        _context?.getDefaultPreferences()
            ?.getString(AccountActivity.LOGIN_TOKEN, "put-your-login-token-here")
    }

    // dialog to tell if something is wrong
    private var dialog: AlertDialog? = null
    private var preventDialogs = false

    // location object
    private var mCurrentLocation: Location? = null
    private var currentLocation: CurrentLocation? = null

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
        boxBinding = FragmentEmptyBinding.inflate(inflater, container, false)
        return boxBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        boxBinding?.let { boxBinding ->

            _context = view.context ?: null

            // call api first to get list then set box spinner
            getDropBoxes(view, boxBinding)

            // add click listener to spinner size
            boxBinding.spinnerDropbox.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        // here we will get drop box name.
                        boxName =
                            parent?.getItemAtPosition(position) as? String ?: "ANY_PARTNER"
                        Timber.d("Box Name Selected : $boxName")
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {

                    }
                }

            // navigate to scan fragment code
            boxBinding.btnClean.setOnClickListener {
                var dropBoxId = "any_id"
                for (box in boxes)
                    if (box.dropboxName == boxName)
                        dropBoxId = box.id

                if (!NetworkUtils.isOnline(view.context)) {
                    Snackbar.make(view, getString(R.string.text_no_wifi), Snackbar.LENGTH_LONG)
                        .show()
                    return@setOnClickListener
                }
                // going to make drop box empty.
                dropBoxApi(dropBoxId)
            }

            currentLocation = CurrentLocation(view.context)
            if (!preventDialogs) {
                preventDialogs = true
                if (ContextCompat.checkSelfPermission(
                        view.context, Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED && AppUtils.isAPI23
                ) {
                    currentLocation?.getLocation(this)
                } else if (Build.VERSION.SDK_INT < 23) {
                    currentLocation?.getLocation(this)
                } else {
                    ActivityCompat.requestPermissions(
                        view.context as AppCompatActivity, arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ), 1002
                    )
                }
            }
        }


    }

    private fun getDropBoxes(
        view: View,
        boxBinding: FragmentEmptyBinding
    ) {
        if (!NetworkUtils.isOnline(view.context)) {
            Snackbar.make(view, getString(R.string.text_no_wifi), Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(R.string.retry)) {
                    getDropBoxes(view, boxBinding)
                }.show()
            return
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
                                boxBinding.spinnerDropbox.adapter = adapter
                            }

                            // get data to global partners list for id.
                            boxes = dropBoxData.dropBoxes
                            boxBinding.btnClean.isEnabled = true
                        }
                    }
                    Status.ERROR -> {
                        Timber.d("Drop Box Api Response ${resource.message}")
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
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty())
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, access location now.
                currentLocation?.getLocation(this)
            }
    }

    override fun gotLocation(location: Location?) {
        location?.let {
            mCurrentLocation = it
            val lat = mCurrentLocation?.latitude
            val long = mCurrentLocation?.longitude
            _context?.getDefaultPreferences()?.edit()
                ?.putFloat("GPS_DATA_LAT_F", lat?.toFloat() ?: 0.0f)?.apply()
            _context?.getDefaultPreferences()?.edit()
                ?.putFloat("GPS_DATA_LONG_F", long?.toFloat() ?: 0.0f)?.apply()
        }
    }

    private fun dropBoxApi(dropBoxId: String) {
        // assign container to partner and saving record to cloud
        adminViewModel.emptyDropBox(
            dropBoxId,
            latitude = _context?.getDefaultPreferences()?.getFloat("GPS_DATA_LAT_F", 0.0f) ?: 0.0f,
            longitude = _context?.getDefaultPreferences()?.getFloat("GPS_DATA_LONG_F", 0.0f) ?: 0.0f
        ).observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        resource.data?.let { response ->
                            Timber.d("DropBox Api Response: ${response.message}")
                            showDialog(
                                title = getString(R.string.text_admin_response),
                                desc = resource.message
                                    ?: "Dropbox Empty Successful",
                                right = getString(R.string.okay),
                                dialogType = 104,
                                context = _context
                            )
                        }
                    }
                    Status.ERROR -> {
                        Timber.d("DropBox Api Response ${resource.message}")
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