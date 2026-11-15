package com.nooralislam.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nooralislam.app.R

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "تنبيه نور الإسلام"
        val body = intent.getStringExtra("body") ?: "حان موعد الذكر أو الصلاة"
        showNotification(context, title, body, intent.getIntExtra("id", System.currentTimeMillis().toInt()))
    }

    companion object {
        fun showNotification(context: Context, title: String, body: String, notifId: Int = 1001) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "noor_islam_reminders"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ch = NotificationChannel(channelId, "مواقيت الصلاة والأذكار", NotificationManager.IMPORTANCE_HIGH)
                nm.createNotificationChannel(ch)
            }
            val notif = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            nm.notify(notifId, notif)
        }
    }
}
