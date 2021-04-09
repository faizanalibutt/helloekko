package com.ekku.nfc.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.appcompat.app.AppCompatActivity
import com.ekku.nfc.ui.activity.MainActivity
import com.ekku.nfc.util.AppUtils
import com.ekku.nfc.util.NotifyUtils.playNotification
import timber.log.Timber
import java.util.*

class TagBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(ACTION_TAG_BOOT_RECIEVER)) {
            Timber.d("On Reboot called come to Ekko app too.")
            setMidNightWork(context)
            setIntervalWork(context)
            context?.playNotification("Boot Called", 10007, "boot_channel")
            context?.startActivity(Intent(context, MainActivity::class.java).addFlags(FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun setMidNightWork(context: Context?) {
        val alarmIntent = Intent(context, TagAlarmReceiver::class.java)
        val pendingIntent: PendingIntent? = PendingIntent.getBroadcast(
            context, 0, alarmIntent, 0
        )
        val manager = context?.getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager
        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        manager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
        Timber.d("Alarm set from Boot Successfully for midnight")
    }

    private fun setIntervalWork(context: Context?) {
        val alarmIntent = Intent(context, TagAlarmReceiver::class.java)
        val pendingIntent: PendingIntent? = PendingIntent.getBroadcast(
            context, 0, alarmIntent, 0
        )
        val manager = context?.getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager

        val firstInterval: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }
        val secondInterval: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 17)
            set(Calendar.MINUTE, 0)
        }
        val thirdInterval: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 0)
        }
        val fourInterval: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 5)
            set(Calendar.MINUTE, 0)
        }
        var desiredInterval: Long
        var currentInterval = System.currentTimeMillis()
        val intervalsList = arrayOf(
            firstInterval.timeInMillis, secondInterval.timeInMillis, thirdInterval.timeInMillis,
            fourInterval.timeInMillis
        )

        for (interval in intervalsList) {
            when {
                currentInterval < interval -> {
                    // 22:00 = 22:00 - 20:48 -> 80
                    desiredInterval = interval - currentInterval
                    currentInterval += desiredInterval
                }
                currentInterval == interval -> {
                    desiredInterval = interval - currentInterval
                    currentInterval += desiredInterval
                }
            }
        }

        manager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            currentInterval,
            AppUtils.ALARM_INTERVAL_TIME,
            pendingIntent
        )
        Timber.d("Alarm set successfully for intervals")
    }

companion object {
        const val ACTION_TAG_BOOT_RECIEVER = "android.intent.action.BOOT_COMPLETED"
    }
}