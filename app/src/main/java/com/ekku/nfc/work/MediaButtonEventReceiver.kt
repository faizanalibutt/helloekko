package com.ekku.nfc.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.view.KeyEvent
import com.ekku.nfc.R
import com.ekku.nfc.ui.activity.MainActivity
import com.ekku.nfc.util.AppUtils
import com.ekku.nfc.util.AppUtils.startActivity
import com.ekku.nfc.util.NotifyUtils.playNotification
import com.ekku.nfc.util.getDefaultPreferences
import timber.log.Timber

class MediaButtonEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val intentAction = intent?.action
        if (Intent.ACTION_MEDIA_BUTTON != intentAction) return
        val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            ?: return
        val action = event.action
        if (action == KeyEvent.ACTION_DOWN) {
            // do something
            // call for localBroadcast to enable flash and nfc scanning.
            // activity will be started with new intent and parameters
            Timber.e("onReceive: ")
            if (context?.getDefaultPreferences()?.getBoolean("HEAD_JACK_RESPONSE", false) == false)
                context.getDefaultPreferences().edit()?.putBoolean("HEAD_JACK_RESPONSE", true)?.apply()
            else
                context?.getDefaultPreferences()?.edit()?.putBoolean("HEAD_JACK_RESPONSE", false)?.apply()
            context?.startActivity<MainActivity> {
                this.addFlags(FLAG_ACTIVITY_CLEAR_TOP)
                this.addFlags(FLAG_ACTIVITY_CLEAR_TASK)
                //this.addFlags(FLAG_ACTIVITY_SINGLE_TOP)
                this.addFlags(FLAG_ACTIVITY_NEW_TASK)
                //this.action = "ACTION_HEAD_PHONE_JACK_CLICK"
            }

        }
        abortBroadcast()
    }

}