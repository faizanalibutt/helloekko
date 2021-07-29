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
import com.ekku.nfc.repository.TagRepository
import com.ekku.nfc.ui.activity.AccountActivity
import com.ekku.nfc.util.NetworkUtils
import com.ekku.nfc.util.NetworkUtils.getDeviceIMEI
import com.ekku.nfc.util.TimeUtils
import com.ekku.nfc.util.getDataFromToken
import com.ekku.nfc.util.getDefaultPreferences
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber


class TagAlarmReceiver : BroadcastReceiver() {

    private var mNotificationManager: NotificationManager? = null
    private val apiService: ApiService by lazy {
        ApiClient.apiClient().create(ApiService::class.java)
    }

    @DelicateCoroutinesApi
    override fun onReceive(context: Context?, intent: Intent?) {

        val authToken by lazy {
            context?.getDefaultPreferences()?.getString(
                AccountActivity.LOGIN_TOKEN, "put-your-login-token-here"
            )
        }

        if (intent?.action.equals("INTERVAL")) {
            //context?.playNotification("Device Logs", 1005, "device_channel")
            Timber.d("I'm sending logs to DeviceSheet.")
            val currentTime = TimeUtils.getFormatDateTime(System.currentTimeMillis())
            val appUid = context?.getDeviceIMEI()
            val batteryStatus = isConnected(context)
            val location_lat = context?.getDefaultPreferences()
                ?.getString("GPS_DATA_LAT", "location permission not allowed")
            val location_long = context?.getDefaultPreferences()
                ?.getString("GPS_DATA_LONG", "location permission not allowed")
            val carrierName = NetworkUtils.getCarrierName(context)

            // summary of all we got
            Timber.d("time: $currentTime, uid: $appUid, battery: $batteryStatus, location: $location_lat, $location_long, carrieName: $carrierName")

            // finally call the API, make sheet first
            apiService.sendDeviceInfo(
                user_id = getDataFromToken(tokenName = "id", authToken)?.asString() ?: "F",
                name = getDataFromToken("partnerName", authToken)?.asString()
                    ?: getDataFromToken("dropboxName", authToken)?.asString() ?: "UNKNOWN_NAME",
                latitude = location_lat ?: "location permission not allowed",
                longitude = location_long ?: "location permission not allowed",
                phone_uid = appUid ?: "app imei not found",
                battery = batteryStatus,
                network = carrierName
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
            val intent =
                context?.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val batteryPct: Float? = intent?.let { it ->
                val level: Int = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                level * 100 / scale.toFloat()
            }
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging =
                status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val batteryStatus = plugged == BatteryManager.BATTERY_PLUGGED_AC
                    || plugged == BatteryManager.BATTERY_PLUGGED_USB || isCharging
            return "status: ${if (batteryStatus) "charging" else "idle"}, level: $batteryPct"
        }


    }

}