package com.example.reminderapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import java.util.Calendar
import java.util.Date

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: Reminder): Long {
        if (!reminder.isEnabled) return -1L

        var triggerTime = reminder.timeInMillis
        val currentTime = System.currentTimeMillis()

        // Если время напоминания в прошлом более чем на 1 минуту, рассчитываем следующее срабатывание
        // Если задержка меньше минуты, даем сработать сейчас (важно при синхронизации)
        if (triggerTime < currentTime - 60000) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = triggerTime

            when (reminder.repeatMode) {
                RepeatMode.DAILY -> {
                    while (calendar.timeInMillis <= currentTime) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                RepeatMode.WEEKDAYS -> {
                    while (calendar.timeInMillis <= currentTime || calendar.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                RepeatMode.YEARLY -> {
                    while (calendar.timeInMillis <= currentTime) {
                        calendar.add(Calendar.YEAR, 1)
                    }
                }
                RepeatMode.MONTHLY -> {
                    while (calendar.timeInMillis <= currentTime) {
                        calendar.add(Calendar.MONTH, 1)
                    }
                }
                RepeatMode.CUSTOM_DAYS -> {
                    if (reminder.repeatDays.isNotEmpty()) {
                        while (calendar.timeInMillis <= currentTime || !reminder.repeatDays.contains(calendar.get(Calendar.DAY_OF_WEEK))) {
                            calendar.add(Calendar.DAY_OF_YEAR, 1)
                        }
                    } else {
                        while (calendar.timeInMillis <= currentTime) {
                            calendar.add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }
                }
                // Для RepeatMode.ONCE (один раз) не переносим на завтра, если время прошло давно, 
                // просто не планируем (это логично для пропущенного напоминания)
                else -> {
                    if (triggerTime < currentTime) return -1L
                }
            }
            triggerTime = calendar.timeInMillis
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("REMINDER_ID", reminder.localId)
            putExtra("REMINDER_TITLE", reminder.title)
            putExtra("REMINDER_JSON", Gson().toJson(reminder.copy(timeInMillis = triggerTime)))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.localId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val info = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
        alarmManager.setAlarmClock(info, pendingIntent)

        Log.d("ReminderScheduler", "Scheduled: ${reminder.title} (ID: ${reminder.localId}) at ${Date(triggerTime)}")
        return triggerTime
    }

    fun cancel(reminder: Reminder) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.localId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("ReminderScheduler", "Cancelled: ${reminder.localId}")
    }
}
