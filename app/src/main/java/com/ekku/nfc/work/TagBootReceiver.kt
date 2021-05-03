package com.ekku.nfc.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.appcompat.app.AppCompatActivity
import com.ekku.nfc.ui.activity.ConsumerActivity
import com.ekku.nfc.ui.activity.MainActivity
import com.ekku.nfc.ui.activity.RestaurantActivity
import com.ekku.nfc.ui.activity.WelcomeActivity
import com.ekku.nfc.util.AppUtils
import com.ekku.nfc.util.NotifyUtils.playNotification
import com.ekku.nfc.util.getDefaultPreferences
import timber.log.Timber
import java.util.*

class TagBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(ACTION_TAG_BOOT_RECIEVER)) {
            Timber.d("On Reboot called come to Ekko app too.")
            //context?.playNotification("Boot Called", 10007, "boot_channel")
            context?.startActivity(
                Intent(
                    context, when (context.getDefaultPreferences().getInt("APP_TYPE", -1)) {
                        0 -> RestaurantActivity::class.java
                        1 -> MainActivity::class.java
                        else -> WelcomeActivity::class.java
                    }
                ).addFlags(FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    companion object {
        const val ACTION_TAG_BOOT_RECIEVER = "android.intent.action.BOOT_COMPLETED"
    }
}