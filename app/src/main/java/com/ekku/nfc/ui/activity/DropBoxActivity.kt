package com.ekku.nfc.ui.activity

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.camera2.CameraManager
import android.location.Location
import android.media.AudioManager
import android.nfc.NfcAdapter.ReaderCallback
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ekku.nfc.AppDelegate
import com.ekku.nfc.R
import com.ekku.nfc.app.UserActivity
import com.ekku.nfc.ui.adapter.TagListAdapter
import com.ekku.nfc.ui.viewmodel.TAGViewModel
import com.ekku.nfc.util.*
import com.ekku.nfc.util.AppUtils.CONSUMER_TIME_OUT
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
import com.ekku.nfc.work.MediaButtonEventReceiver
import com.google.android.material.snackbar.Snackbar
import com.google.common.io.BaseEncoding
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*
import com.ekku.nfc.model.Tag as TagEntity


class DropBoxActivity : UserActivity(), ReaderCallback, CurrentLocation.LocationResultListener {

    private var dialog: AlertDialog? = null
    private val tagViewMadel: TAGViewModel by viewModels {
        TAGViewModel.TagViewModelFactory((application as AppDelegate).repository, this)
    }
    private var preventDialogs = false

    // token has information about dropbox
    private val dropBoxToken by lazy {
        getDefaultPreferences().getString(
            AccountActivity.LOGIN_TOKEN, "put-your-login-token-here"
        )
    }
    /**
     * check no duplication happened tags must be unique.
     * */
    private lateinit var nfcTagScanList: MutableList<TagEntity>
    private var isIdAvailable = true
    private var isNfcStarted = true

    // access location for device
    private var mCurrentLocation: Location? = null
    private var currentLocation: CurrentLocation? = null

    // take camera variable for flash
    private var camera: Camera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consumer)

        if (getDefaultPreferences().getBoolean("HEAD_JACK_RESPONSE", false)) {
            isNfcStarted = true
        } else {
            isNfcStarted = false
            disableScanning()
        }

        /**
         * give some feedbacks to us dropbox becomes empty.
         */
        val recyclerView = findViewById<RecyclerView>(R.id.tagListView)
        val adapter = TagListAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        nfcTagScanList = mutableListOf()

        tagViewMadel.allTags.observe(this, { tags ->
            tags?.let { adapter.submitList(it) }
        })

        /**
         * save IMEI to global guid. if permission is allowed.
         */
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        )
            AppUtils.STRING_GUID = getDeviceIMEI()

        // instantiate location object.
        currentLocation = CurrentLocation(this@DropBoxActivity)

        // implement alarm manager at midnight, intervals.
        if (!getDefaultPreferences().getBoolean(AppUtils.TAG_ALARM_KEY, false)) {
            getDefaultPreferences().edit().putBoolean(AppUtils.TAG_ALARM_KEY, true).apply()
            setIntervalWork()
        }

        // headphone jack code here.
        (getSystemService(Context.AUDIO_SERVICE) as AudioManager).registerMediaButtonEventReceiver(
            ComponentName(
                packageName,
                MediaButtonEventReceiver::class.java.name
            )
        )

        // dropbox name is here.
        supportActionBar?.let {
            it.title =
                getDataFromToken("dropboxName", dropBoxToken)?.asString() ?: "title not found"
        }

        if (!NetworkUtils.isOnline(this)) {
            Snackbar.make(recyclerView,
                "No Internet Connection Available, Please connect to network", Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isNfcStarted) {
            setUpTorch(true)
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
            if (isNFCOnline()) {
                addNfcCallback(this, this)
                if (!canWrite) {
                    showDialog(
                        getString(R.string.txt_dim_title),
                        getString(R.string.txt_dim_desc),
                        right = getString(R.string.txt_go_to_settings), dialogType = 102
                    )
                } else {
                    setBrightness(
                        -1F, 20,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL, this@DropBoxActivity
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
                Handler(Looper.getMainLooper()).postDelayed({
                    // halt the scanning operation manually
                    disableScanning()
                }, CONSUMER_TIME_OUT)
            } else
                getNfcAdapter()?.let {
                    showDialog(
                        getString(R.string.dialog_nfc_title),
                        getString(R.string.dialog_nfc_desc),
                        right = getString(R.string.txt_go_to_settings), dialogType = 101
                    )
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        setUpTorch(false)
        if (canWrite)
            setBrightness(
                .5F, 10,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC, this@DropBoxActivity
            )
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
                    tag_orderId = "consumer"
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
                if (!isIdAvailable)
                    return@post
                // api call here
                tagViewMadel.postDropBoxData(
                    BaseEncoding.base16().encode(tag.id),
                    dropBoxId = getDataFromToken(tokenName = "id", dropBoxToken)?.asString() ?: "error"
                ).observe(this, { it ->
                    it?.let { resource ->
                        when (resource.status) {
                            Status.SUCCESS -> {
                                resource.data?.let { Timber.d("tag data uploaded successfully $it") }
                                tagViewMadel.insert(tagEntity)
                            }
                            Status.ERROR -> {
                                Timber.d("tag data not uploaded. ${resource.message}")
                                tagViewMadel.insert(tagEntity)
                            }
                            Status.LOADING -> {
                            }
                        }
                    }
                })
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

    override fun gotLocation(location: Location?) {
        location?.let {
            mCurrentLocation = it
            val lat = mCurrentLocation?.latitude
            val long = mCurrentLocation?.longitude
            getDefaultPreferences().edit().putString("GPS_DATA_LAT", "$lat").apply()
            getDefaultPreferences().edit().putString("GPS_DATA_LONG", "$long").apply()
            Timber.d("Location DropBox: $lat, $long")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
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
            /**
             * its compulsory for uniqueness of a device. must grant it.
             * show dialog to the user.
             */
            showDialog(
                getString(R.string.dialog_phone_title),
                getString(R.string.dialog_phone_desc),
                right = getString(R.string.okay), dialogType = 103
            )
        }
    }

    private fun disableScanning() {
        setUpTorch(false)
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        removeNfcCallback(this@DropBoxActivity)
        getDefaultPreferences().edit()?.putBoolean("HEAD_JACK_RESPONSE", false)?.apply()
    }

    private fun setUpTorch(torchSwitch: Boolean) {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
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
                Toast.makeText(this, "This device has no flash", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "This device has no camera", Toast.LENGTH_SHORT).show()
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
                        type == ButtonType.RIGHT && dialogType == 104 -> dialog.dismiss()
                    }
                }
            })
        if (!isFinishing)
            dialog?.show()
    }

}