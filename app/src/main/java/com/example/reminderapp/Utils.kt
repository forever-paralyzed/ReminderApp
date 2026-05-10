package com.example.reminderapp

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

private const val RINGTONE_PREFS_NAME = "ringtone_prefs"
private const val KEY_RINGTONE_URI = "selected_ringtone_uri"

fun saveRingtone(context: Context, uri: Uri) {
    val prefs = context.getSharedPreferences(RINGTONE_PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_RINGTONE_URI, uri.toString()).apply()
    updateNotificationChannel(context)
}

fun getRingtone(context: Context): Uri {
    val prefs = context.getSharedPreferences(RINGTONE_PREFS_NAME, Context.MODE_PRIVATE)
    val uriString = prefs.getString(KEY_RINGTONE_URI, null)
    return uriString?.let { Uri.parse(it) } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
}

fun getChannelId(context: Context): String {
    val ringtoneUri = getRingtone(context)
    // Используем v3 для принудительного обновления настроек звука в системе
    return "reminder_alarm_channel_v3_${ringtoneUri.toString().hashCode()}"
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelId = getChannelId(context)
        val name = context.getString(R.string.channel_name)
        val importance = NotificationManager.IMPORTANCE_HIGH
        
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = context.getString(R.string.channel_description)
            enableVibration(true)
            enableLights(true)
            // Устанавливаем звук на уровне канала (критично для Android 8+)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(getRingtone(context), audioAttributes)
        }
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Удаляем старые каналы, чтобы настройки не конфликтовали
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.notificationChannels.forEach {
                if (it.id.startsWith("reminder_alarm_channel_")) {
                    notificationManager.deleteNotificationChannel(it.id)
                }
            }
        }

        notificationManager.createNotificationChannel(channel)
    }
}

fun updateNotificationChannel(context: Context) {
    createNotificationChannel(context)
}

@SuppressLint("NewApi")
fun customDaysToText(context: Context, days: List<Int>): String {
    if (days.isEmpty()) return context.getString(R.string.not_selected)
    val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.resources.configuration.locales[0]
    } else {
        context.resources.configuration.locale
    }
    val calendar = Calendar.getInstance()
    // Сортируем так, чтобы неделя начиналась с понедельника (Пн=2, ..., Вс=1)
    val dayOrder = listOf(2, 3, 4, 5, 6, 7, 1)
    return days.sortedBy { dayOrder.indexOf(it) }.joinToString(", ") { dayConstant ->
        calendar.set(Calendar.DAY_OF_WEEK, dayConstant)
        SimpleDateFormat("EEE", currentLocale).format(calendar.time)
    }
}

@SuppressLint("NewApi")
fun Reminder.occursOnDate(date: LocalDate): Boolean {
    val startCal = Calendar.getInstance().apply { timeInMillis = this@occursOnDate.timeInMillis }
    val startDate = LocalDate.of(
        startCal.get(Calendar.YEAR),
        startCal.get(Calendar.MONTH) + 1,
        startCal.get(Calendar.DAY_OF_MONTH)
    )

    if (date.isBefore(startDate)) return false

    return when (repeatMode) {
        RepeatMode.ONCE -> date == startDate
        RepeatMode.DAILY -> true
        RepeatMode.WEEKDAYS -> {
            val dow = date.dayOfWeek
            dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY
        }
        RepeatMode.CUSTOM_DAYS -> {
            val calendarDay = when (date.dayOfWeek) {
                java.time.DayOfWeek.SUNDAY -> Calendar.SUNDAY
                java.time.DayOfWeek.MONDAY -> Calendar.MONDAY
                java.time.DayOfWeek.TUESDAY -> Calendar.TUESDAY
                java.time.DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
                java.time.DayOfWeek.THURSDAY -> Calendar.THURSDAY
                java.time.DayOfWeek.FRIDAY -> Calendar.FRIDAY
                java.time.DayOfWeek.SATURDAY -> Calendar.SATURDAY
            }
            repeatDays.contains(calendarDay)
        }
        RepeatMode.MONTHLY -> {
            if (repeatDaysOfMonth.isNotEmpty()) {
                repeatDaysOfMonth.contains(date.dayOfMonth)
            } else {
                date.dayOfMonth == startDate.dayOfMonth
            }
        }
        RepeatMode.YEARLY -> {
            date.monthValue == startDate.monthValue && date.dayOfMonth == startDate.dayOfMonth
        }
    }
}
