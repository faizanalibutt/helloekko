package com.ekku.nfc.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import com.ekku.nfc.ui.activity.DropBoxActivity
import com.ekku.nfc.ui.activity.PartnerActivity
import com.ekku.nfc.ui.activity.WelcomeActivity
import com.ekku.nfc.util.NotifyUtils.setIntervalWork
import com.ekku.nfc.util.NotifyUtils.setMidNightWork
import com.ekku.nfc.util.getDefaultPreferences
import timber.log.Timber

class TagBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(ACTION_TAG_BOOT_RECIEVER)) {
            Timber.d("On Reboot called come to Ekko app too.")
            //context?.playNotification("Boot Called", 10007, "boot_channel")
            context?.startActivity(
                Intent(
                    context, when (context.getDefaultPreferences().getInt("APP_TYPE", -1)) {
                        0 -> PartnerActivity::class.java
                        1 -> DropBoxActivity::class.java
                        else -> WelcomeActivity::class.java
                    }
                ).addFlags(FLAG_ACTIVITY_NEW_TASK)
            )

            context?.setMidNightWork()
            context?.setIntervalWork()
        }
    }

    companion object {
        const val ACTION_TAG_BOOT_RECIEVER = "android.intent.action.BOOT_COMPLETED"
    }
}