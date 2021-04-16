package com.ekku.nfc.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.ekku.nfc.R
import com.ekku.nfc.work.TagAlarmReceiver
import timber.log.Timber
import java.util.*


object NotifyUtils {
    // custom sound provided by client.
    fun Context.playSound() {
        val mediaPlayer = MediaPlayer.create(this, R.raw.beep_ton)
        mediaPlayer.start()
    }

    // play vibration instead of notification vibration.
    fun Context.playVibration() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (AppUtils.isOreo) {
            vibrator.vibrate(
                VibrationEffect
                    .createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    // build notification to let user know nfc tag is scanned.
    fun Context.playNotification(tagDesc: String, notificationId: Int, channelName: String) {
        val notificationBuilder: NotificationCompat.Builder
        if (AppUtils.isOreo)
            notificationBuilder = NotificationCompat.Builder(this, channelName)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(tagDesc)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
        else {
            notificationBuilder = NotificationCompat.Builder(this, channelName)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(tagDesc)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setLights(Color.GREEN, 1000, 500)
                .setVibrate(longArrayOf(0, 500))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager, channelName)
        with(notificationManager) {
            notify(notificationId, notificationBuilder.build())
        }
    }

    // create notification channel for oreo and above to give importance to notification.
    private fun Context.createNotificationChannel(
        notificationManager: NotificationManager,
        channelName: String
    ) {
        if (AppUtils.isOreo) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel =
                NotificationChannel(channelName, name, importance)
                    .apply {
                        setShowBadge(false)
                    }
            channel.enableLights(true)
            channel.lightColor = Color.GREEN
            channel.enableVibration(true)
            channel.description = descriptionText
            // Register the channel with the system
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun Context.setIntervalWork() {
        val alarmIntent = Intent(this, TagAlarmReceiver::class.java)
        val pendingIntent: PendingIntent? = PendingIntent.getBroadcast(
            this, 0, alarmIntent, 0
        )
        val manager = getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager

        val firstInterval: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 5)
            set(Calendar.MINUTE, 0)
        }
        val secondInterval: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }
        val thirdInterval: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 17)
            set(Calendar.MINUTE, 0)
        }
        val fourInterval: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 22)
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
                    break
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
        Timber.d(
            "Alarm set successfully for intervals: ${
                TimeUtils.getFormatDateTime(
                    currentInterval
                )
            }"
        )
    }

    fun Context.setMidNightWork() {
        val alarmIntent = Intent(
            this, TagAlarmReceiver::class.java
        ).setAction("MIDNIGHT")
        val pendingIntent: PendingIntent? = PendingIntent.getBroadcast(
            this, 0, alarmIntent, 0
        )
        val manager = getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager
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
        Timber.d("Alarm set successfully for midnight")
    }

}