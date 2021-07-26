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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ekku.nfc.AppDelegate
import com.ekku.nfc.R
import com.ekku.nfc.app.UserActivity
import com.ekku.nfc.model.TagAPI
import com.ekku.nfc.model.TagDao
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
import com.google.common.io.BaseEncoding
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*
import com.ekku.nfc.model.Tag as TagEntity


class ConsumerActivity : UserActivity(), ReaderCallback, CurrentLocation.LocationResultListener {

    private var dialog: AlertDialog? = null
    private val tagViewMadel: TAGViewModel by viewModels {
        TAGViewModel.TagViewModelFactory((application as AppDelegate).repository)
    }
    private var currentLocation: CurrentLocation? = null
    private var preventDialogs = false
    private lateinit var nfcTagScanList: MutableList<TagEntity>

    /**
     * check no duplication happened tags must be unique.
     * */
    private var isIdAvailable = true
    private var isNfcStarted = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consumer)

        if (getDefaultPreferences().getBoolean("HEAD_JACK_RESPONSE", false)) {
            isNfcStarted = true
        } else {
            isNfcStarted = false
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val recyclerView = findViewById<RecyclerView>(R.id.tagListView)
        val adapter = TagListAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        nfcTagScanList = mutableListOf()

        tagViewMadel.allTags.observe(this, { tags ->
            tags?.let { adapter.submitList(it) }
        })

        tagViewMadel.syncTags.observe(this, { tags ->
            tags?.let { Timber.d("Synced Tag List: $it") }
        })

        // tag data syncing to google sheet of every scan.
        Thread {
            while (!isFinishing) {
                Thread.sleep(AppUtils.TAG_SYNC_TIME)
                runOnUiThread {
                    syncData(tagViewMadel.syncTags.value)
                }
            }
        }.start()

        /**
         * save IMEI to global guid. if permission is allowed.
         */
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        )
            AppUtils.STRING_GUID = getDeviceIMEI()

        // instantiate location object.
        currentLocation = CurrentLocation(this@ConsumerActivity)

        // implement alarm manager at midnight, intervals.
        if (!getDefaultPreferences().getBoolean(AppUtils.TAG_ALARM_KEY, false)) {
            getDefaultPreferences().edit().putBoolean(AppUtils.TAG_ALARM_KEY, true).apply()
            setMidNightWork()
            setIntervalWork()
        }

        (getSystemService(Context.AUDIO_SERVICE) as AudioManager).registerMediaButtonEventReceiver(
            ComponentName(
                packageName,
                MediaButtonEventReceiver::class.java.name
            )
        )

    }

    override fun onResume() {
        super.onResume()
        if(isNfcStarted) {
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
                    dialog = createConfirmationAlert(
                        getString(R.string.txt_dim_title),
                        getString(R.string.txt_dim_desc),
                        right = getString(R.string.txt_go_to_settings),
                        listener = object : AlertButtonListener {
                            override fun onClick(
                                dialog: DialogInterface,
                                type: ButtonType
                            ) {
                                if (type == ButtonType.RIGHT)
                                    allowWritePermission()
                            }
                        })
                    if (!isFinishing)
                        dialog?.show()
                } else {
                    setBrightness(
                        -1F, 20,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL, this@ConsumerActivity
                    )
                    if (!preventDialogs) {
                        preventDialogs = true
                        if (ContextCompat.checkSelfPermission(
                                this, Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED && AppUtils.isAPI23
                        ) {
                            currentLocation?.getLocation(this)
                            ActivityCompat.requestPermissions(
                                this, arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.READ_PHONE_STATE
                                ), 1001
                            )
                        } else if (Build.VERSION.SDK_INT < 23) {
                            currentLocation?.getLocation(this)
                        } else {
                            ActivityCompat.requestPermissions(
                                this, arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.READ_PHONE_STATE
                                ), 1001
                            )
                        }
                    }
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    setUpTorch(false)
                    window.clearFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    )
                    removeNfcCallback(this@ConsumerActivity)
                }, CONSUMER_TIME_OUT)

            } else
                getNfcAdapter()?.let {
                    val isShowing = dialog?.isShowing ?: false
                    if (isShowing)
                        return
                    dialog = createConfirmationAlert(
                        getString(R.string.dialog_nfc_title),
                        getString(R.string.dialog_nfc_desc),
                        right = getString(R.string.txt_go_to_settings),
                        listener = object : AlertButtonListener {
                            override fun onClick(
                                dialog: DialogInterface, type: ButtonType
                            ) {
                                if (type == ButtonType.RIGHT)
                                    showNFCSettings()
                            }
                        })

                    if (!isFinishing)
                        dialog?.show()
                }
        }
    }

    private fun syncData(tagList: List<TagAPI>?) {
        tagList?.let {
            if (it.isEmpty())
                return
            for (tag in it) {
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

    private var camera: Camera? = null
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

    override fun onTagDiscovered(tag: Tag?) {
        if (tag != null) {
            Timber.d("Tag Id is: ${BaseEncoding.base16().encode(tag.id)}")
            Handler(Looper.getMainLooper()).post {
                val tagAPI = TagAPI(
                    tag_uid = BaseEncoding.base16().encode(tag.id),
                    tag_date_time = TimeUtils.getFormatDateTime(TimeUtils.getToday()),
                    tag_phone_uid = AppUtils.STRING_GUID,
                    tag_sync = 1,
                    tag_orderId = "consumer"
                )
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

                nfcTagScanList.add(tagEntity)
                tagViewMadel.postTag(
                    tagAPI
                ).observe(this, { it1 ->
                    it1?.let { resource ->
                        when (resource.status) {
                            Status.SUCCESS -> {
                                resource.data?.let {
                                    Timber.d("tag data uploaded successfully ${tagEntity.tag_sync}")
                                    tagViewMadel.insert(tagEntity)
                                }
                            }
                            Status.ERROR -> {
                                tagEntity.tag_sync = 0
                                Timber.d("tag data not uploaded. ${tagEntity.tag_sync}")
                                tagViewMadel.insert(tagEntity)
                            }
                            Status.LOADING -> {
                            }
                        }
                    }
                })
            }
            setUpTorch(false)
            Handler(Looper.getMainLooper()).postDelayed({
                setUpTorch(true)
            }, 700)
            playNotification(
                getString(R.string.notification_desc), AppUtils.NOTIFICATION_ID, "loved_it"
            )
        } else
            playNotification(
                getString(R.string.notification_desc_unses), AppUtils.NOTIFICATION_ID, "loved_it"
            )
    }

    override fun onDestroy() {
        super.onDestroy()
        setUpTorch(false)
        if (canWrite)
            setBrightness(
                .0F, 0,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC, this@ConsumerActivity
            )
    }

    private var mCurrentLocation: Location? = null
    override fun gotLocation(location: Location?) {
        location?.let {
            mCurrentLocation = it
            val lat = mCurrentLocation?.latitude
            val long = mCurrentLocation?.longitude

            getDefaultPreferences().edit().putString("GPS_DATA", "$lat, $long").apply()
        }
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
            /**
             * its compulsory for uniqueness of a device. must grant it.
             * show dialog to the user.
             */
            val isShowing = dialog?.isShowing ?: false
            if (isShowing)
                return
            dialog = createConfirmationAlert(
                getString(R.string.dialog_phone_title),
                getString(R.string.dialog_phone_desc),
                right = getString(R.string.okay),
                listener = object : AlertButtonListener {
                    override fun onClick(
                        dialog: DialogInterface, type: ButtonType
                    ) {
                        if (type == ButtonType.RIGHT)
                            ActivityCompat.requestPermissions(
                                this@ConsumerActivity, arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.READ_PHONE_STATE
                                ), 1001
                            )
                    }
                })

            if (!isFinishing)
                dialog?.show()
        }
    }

}