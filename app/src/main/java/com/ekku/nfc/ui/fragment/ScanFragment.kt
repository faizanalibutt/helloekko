package com.ekku.nfc.ui.fragment

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.camera2.CameraManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ekku.nfc.R
import com.ekku.nfc.databinding.FragmentScanBinding
import com.ekku.nfc.model.Consumer
import com.ekku.nfc.model.Container
import com.ekku.nfc.ui.activity.AccountActivity
import com.ekku.nfc.ui.activity.AccountActivity.Companion.ADMIN_MODE
import com.ekku.nfc.ui.activity.AccountActivity.Companion.LOGIN_TOKEN
import com.ekku.nfc.ui.activity.AdminActivity
import com.ekku.nfc.ui.viewmodel.AdminViewModel
import com.ekku.nfc.util.*
import com.ekku.nfc.util.AppUtils.allowWritePermission
import com.ekku.nfc.util.AppUtils.canWrite
import com.ekku.nfc.util.AppUtils.createConfirmationAlert
import com.ekku.nfc.util.NfcUtils.addNfcCallback
import com.ekku.nfc.util.NfcUtils.getNfcAdapter
import com.ekku.nfc.util.NfcUtils.isNFCOnline
import com.ekku.nfc.util.NfcUtils.showNFCSettings
import com.ekku.nfc.util.NotifyUtils.playNotification
import com.google.android.material.snackbar.Snackbar
import com.google.common.io.BaseEncoding
import timber.log.Timber
import com.ekku.nfc.model.Tag as TagEntity

class ScanFragment : Fragment(), NfcAdapter.ReaderCallback {

    private var scanBinding: FragmentScanBinding? = null
    private val adminMode by lazy {
        activity?.getDefaultPreferences()?.getString(ADMIN_MODE, "Fleet")
    }
    private val scanFragmentArgs: ScanFragmentArgs by navArgs()
    private val adminViewModel: AdminViewModel by viewModels {
        AdminViewModel.AdminViewModelFactory(_context)
    }
    private val adminToken by lazy {
        _context?.getDefaultPreferences()?.getString(LOGIN_TOKEN, "put-your-login-token-here")
    }

    // take camera variable for flash
    private var camera: Camera? = null

    // dialog to tell if something is wrong
    private var dialog: AlertDialog? = null
    private var _context: Context? = null

    // tag list to send
    private lateinit var nfcTagScanList: MutableList<TagEntity>

    /**
     * check no duplication happened tags must be unique.
     * */
    private var isIdAvailable = true

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

            _context = view.context ?: null
            scanBinding.btnSubmit.setOnClickListener {
                // call multiple api based on admin mode.
                postAdminData(adminMode)
            }
            scanBinding.clearContainers.setOnClickListener { navigateToAdminMode(adminMode) }

