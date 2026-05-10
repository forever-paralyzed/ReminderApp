package com.example.reminderapp

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.RingtoneManager
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm received!")
        
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "ReminderApp:AlarmWakeLock"
        )
        wakeLock.acquire(10 * 1000L)

        // Получаем контекст с правильным языком
        val localizedContext = getLocalizedContext(context)
        updateNotificationChannel(localizedContext)

        val reminderId = intent.getLongExtra("REMINDER_ID", -1L)
        val reminderTitle = intent.getStringExtra("REMINDER_TITLE") ?: localizedContext.getString(R.string.reminders)
        val reminderJson = intent.getStringExtra("REMINDER_JSON")

        if (reminderId == -1L) return

        val fullScreenIntent = Intent(localizedContext, AlarmActivity::class.java).apply {
            putExtra("REMINDER_ID", reminderId)
            putExtra("REMINDER_TITLE", reminderTitle)
            putExtra("REMINDER_JSON", reminderJson)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            localizedContext,
            reminderId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Кнопка "+30 m"
        val snoozeIntent = Intent(localizedContext, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            localizedContext,
            reminderId.toInt() + 1000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Кнопка "Выключить"
        val dismissIntent = Intent(localizedContext, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            localizedContext,
            reminderId.toInt() + 2000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = localizedContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val channelId = getChannelId(localizedContext)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val builder = NotificationCompat.Builder(localizedContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(reminderTitle)
            .setContentText(localizedContext.getString(R.string.time_is_up))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmSound) // ПРИНУДИТЕЛЬНЫЙ ЗВУК
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000)) // ПРИНУДИТЕЛЬНАЯ ВИБРАЦИЯ
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(0, "+30 m", snoozePendingIntent)
            .addAction(0, localizedContext.getString(R.string.dismiss_alarm), dismissPendingIntent)

        notificationManager?.notify(reminderId.toInt(), builder.build())
    }

    private fun getLocalizedContext(context: Context): Context {
        val prefs = context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("selected_language", "ru") ?: "ru"
        val locale = when (lang) {
            "en" -> Locale.ENGLISH
            "pt" -> Locale("pt")
            else -> Locale("ru")
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
