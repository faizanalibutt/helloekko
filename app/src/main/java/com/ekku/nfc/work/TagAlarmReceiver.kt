package com.ekku.nfc.work

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.ekku.nfc.model.*
import com.ekku.nfc.network.ApiClient
import com.ekku.nfc.network.ApiService
import com.ekku.nfc.util.NetworkUtils
import com.ekku.nfc.util.NetworkUtils.getDeviceIMEI
import com.ekku.nfc.util.NotifyUtils.playNotification
import com.ekku.nfc.util.TimeUtils
import com.ekku.nfc.util.getDefaultPreferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber


class TagAlarmReceiver : BroadcastReceiver() {

    private var mNotificationManager: NotificationManager? = null
    private val apiService: ApiService by lazy { ApiClient.apiClient().create(ApiService::class.java) }

    override fun onReceive(context: Context?, intent: Intent?) {

        val database by lazy { context?.let { TagRoomDatabase.getDatabase(it, null) } }
        val repository by lazy { database?.let { TagRepository(it.tagDao()) } }

        if (intent?.action.equals("MIDNIGHT"))
        {
            Timber.d("I'm sending logs to LogSheet.")
            mNotificationManager =
                context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Deliver the notification.
            //context.playNotification("Daily Logs", 1004, "logs_channel")

            // call database object here.
            GlobalScope.launch {
                val sendTodayTagList = repository?.todayTags?.firstOrNull()
                Timber.d("List of today's logs : $sendTodayTagList ")
                sendTodayTagList?.let { logsList ->
                    if (logsList.isEmpty())
                        return@let
                    for (tagData in logsList) {
                        apiService.sendDailyLogs(
                            tagData.id.toString(),
                            tagData.tag_uid,
                            tagData.tag_date_time,
                            tagData.tag_phone_uid,
                            tagData.tag_sync.toString(),
                            tagData.tag_orderId
                        ).enqueue(object : Callback<String> {
                            override fun onFailure(call: Call<String>?, t: Throwable?) {
                                Timber.d("Sending daily logs failed")
                            }
                            override fun onResponse(call: Call<String>?, response: Response<String>?) {}
                        })
                    }
                }
            }
        } else {
            //context?.playNotification("Device Logs", 1005, "device_channel")
            Timber.d("I'm sending logs to DeviceSheet.")
            val currentTime = TimeUtils.getFormatDateTime(System.currentTimeMillis())
            val appUid = context?.getDeviceIMEI()
            val batteryStatus = isConnected(context)
            val location = context?.getDefaultPreferences()?.getString("GPS_DATA", "location permission not allowed")
            val carrierName = NetworkUtils.getCarrierName(context)
            Timber.d("time: $currentTime, uid: $appUid, battery: $batteryStatus, location: $location, carrieName: $carrierName")
            // finally call the API, make sheet first
            apiService.sendDeviceInfo(
                appUid,
                currentTime,
                batteryStatus,
                location ?: "location permission not allowed",
                carrierName
            ).enqueue(object : Callback<String> {
                override fun onFailure(call: Call<String>?, t: Throwable?) {
                    Timber.d("Sending device logs failed: ${t?.message}")
                }
                override fun onResponse(call: Call<String>?, response: Response<String>?) {}
            })
        }

    }

    companion object {

        fun isConnected(context: Context?): String {
            val intent = context?.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val batteryPct: Float? = intent?.let { it ->
                val level: Int = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                level * 100 / scale.toFloat()
            }
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val batteryStatus = plugged == BatteryManager.BATTERY_PLUGGED_AC
                    || plugged == BatteryManager.BATTERY_PLUGGED_USB || isCharging
            return "status: ${if (batteryStatus) "charging" else "idle"}, level: $batteryPct"
        }


    }

}