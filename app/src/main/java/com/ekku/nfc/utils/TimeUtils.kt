package com.ekku.nfc.utils

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {

    fun getFormatDateTime(millis: Long): String {
        return SimpleDateFormat("yyyy:M:dd:hh:mm:ss", Locale.getDefault()).format(Date(millis))
    }

    fun getFormatDate(millis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
    }

    /**
     * @return milliseconds since 1.1.1970 for today 0:00:00 local timezone
     */
    fun getToday(): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = System.currentTimeMillis()
        return c.timeInMillis
    }

    fun getPreviousDay(): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DATE, -1)
        }
        return c.timeInMillis
    }
}