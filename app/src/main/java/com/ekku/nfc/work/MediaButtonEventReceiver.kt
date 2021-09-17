package com.ekku.nfc.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.view.KeyEvent
import com.ekku.nfc.ui.activity.DropBoxActivity
import com.ekku.nfc.util.AppUtils.startActivity
import com.ekku.nfc.util.getDefaultPreferences
import timber.log.Timber

class MediaButtonEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val intentAction = intent?.action
        if (ACTION_MEDIA_BUTTON != intentAction) return
        val event = intent.getParcelableExtra<KeyEvent>(EXTRA_KEY_EVENT)
            ?: return
        val action = event.action
        if (action == KeyEvent.ACTION_DOWN) {
            // do something
            // call for localBroadcast to enable flash and nfc scanning.
            // activity will be started with new intent and parameters
            Timber.e("onButtonReceive: ")
            if (context?.getDefaultPreferences()?.getBoolean("HEAD_JACK_RESPONSE", false) == false)
                context.getDefaultPreferences().edit()?.putBoolean("HEAD_JACK_RESPONSE", true)?.apply()
            else
                context?.getDefaultPreferences()?.edit()?.putBoolean("HEAD_JACK_RESPONSE", false)?.apply()
            context?.startActivity<DropBoxActivity> {
                this.addFlags(FLAG_ACTIVITY_CLEAR_TOP)
                this.addFlags(FLAG_ACTIVITY_CLEAR_TASK)
                this.addFlags(FLAG_ACTIVITY_NEW_TASK)
            }

        }
        try{
            abortBroadcast()
        } catch (ignore: Exception) {}
    }

}