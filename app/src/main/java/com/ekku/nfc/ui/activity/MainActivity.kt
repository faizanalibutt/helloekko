package com.ekku.nfc.ui.activity

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.nfc.NfcAdapter.ReaderCallback
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ekku.nfc.AppDelegate
import com.ekku.nfc.R
import com.ekku.nfc.model.TagAPI
import com.ekku.nfc.model.TagDao
import com.ekku.nfc.ui.adapter.TagListAdapter
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

import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*
import com.ekku.nfc.model.Tag as TagEntity


class MainActivity : AppCompatActivity(), ReaderCallback, CurrentLocation.LocationResultListener {

    private var dialog: AlertDialog? = null
    private val tagViewMadel: TAGViewModel by viewModels {
        TAGViewModel.TagViewModelFactory((application as AppDelegate).repository)
    }
    private var currentLocation: CurrentLocation? = null
    private var preventDialogs = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.tagListView)
        val adapter = TagListAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        tagViewMadel.allTags.observe(this, { tags ->
            tags?.let { adapter.submitList(it) }
        })

        // tag data syncing to google sheet of every scan.
        Thread {
            while (!isFinishing) {
                Thread.sleep(10000)
                runOnUiThread {
                    syncData(tagViewMadel.allTags.value)
                }
            }
        }.start()

        /**
         * save IMEI to global guid. if permission is allowed.
         */
        if (ContextCompat
                .checkSelfPermission(
                    this, Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
        )
            AppUtils.STRING_GUID = getDeviceIMEI()

        // instantiate location object.
        currentLocation = CurrentLocation(this@MainActivity)

        // implement alarm manager at midnight, intervals.
        if (!getDefaultPreferences().getBoolean(AppUtils.TAG_ALARM_KEY, false)) {
            getDefaultPreferences().edit().putBoolean(AppUtils.TAG_ALARM_KEY, true).apply()
            setMidNightWork()
            setIntervalWork()
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

    override fun onResume() {
        super.onResume()
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
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL, this@MainActivity
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

    override fun onPause() {
        super.onPause()
        removeNfcCallback(this@MainActivity)
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
                    tag_orderId = ""
                )
                val tagEntity = TagEntity(
                    tag_uid = BaseEncoding.base16().encode(tag.id),
                    tag_time = TimeUtils.getToday(),
                    tag_date = TimeUtils.getFormatDate(TimeUtils.getToday()),
                    tag_date_time = TimeUtils.getFormatDateTime(TimeUtils.getToday()),
                    tag_phone_uid = AppUtils.STRING_GUID,
                    tag_sync = 1,
                    tag_orderId = ""
                )
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
        if (canWrite)
            setBrightness(
                .0F, 0,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC, this@MainActivity
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
                                this@MainActivity, arrayOf(
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