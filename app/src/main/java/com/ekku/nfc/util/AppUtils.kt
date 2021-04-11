package com.ekku.nfc.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog

object AppUtils {

    const val TAG_ALARM_KEY: String = "tag_alarm_key"
    const val ALARM_INTERVAL_TIME = 1000 * 60 * 60 * 5L

    // guid to separate app instance.
    var STRING_GUID = ""
    const val NOTIFICATION_ID = 1003

    val isAPI23
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    val isOreo
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    val isAndroidTen
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    // get write system settings permission status
    val Context.canWrite: Boolean
        get() {
            return if (
                isAPI23
            ) Settings.System.canWrite(this) else true
        }

    // confirmation dialog for numerous tasks.
    @JvmStatic
    fun Context.createConfirmationAlert(
        title: String,
        message: String,
        right: String = "",
        left: String = "",
        listener: AlertButtonListener?,
        context: Context = this,
    ): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(true)
            .setNegativeButton(left) { dialogInterface, _ ->
                listener?.onClick(dialogInterface, ButtonType.LEFT)
            }
            .setPositiveButton(right) { dialogInterface, _ ->
                listener?.onClick(dialogInterface, ButtonType.RIGHT)
            }.create()
    }

    // Allow write system settings
    fun Context.allowWritePermission() {
        if (isAPI23) {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    // get screen brightness programmatically
    val Context.brightness: Int
        get() {
            return Settings.System.getInt(
                this.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                0
            )
        }

    // set screen brightness programmatically
    fun Context.setBrightness(value: Int) {
        Settings.System.putInt(
            this.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            value
        )
    }

    fun Context.setBrightness(level: Float, value: Int, mode: Int, activity: ComponentActivity) {
        try {
            Settings.System.putInt(
                this.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                mode
            )
            Settings.System.putInt(this.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val window = activity.window
        val lp = window.attributes
        lp.screenBrightness = level
        window.attributes = lp
    }

    inline fun <reified T : Activity> Context.startActivity(block: Intent.() -> Unit = {}) {
        startActivity(Intent(this, T::class.java).apply(block))
    }

}