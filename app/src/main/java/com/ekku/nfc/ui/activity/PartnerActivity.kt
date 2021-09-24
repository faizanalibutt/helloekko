package com.ekku.nfc.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.camera2.CameraManager
import android.location.Location
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneNumberFormattingTextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.ekku.nfc.AppDelegate
import com.ekku.nfc.R
import com.ekku.nfc.app.UserActivity
import com.ekku.nfc.databinding.ActivityRestaurantBinding
import com.ekku.nfc.model.Consumer
import com.ekku.nfc.ui.activity.AccountActivity.Companion.LOGIN_TOKEN
import com.ekku.nfc.ui.viewmodel.TAGViewModel
import com.ekku.nfc.util.*
import com.ekku.nfc.util.AppUtils.allowWritePermission
import com.ekku.nfc.util.AppUtils.createConfirmationAlert
import com.ekku.nfc.util.AppUtils.isAPI23
import com.ekku.nfc.util.NetworkUtils.getDeviceIMEI
import com.ekku.nfc.util.NfcUtils.addNfcCallback
import com.ekku.nfc.util.NfcUtils.getNfcAdapter
import com.ekku.nfc.util.NfcUtils.isNFCOnline
import com.ekku.nfc.util.NfcUtils.removeNfcCallback
import com.ekku.nfc.util.NfcUtils.showNFCSettings
import com.ekku.nfc.util.NotifyUtils.playNotification
import com.ekku.nfc.util.NotifyUtils.setIntervalWork
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.common.io.BaseEncoding
import timber.log.Timber
import com.ekku.nfc.model.Tag as TagEntity


