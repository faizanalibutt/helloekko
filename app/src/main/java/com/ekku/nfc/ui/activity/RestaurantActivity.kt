package com.ekku.nfc.ui.activity

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ekku.nfc.AppDelegate
import com.ekku.nfc.R
import com.ekku.nfc.databinding.ActivityRestaurantBinding
import com.ekku.nfc.model.TagAPI
import com.ekku.nfc.model.TagDao
import com.ekku.nfc.ui.viewmodel.TAGViewModel
import com.ekku.nfc.util.*
import com.ekku.nfc.util.AppUtils.allowWritePermission
import com.ekku.nfc.util.AppUtils.canWrite
import com.ekku.nfc.util.AppUtils.createConfirmationAlert
import com.ekku.nfc.util.AppUtils.setBrightness
import com.ekku.nfc.util.NetworkUtils.getDeviceIMEI
import com.ekku.nfc.util.NfcUtils.addNfcCallback
import com.ekku.nfc.util.NfcUtils.getNfcAdapter
import com.ekku.nfc.util.NfcUtils.isNFCOnline
import com.ekku.nfc.util.NfcUtils.removeNfcCallback
import com.ekku.nfc.util.NfcUtils.showNFCSettings
import com.ekku.nfc.util.NotifyUtils.playNotification
import com.ekku.nfc.util.NotifyUtils.setIntervalWork
import com.ekku.nfc.util.NotifyUtils.setMidNightWork
import com.google.common.io.BaseEncoding
import timber.log.Timber
import com.ekku.nfc.model.Tag as TagEntity

