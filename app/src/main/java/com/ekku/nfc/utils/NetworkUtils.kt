package com.ekku.nfc.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import timber.log.Timber


object NetworkUtils {

    @JvmStatic
    fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    fun getCarrierName(context: Context?): String {
        val connectivityManager =
            context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isCellularWifi =
            connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)?.isConnected
        if (isCellularWifi == true) {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
            val info = wifiManager?.connectionInfo
            if (info?.supplicantState == SupplicantState.COMPLETED) {
                return info.ssid ?: "wifi ssid not available"
            }
        } else {
            val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
            return manager?.networkOperatorName ?: "cellular name not available"
        }
        return "something wrong"
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    fun Context.getDeviceIMEI(): String {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        kotlin.runCatching {
            return if (AppUtils.isOreo)
                telephonyManager?.imei ?: "unable to get device id"
            else telephonyManager?.deviceId ?: "unable to get device id"
        }.onFailure { Timber.d("IMEI Exception Message: ${it.message}") }
        return "unable to get device id"
    }

}