class PartnerActivity : UserActivity(), NfcAdapter.ReaderCallback,
    CurrentLocation.LocationResultListener {

    private var scanBtnClicked: Boolean = false
    private lateinit var restaurantBinding: ActivityRestaurantBinding
    private var dialog: AlertDialog? = null
    private var currentLocation: CurrentLocation? = null
    private var preventDialogs = false
    private val tagViewMadel: TAGViewModel by viewModels {
        TAGViewModel.TagViewModelFactory((application as AppDelegate).repository, this)
    }
    private lateinit var nfcTagScanList: MutableList<TagEntity>
    private lateinit var consumersList: List<Consumer>

    // token has information about partner
    private val partnerToken by lazy {
        getDefaultPreferences().getString(LOGIN_TOKEN, "put-your-login-token-here")
    }

    /**
     * check no duplication happened tags must be unique.
     * */
    private var isIdAvailable = true
    private lateinit var consumerId: String
    private lateinit var consumerPhone: String

    /**
     * see if we have same number of containers
     */
    var containerNo: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restaurantBinding = ActivityRestaurantBinding.inflate(layoutInflater)
        val view = restaurantBinding.root
        setContentView(view)

        // instantiate location object.
        currentLocation = CurrentLocation(this@PartnerActivity)
        // save IMEI to global guid. if permission is allowed.
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        )
            AppUtils.STRING_GUID = getDeviceIMEI()

        // implement alarm manager at midnight, intervals.
        if (!getDefaultPreferences().getBoolean(AppUtils.TAG_ALARM_KEY, false)) {
            getDefaultPreferences().edit().putBoolean(AppUtils.TAG_ALARM_KEY, true).apply()
            setIntervalWork()
        }

        restaurantBinding.btnScan.setOnClickListener {
            if (restaurantBinding.orderField.text.isEmpty()) {
                Toast.makeText(
                    this@PartnerActivity,
                    "Please enter Ekko ID", Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            } else if (!NetworkUtils.isOnline(this)) {
                Snackbar.make(
                    restaurantBinding.root,
                    "No Internet Connection Available", Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            scanBtnClicked = true
            hideSystemKeyboard(this@PartnerActivity)
            restaurantBinding.orderField.clearFocus()
            restaurantBinding.scansGroup.visibility = View.GONE
            restaurantBinding.containersGroup.visibility = View.VISIBLE
            restaurantBinding.orderField.isEnabled = false
            restaurantBinding.progressBar.visibility = View.VISIBLE
            // initialize containers scanned list
            nfcTagScanList = mutableListOf()
            // start nfc process right away.
            startNFCScanProcess()
        }

        restaurantBinding.clearContainers.setOnClickListener {
            // user forget something so clearing what's been done.
            reset()
        }

        restaurantBinding.btnSubmit.setOnClickListener {
            // user is ready to upload data to server and save in local db.
            when {
                restaurantBinding.containersNumber.text.isEmpty() -> {
                    Toast.makeText(
                        this@PartnerActivity,
                        "Please first scan reusable.", Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                !NetworkUtils.isOnline(this) -> {
                    Snackbar.make(
                        restaurantBinding.root,
                        "No Internet Connection Available", Snackbar.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }
                containerNo != nfcTagScanList.size -> {
                    showDialog(
                        desc = getString(R.string.text_containers_scanned_ordered_check),
                        right = getString(R.string.okay)
                    )
                    return@setOnClickListener
                }
                else -> {
                    submitTagData()
                    reset()
                }
            }
        }

        // phone number dashes added perfectly.
        restaurantBinding.orderField.addTextChangedListener(PhoneNumberFormattingTextWatcher())

        // display partner name at title bar.
        supportActionBar?.let {
            it.title =
                getDataFromToken("partnerName", partnerToken)?.asString() ?: "title not found"
        }

        if (!NetworkUtils.isOnline(this))
            Snackbar.make(
                restaurantBinding.root,
                "No Internet Connection Available", Snackbar.LENGTH_LONG
            ).show()
        // verify consumer upon order from partner side
        showConsumers(view)
    }

    override fun onResume() {
        super.onResume()
        if (scanBtnClicked)
            setUpNfc()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty())
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, access location now.
                currentLocation?.getLocation(this)
            }
        if (grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            AppUtils.STRING_GUID = getDeviceIMEI()
        }
        if (grantResults[2] == PackageManager.PERMISSION_DENIED
        ) {
            // its compulsory for uniqueness of a device. must grant it.
            // show dialog to the user.
            showDialog(
                getString(R.string.dialog_phone_title),
                getString(R.string.dialog_phone_desc),
                right = getString(R.string.okay), dialogType = 103
            )
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (tag != null) {
            Timber.d("Tag Id is: ${BaseEncoding.base16().encode(tag.id)}")
            Handler(Looper.getMainLooper()).post {
                val tagEntity = TagEntity(
                    tag_uid = BaseEncoding.base16().encode(tag.id),
                    tag_time = TimeUtils.getToday(),
                    tag_date = TimeUtils.getFormatDate(TimeUtils.getToday()),
                    tag_date_time = TimeUtils.getFormatDateTime(TimeUtils.getToday()),
                    tag_phone_uid = AppUtils.STRING_GUID,
                    tag_sync = 1,
                    tag_orderId = restaurantBinding.orderField.text.toString()
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
                    restaurantBinding.containersNumber.text =
                        "${nfcTagScanList.size} of $containerNo"
                }
            }
            setUpTorch(false)
            Handler(Looper.getMainLooper()).postDelayed({ setUpTorch(true) }, 700)
            playNotification(
                getString(R.string.notification_desc), AppUtils.NOTIFICATION_ID, "loved_it"
            )
        } else
            playNotification(
                getString(R.string.notification_desc_unses), AppUtils.NOTIFICATION_ID, "loved_it"
            )
    }

    /*
    * just to see where the phone is.
    * */
    override fun gotLocation(location: Location?) {
        location?.let {
            val lat = it.latitude
            val long = it.longitude
            getDefaultPreferences().edit().putString("GPS_DATA_LAT", "$lat").apply()
            getDefaultPreferences().edit().putString("GPS_DATA_LONG", "$long").apply()
            Timber.d("Location Partner: $lat, $long")
        }
    }

    private fun isConsumerExist(): Boolean {
        for (consumer in consumersList)//(134) 137-9172)
            if (takeNumberOnly(restaurantBinding.orderField.text.toString()) == takeNumberOnly(
                    consumer.phoneNo
                )
            ) {
                restaurantBinding.progressBar.visibility = View.GONE
                consumerId = consumer.id
                consumerPhone = consumer.phoneNo
                return true
            }
        return false
    }

    private fun reEnterEkkoId() {
        // give focus to text field, blink cursor and show keyboard
        reset(isValidating = true)
        restaurantBinding.orderField.focusAndShowKeyboard()
    }

    private fun reset(isValidating: Boolean = false) {
        restaurantBinding.containersNumber.text = "0"
        if (!isValidating) restaurantBinding.orderField.setText("")
        restaurantBinding.scansGroup.visibility = View.VISIBLE
        restaurantBinding.containersGroup.visibility = View.GONE
        restaurantBinding.orderField.isEnabled = true
        restaurantBinding.progressBar.visibility = View.GONE
        // remove list created for scans against orderID
        // disable nfc scanning
        nfcTagScanList.clear()
        setUpTorch(false)
        removeNfcCallback(this@PartnerActivity)
        scanBtnClicked = false
        isIdAvailable = true
    }

    private fun setUpNfc() {
        if (isNFCOnline()) {
            addNfcCallback(this@PartnerActivity, this)
            if (!preventDialogs) {
                preventDialogs = true
                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED && isAPI23
                ) {
                    currentLocation?.getLocation(this)
                    askForPermission()
                } else if (Build.VERSION.SDK_INT < 23) {
                    currentLocation?.getLocation(this)
                } else {
                    askForPermission()
                }
            }
        } else
            getNfcAdapter()?.let {
                showDialog(
                    getString(R.string.dialog_nfc_title),
                    getString(R.string.dialog_nfc_desc),
                    right = getString(R.string.txt_go_to_settings), dialogType = 101
                )
            }
    }

    private var camera: Camera? = null
    private fun setUpTorch(torchSwitch: Boolean) {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                try {
                    if (isAPI23)
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
                Toast.makeText(this, "This device has no flash", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "This device has no camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showConsumers(view: ConstraintLayout) {
        if (!NetworkUtils.isOnline(this)) {
                Snackbar.make(view, getString(R.string.text_no_wifi), Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry)) {
                        showConsumers(view)
                    }.show()
                return
            }
        tagViewMadel.getConsumersData().observe(this@PartnerActivity, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        //get users
                        resource.data?.let { customer ->
                            consumersList = customer.consumers
                            Timber.d("customers list: $consumersList")
                        }
                    }
                    Status.ERROR -> {
                        // inform user of no connection
                        Timber.d("we got error while fetching consumers list ${resource.message}")
                    }
                    Status.LOADING -> {
                    }
                }
            }
        })
    }

    @SuppressLint("InflateParams")
    private fun showContainersNumberDialog() {
        val containerDialog = MaterialAlertDialogBuilder(
            this@PartnerActivity,
            if (isAPI23) R.style.ThemeOverlay_MyApp_MaterialAlertDialog else R.style.AlertDialogTheme
        )
        val customDialogView: View = LayoutInflater.from(this@PartnerActivity)
            .inflate(R.layout.layout_container_dialog, null, false)
        val fieldContainer = customDialogView.findViewById<EditText>(R.id.field_container_number)
        containerDialog.setView(customDialogView)
            .setTitle(getString(R.string.text_containers_no))
            .setPositiveButton(getString(R.string.text_set)) { _, _ ->
                containerNo =
                    if (fieldContainer.text.toString()
                            .isEmpty()
                    ) 0 else fieldContainer.text.toString().toInt()
                // enable NFC scanning
                setUpNfc()
                setUpTorch(true)
                "0 of $containerNo".also { restaurantBinding.containersNumber.text = it }
            }
            .setNegativeButton(getString(R.string.text_cancel)) { _, _ ->
                // initialize list for scans against orderID
                reEnterEkkoId()
            }
            .setCancelable(false)
        if (!isFinishing)
            containerDialog.show()
    }

    @SuppressLint("InflateParams")
    private fun showContainersKeypadDialog() {
        var _dialog: AlertDialog? = null
        fun startNfcProcess() {
            _dialog?.dismiss()
            // enable NFC scanning
            setUpNfc()
            setUpTorch(true)
            "0 of $containerNo".also { restaurantBinding.containersNumber.text = it }
        }

        val containerDialog = MaterialAlertDialogBuilder(
            this@PartnerActivity,
            if (isAPI23) R.style.ThemeOverlay_MyApp_MaterialAlertDialog else R.style.AlertDialogTheme
        )
        val customDialogView: View = LayoutInflater.from(this@PartnerActivity)
            .inflate(R.layout.layout_keypad_style, null, false)
        customDialogView.findViewById<Chip>(R.id.id_one)
            .setOnClickListener { containerNo = 1; startNfcProcess() }
        customDialogView.findViewById<Chip>(R.id.id_two)
            .setOnClickListener { containerNo = 2; startNfcProcess() }
        customDialogView.findViewById<Chip>(R.id.id_three)
            .setOnClickListener { containerNo = 3; startNfcProcess() }
        customDialogView.findViewById<Chip>(R.id.id_four)
            .setOnClickListener { containerNo = 4; startNfcProcess() }
        customDialogView.findViewById<Chip>(R.id.id_five)
            .setOnClickListener { containerNo = 5; startNfcProcess() }
        customDialogView.findViewById<Chip>(R.id.id_six)
            .setOnClickListener { containerNo = 6; startNfcProcess() }
        customDialogView.findViewById<Chip>(R.id.id_seven)
            .setOnClickListener { containerNo = 7;startNfcProcess() }
        customDialogView.findViewById<Chip>(R.id.id_eight)
            .setOnClickListener { containerNo = 8;startNfcProcess() }
        customDialogView.findViewById<Chip>(R.id.id_nine)
            .setOnClickListener { containerNo = 9; startNfcProcess() }
        containerDialog.setView(customDialogView)
            .setTitle(getString(R.string.text_containers_no))
            .setPositiveButton(getString(R.string.text_more)) { _, _ ->
                showContainersNumberDialog()
            }
            .setNegativeButton(getString(R.string.text_cancel)) { _, _ ->
                // initialize list for scans against orderID
                reEnterEkkoId()
            }
            .setCancelable(false)
        if (!isFinishing)
            _dialog = containerDialog.show()
    }

    private fun startNFCScanProcess() {
        val isReady = isConsumerExist()
        if (isReady) {
            // set up containers number and halt this function.
            showContainersKeypadDialog()
        } else {
            restaurantBinding.progressBar.visibility = View.GONE
            showDialog(
                dialogType = 105,
                desc = getString(R.string.text_user_not_found),
                right = getString(R.string.text_confirm),
                left = getString(R.string.text_edit),
                cancelable = false
            )
        }
    }

    private fun submitTagData() {
        // add containers id to final list
        val containersIds: MutableList<String> = mutableListOf()
        for (container in nfcTagScanList)
            containersIds.add(container.tag_uid)

        val isConsumerAvailable = isConsumerExist()
        // api is calling
        tagViewMadel.postCustomerOrder(
            consumerId = if (isConsumerAvailable) consumerId else restaurantBinding.orderField.text.toString(),
            containersIds,
            ekkoId = if (isConsumerAvailable) consumerPhone else restaurantBinding.orderField.text.toString()
        ).observe(this, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        resource.data?.let { response ->
                            Timber.d("tag data uploaded successfully ${response.message}")
                            restaurantBinding.progressBar.visibility = View.GONE
                            showDialog(
                                title = getString(R.string.text_order_status),
                                desc = response.message,
                                right = getString(R.string.okay),
                                dialogType = 104
                            )
                        }
                    }
                    Status.ERROR -> {
                        Timber.d("tag data not uploaded. ${resource.message}")
                        restaurantBinding.progressBar.visibility = View.GONE
                        showDialog(
                            title = getString(R.string.text_order_status),
                            desc = resource.message
                                ?: getString(R.string.text_order_detail),
                            right = getString(R.string.okay),
                            dialogType = 104
                        )
                    }
                    Status.LOADING -> {
                        restaurantBinding.progressBar.visibility = View.VISIBLE
                    }
                }
            }
        })
    }

    private fun showDialog(
        title: String = "",
        desc: String = "",
        right: String = "",
        left: String = "",
        dialogType: Int = -1,
        cancelable: Boolean = true
    ) {
        val isShowing = dialog?.isShowing ?: false
        if (isShowing)
            return
        dialog = createConfirmationAlert(
            title, desc,
            right = right,
            left = left,
            listener = object : AlertButtonListener {
                override fun onClick(dialog: DialogInterface, type: ButtonType) {
                    when {
                        type == ButtonType.RIGHT && dialogType == 101 -> showNFCSettings()
                        type == ButtonType.RIGHT && dialogType == 102 -> allowWritePermission()
                        type == ButtonType.RIGHT && dialogType == 103 -> askForPermission()
                        type == ButtonType.RIGHT && dialogType == 104 -> dialog.dismiss()
                        type == ButtonType.LEFT && dialogType == 105 -> reEnterEkkoId()
                        type == ButtonType.RIGHT && dialogType == 105 -> {
                            dialog.dismiss()
                            showDialog(
                                desc = getString(R.string.text_customer_not_present_desc),
                                right = getString(R.string.okay)
                            )
                            // send partner id and new customer no to fire store
                            uploadNewCustomer()
                            // customer was not registered so reset it.
                            reset()
                        }
                    }
                }
            },
            cancelable = cancelable
        )
        if (!isFinishing)
            dialog?.show()
    }

    private fun takeNumberOnly(phoneNo: String) =
        phoneNo.replace(Regex("[()\\-\\s]"), "")

    private fun uploadNewCustomer() {
        tagViewMadel.postNewCustomer(restaurantBinding.orderField.text.toString()).observe(
            this, {
                it?.let { resource ->
                    when (resource.status) {
                        Status.SUCCESS -> {
                            Timber.d("New Customer Successfully Captured.")
                        }
                        Status.ERROR -> {
                            Timber.d("new customer not uploaded. ${resource.message}")
                        }
                        Status.LOADING -> {}
                    }
                }
            }
        )
    }
}