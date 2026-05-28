package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val count = intent.getIntExtra("review_count", 0)

        // Show instant test alert or daily active reviews alert
        val message = if (count > 0) {
            "🌱 今天有 $count 個複習項目在等你喔！快來完成複習，保持記憶曲線吧！"
        } else {
            "🌱 今天也是充電的好日子！別忘了預約或記錄新的複習項目喔！"
        }

        showNotification(context, "綠意複習提醒", message)
    }

    private fun showNotification(context: Context, title: String, content: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "green_recall_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "複習日程提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "每日自動提醒您需要複習的項目"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Open app when notification clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            // Use standard Android notification icon - we can point to a standard drawable
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2001, notification)
    }

    companion object {
        fun triggerInstantNotification(context: Context, reviewCount: Int) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("review_count", reviewCount)
            }
            context.sendBroadcast(intent)
        }
    }
}
