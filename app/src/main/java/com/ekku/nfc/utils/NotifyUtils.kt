package com.ekku.nfc.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.ekku.nfc.R

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
        else
            notificationBuilder = NotificationCompat.Builder(this, channelName)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(tagDesc)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setLights(Color.GREEN, 1000, 500)
                .setVibrate(longArrayOf(0, 500))
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager, channelName)
        with(notificationManager) {
            notify(notificationId, notificationBuilder.build())
        }
    }

    // create notification channel for oreo and above to give importance to notification.
    private fun Context.createNotificationChannel(notificationManager: NotificationManager, channelName: String) {
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
}