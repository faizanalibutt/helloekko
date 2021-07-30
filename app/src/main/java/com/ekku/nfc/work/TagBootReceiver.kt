package com.ekku.nfc.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import com.ekku.nfc.ui.activity.*
import com.ekku.nfc.ui.activity.WelcomeActivity.Companion.APP_MODE
import com.ekku.nfc.util.AppUtils.startActivity
import com.ekku.nfc.util.NotifyUtils.setIntervalWork
import com.ekku.nfc.util.NotifyUtils.setMidNightWork
import com.ekku.nfc.util.getDefaultPreferences
import timber.log.Timber

class TagBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(ACTION_TAG_BOOT_RECIEVER)) {
            Timber.d("On Reboot called Come to Ekko App.")
            val isLoggedIn = context?.getDefaultPreferences()?.getBoolean(
                AccountActivity.LOGIN_PREF, false
            )
            context?.startActivity(
                Intent(
                    context,
                    when (context.getDefaultPreferences().getInt(APP_MODE, -1)) {
                        WelcomeActivity.PARTNER -> {
                            if (isLoggedIn == true) PartnerActivity::class.java else AccountActivity::class.java
                        }
                        WelcomeActivity.DROPBOX -> {
                            if (isLoggedIn == true) DropBoxActivity::class.java else AccountActivity::class.java
                        }
                        WelcomeActivity.ADMIN -> {
                            if (isLoggedIn == true) AdminActivity::class.java else AccountActivity::class.java
                        }
                        else -> WelcomeActivity::class.java
                    }
                ).addFlags(FLAG_ACTIVITY_NEW_TASK)
            )
            context?.setIntervalWork()
        }
    }

    companion object {
        const val ACTION_TAG_BOOT_RECIEVER = "android.intent.action.BOOT_COMPLETED"
    }
}