class RestaurantActivity : AppCompatActivity(), NfcAdapter.ReaderCallback,
    CurrentLocation.LocationResultListener {

    private var scanBtnClicked: Boolean = false
    private lateinit var restaurantBinding: ActivityRestaurantBinding
    private var dialog: AlertDialog? = null
    private var currentLocation: CurrentLocation? = null
    private var preventDialogs = false
    private val tagViewMadel: TAGViewModel by viewModels {
        TAGViewModel.TagViewModelFactory((application as AppDelegate).repository)
    }
    private lateinit var nfcTagScanListLocal: MutableList<TagEntity>
    private lateinit var nfcTagScanList: MutableList<TagAPI>
    val TAG_SYNC_TIME = 1000 * 5L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restaurantBinding = ActivityRestaurantBinding.inflate(layoutInflater)
        val view = restaurantBinding.root
        setContentView(view)

        tagViewMadel.allTags.observe(this, { tags ->
            tags?.let { Timber.d("Tag list to be synced: $it") }
        })
        // tag data syncing to google sheet of every scan.
        Thread {
            while (!isFinishing) {
                Thread.sleep(TAG_SYNC_TIME)
                runOnUiThread {
                    syncData(tagViewMadel.allTags.value)
                }
            }
        }.start()
        // instantiate location object.
        currentLocation = CurrentLocation(this@RestaurantActivity)
        // save IMEI to global guid. if permission is allowed.
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        )
            AppUtils.STRING_GUID = getDeviceIMEI()

        // implement alarm manager at midnight, intervals.
        if (!getDefaultPreferences().getBoolean(AppUtils.TAG_ALARM_KEY, false)) {
            getDefaultPreferences().edit().putBoolean(AppUtils.TAG_ALARM_KEY, true).apply()
            setMidNightWork()
            setIntervalWork()
        }

        restaurantBinding.btnScan.setOnClickListener {
            scanBtnClicked = true
            hideSystemKeyboard(this@RestaurantActivity)
            restaurantBinding.orderField.clearFocus()
            restaurantBinding.scansGroup.visibility = View.GONE
            restaurantBinding.containersGroup.visibility = View.VISIBLE
            restaurantBinding.orderField.isEnabled = false
            // initialize list for scans against orderID
            // enable NFC scanning
            setUpNfc()
            //setUpTorch(true)
            nfcTagScanList = mutableListOf()
            nfcTagScanListLocal = mutableListOf()
        }

        restaurantBinding.clearContainers.setOnClickListener {
            // user forget something so clearing what's been done.
            reset()
        }

        restaurantBinding.btnSubmit.setOnClickListener {
            // user is ready to upload data to server and save in local db.
            submitTagData()
            reset()
        }

        restaurantBinding.orderField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                restaurantBinding.textOrderDesc.setTextColor(
                    ContextCompat.getColor(this@RestaurantActivity, R.color.purple_500)
                )
            }

            override fun afterTextChanged(s: Editable?) {
                // can add multi options later.
                /*
                Handler(Looper.getMainLooper()).postDelayed({
                    restaurantBinding?.orderField?.clearFocus()
                }, 3000)
                */
            }
        })

    }

    override fun onResume() {
        super.onResume()
        if (scanBtnClicked)
            setUpNfc()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (canWrite)
            setBrightness(
                .0F, 0,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC, this@RestaurantActivity
            )
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
        if (grantResults[2] == PackageManager.PERMISSION_DENIED) {
            // its compulsory for uniqueness of a device. must grant it.
            // show dialog to the user.
            showDialog(
                getString(R.string.dialog_phone_title),
                getString(R.string.dialog_phone_desc),
                right = getString(R.string.okay), dialogType = 103
            )
        }
    }

    /**
     * check no duplication happened tags must be unique.
     * */
    private var checkUniqueId = "random_uid"
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
                if (tagEntity.tag_uid != checkUniqueId) {
                    checkUniqueId = tagEntity.tag_uid
                    nfcTagScanListLocal.add(tagEntity)
                    //nfcTagScanList.add(tagAPI)
                    restaurantBinding.containersNumber.text = nfcTagScanListLocal.size.toString()
                }
            }
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
            getDefaultPreferences().edit().putString("GPS_DATA", "$lat, $long").apply()
        }
    }

    private fun reset() {
        restaurantBinding.containersNumber.text = ""
        restaurantBinding.orderField.setText("")
        restaurantBinding.textOrderDesc.setTextColor(
            ContextCompat.getColor(this@RestaurantActivity, android.R.color.background_dark)
        )
        restaurantBinding.scansGroup.visibility = View.VISIBLE
        restaurantBinding.containersGroup.visibility = View.GONE
        restaurantBinding.orderField.isEnabled = true
        // remove list created for scans against orderID
        // disable nfc scanning
        nfcTagScanList.clear()
        nfcTagScanListLocal.clear()
        //setUpTorch(false)
        removeNfcCallback(this@RestaurantActivity)
        scanBtnClicked = false
    }

    private fun setUpNfc() {
        if (isNFCOnline()) {
            addNfcCallback(this@RestaurantActivity, this)
            if (!canWrite) {
                showDialog(
                    getString(R.string.txt_dim_title),
                    getString(R.string.txt_dim_desc),
                    right = getString(R.string.txt_go_to_settings), dialogType = 102
                )
            } else {
                setBrightness(
                    -1F, 20,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL, this@RestaurantActivity
                )
                if (!preventDialogs) {
                    preventDialogs = true
                    if (ContextCompat.checkSelfPermission(
                            this, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED && AppUtils.isAPI23
                    ) {
                        currentLocation?.getLocation(this)
                        askForPermission()
                    } else if (Build.VERSION.SDK_INT < 23) {
                        currentLocation?.getLocation(this)
                    } else {
                        askForPermission()
                    }
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

    private fun syncData(tagList: List<TagAPI>?) {
        tagList?.let {
            if (it.isEmpty())
                return
            for (tag in it) {
                if (tag.tag_sync == 0)
                    tagViewMadel.postTag(tag).observe(this, { result ->
                        result?.let { resource ->
                            when (resource.status) {
                                Status.SUCCESS -> {
                                    Timber.d("${tag.id} tag data is synced successfully.")
                                    // update the status
                                    tag.tag_sync = 1
                                    tagViewMadel.update(
                                        tagUpdate = TagDao.TagUpdate(
                                            tag.id,
                                            tag.tag_sync
                                        )
                                    )
                                }
                                Status.ERROR -> {
                                    Timber.d("data is not synced as expected")
                                }
                                Status.LOADING -> {
                                }
                            }
                        }
                    })
            }
        }
    }

    private fun submitTagData() {
        for (tag in nfcTagScanListLocal) {
            val tagAPI = TagAPI(
                tag_uid = tag.tag_uid,
                tag_date_time = tag.tag_date_time,
                tag_phone_uid = tag.tag_phone_uid,
                tag_sync = tag.tag_sync,
                tag_orderId = tag.tag_orderId
            )
            tagViewMadel.postTag(
                tagAPI
            ).observe(this, { it1 ->
                it1?.let { resource ->
                    when (resource.status) {
                        Status.SUCCESS -> {
                            resource.data?.let {
                                Timber.d("tag data uploaded successfully ${tag.tag_sync}")
                                tagViewMadel.insert(tag)
                            }
                        }
                        Status.ERROR -> {
                            tag.tag_sync = 0
                            Timber.d("tag data not uploaded. ${tag.tag_sync}")
                            tagViewMadel.insert(tag)
                        }
                        Status.LOADING -> {}
                    }
                }
            })
        }
    }

    private fun showDialog(
        title: String,
        desc: String,
        right: String = "",
        left: String = "",
        dialogType: Int
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
                    }
                }
            })
        if (!isFinishing)
            dialog?.show()
    }

    private fun askForPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
            ), 1001
        )
    }

}