            nfcTagScanList = mutableListOf()
        }

    }

    override fun onResume() {
        super.onResume()
        _context?.let {
            setUpTorch(true, it)
            setUpNfc(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _context?.let { setUpTorch(false, it) }
    }

    override fun onTagDiscovered(tagInfo: Tag?) {
        tagInfo?.let { tag ->
            Timber.d("Scan Fragment Tag Id is: ${BaseEncoding.base16().encode(tag.id)}")
            Handler(Looper.getMainLooper()).post {
                val tagEntity = TagEntity(
                    tag_uid = BaseEncoding.base16().encode(tag.id),
                    tag_time = TimeUtils.getToday(),
                    tag_date = TimeUtils.getFormatDate(TimeUtils.getToday()),
                    tag_date_time = TimeUtils.getFormatDateTime(TimeUtils.getToday()),
                    tag_phone_uid = AppUtils.STRING_GUID,
                )
                if (nfcTagScanList.isNotEmpty()) {
                    for (checkId in nfcTagScanList) {
                        if (checkId.tag_uid == tagEntity.tag_uid) {
                            isIdAvailable = false
                            break
                        } else
                            isIdAvailable = true
                    }
                }
                if (isIdAvailable) {
                    nfcTagScanList.add(tagEntity)
                    scanBinding?.containersNumber?.text = nfcTagScanList.size.toString()
                }
            }
            _context?.let { setUpTorch(false, it) }
            Handler(Looper.getMainLooper()).postDelayed(
                { _context?.let { setUpTorch(true, it) } }, 700
            )
            _context?.playNotification(
                getString(R.string.notification_desc),
                AppUtils.NOTIFICATION_ID,
                channelName = "loved_it"
            )
        } ?: run {
            _context?.playNotification(
                getString(R.string.notification_desc_unses),
                AppUtils.NOTIFICATION_ID,
                channelName = "loved_it"
            )
        }
    }

    private fun navigateToAdminMode(adminMode: String?) {
        when (adminMode) {
            getString(R.string.text_fleet) -> findNavController().navigate(R.id.fleet_action)
            getString(R.string.text_assign) -> findNavController().navigate(R.id.assign_action)
            getString(R.string.text_check_in) -> findNavController().navigate(R.id.check_in_action)
            getString(R.string.text_empty) -> findNavController().navigate(R.id.empty_action)
            getString(R.string.text_retired) -> findNavController().navigate(R.id.retired_action)
        }
    }

    private fun postAdminData(adminMode: String?) {

        when (adminMode) {
            getString(R.string.text_fleet) -> {
                fleetApi()
            }
            getString(R.string.text_assign) -> {
                assignApi()
            }
            getString(R.string.text_check_in) -> {

            }
            getString(R.string.text_retired) -> {

            }
        }
    }

    private fun assignApi() {
        // prepare object to send
        val containersAssign = mutableListOf<String>()
        for (container in nfcTagScanList)
            containersAssign.add(container.tag_uid)

        // assign container to partner and saving record to cloud
        adminViewModel.postAssignedContainers(scanFragmentArgs.argPartnerName, containersAssign).observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        resource.data?.let { response ->
                            Timber.d("Fleet Api Response: ${response.message}")
                            showDialog(
                                title = getString(R.string.text_admin_response),
                                desc = response.message,
                                right = getString(R.string.okay),
                                dialogType = 104,
                                context = _context
                            )
                        }
                    }
                    Status.ERROR -> {
                        Timber.d("Fleet Api Response ${resource.message}")
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

    private fun fleetApi() {
        // prepare object to send
        val containersFleet = mutableListOf<Container>()
        for (container in nfcTagScanList)
            containersFleet.add(
                Container(
                    container.tag_uid,
                    containerSource = context?.getDataFromToken("region", adminToken)
                        ?.asString() ?: "region_not_available",
                    scanFragmentArgs.argContainerType,
                    scanFragmentArgs.argContainerSize,
                    region = context?.getDataFromToken("region", adminToken)
                        ?.asString() ?: "region_not_available"
                )
            )


        // add api for fleet to add container in it
        adminViewModel.postContainersToFleet(containersFleet).observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        resource.data?.let { response ->
                            Timber.d("Fleet Api Response: ${response.message}")
                            showDialog(
                                title = getString(R.string.text_admin_response),
                                desc = response.message,
                                right = getString(R.string.okay),
                                dialogType = 104,
                                context = _context
                            )
                        }
                    }
                    Status.ERROR -> {
                        Timber.d("Fleet Api Response ${resource.message}")
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

    private fun setUpNfc(context: Context) {
        if (context.isNFCOnline()) {
            context.addNfcCallback(activity as AppCompatActivity, this)
        } else
            context.getNfcAdapter()?.let {
                showDialog(
                    getString(R.string.dialog_nfc_title),
                    getString(R.string.dialog_nfc_desc),
                    right = getString(R.string.txt_go_to_settings),
                    dialogType = 101,
                    context = context
                )
            }
    }

    private fun setUpTorch(torchSwitch: Boolean, context: Context) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                try {
                    if (AppUtils.isAPI23)
                        cameraManager.setTorchMode("0", torchSwitch)
                    else {
                        if (camera == null) {
                            camera = Camera.open()
                        }
                        if (torchSwitch) {
                            val parameters = camera?.parameters
                            parameters?.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                            camera?.parameters = parameters
                            camera?.startPreview()
                        } else {
                            val parameters = camera?.parameters
                            parameters?.flashMode = Camera.Parameters.FLASH_MODE_OFF
                            camera?.parameters = parameters
                            camera?.stopPreview()
                            camera?.release()
                            camera = null
                        }
                    }
                } catch (ignored: Exception) {
                    Timber.d("flash got error: ${ignored.message}")
                }
            } else {
                Toast.makeText(context, "This device has no flash", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "This device has no camera", Toast.LENGTH_SHORT).show()
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
                        type == ButtonType.RIGHT && dialogType == 104 -> {
                            // navigate to admin mode from here (Scanning Session)
                            navigateToAdminMode(adminMode)
                            dialog.dismiss()
                        }
                    }
                }
            })
        if ((context as? AppCompatActivity)?.isFinishing == false)
            dialog?.show()
    }
}