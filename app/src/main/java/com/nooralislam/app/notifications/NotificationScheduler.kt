package com.nooralislam.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.nooralislam.app.data.PrayerTimesModel
import com.nooralislam.app.data.ReminderPrefs
import java.util.Calendar
import kotlin.math.floor

object NotificationScheduler {
    private fun scheduleAt(context: Context, requestCode: Int, triggerAtMillis: Long, title: String, body: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra("id", requestCode)
            putExtra("title", title)
            putExtra("body", body)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        runCatching {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun cancelAt(context: Context, requestCode: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }

    fun rescheduleAll(context: Context, prefs: ReminderPrefs, times: PrayerTimesModel) {
        // 1. Pre-prayer 30-min reminders (codes 201..205)
        val prayers = listOf(
            Triple(201, "الفجر", times.fajr),
            Triple(202, "الظهر", times.dhuhr),
            Triple(203, "العصر", times.asr),
            Triple(204, "المغرب", times.maghrib),
            Triple(205, "العشاء", times.isha)
        )
        val now = System.currentTimeMillis()
        for ((code, nameAr, h) in prayers) {
            if (!prefs.prePrayer30Min) {
                cancelAt(context, code)
                continue
            }
            val remHours = h - 0.5
            val hour = floor(remHours).toInt().mod(24)
            val minute = ((remHours - floor(remHours)) * 60.0).toInt().coerceIn(0, 59)
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
            }
            scheduleAt(context, code, cal.timeInMillis, "اقترب موعد صلاة $nameAr", "تبقى ٣٠ دقيقة على أذان $nameAr — بادر بالوضوء والاستعداد.")
        }

        // 2. Morning & Evening Adhkar (codes 301, 302)
        scheduleDailyTime(context, 301, prefs.morningEnabled, prefs.morningTime, "أذكار الصباح", "لا تنسَ قراءة أذكار الصباح لحفظ يومك وبركته.")
        scheduleDailyTime(context, 302, prefs.eveningEnabled, prefs.eveningTime, "أذكار المساء", "حان وقت أذكار المساء — حصّن نفسك بالذكر والدعاء.")
    }

    private fun scheduleDailyTime(context: Context, code: Int, enabled: Boolean, hhmm: String, title: String, body: String) {
        if (!enabled) {
            cancelAt(context, code)
            return
        }
        val parts = hhmm.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: return
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: return
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }
        scheduleAt(context, code, cal.timeInMillis, title, body)
    }
}
