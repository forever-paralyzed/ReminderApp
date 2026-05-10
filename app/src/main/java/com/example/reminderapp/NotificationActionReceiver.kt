package com.example.reminderapp

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.reminderapp.data.FirestoreRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SNOOZE = "com.example.reminderapp.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.example.reminderapp.ACTION_DISMISS"
        const val EXTRA_REMINDER_ID = "REMINDER_ID" // Соответствует ключу в AlarmActivity и AlarmReceiver
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        
        Log.d("ActionReceiver", "Received action: ${intent.action} for ID: $reminderId")

        if (reminderId == -1L) {
            Log.e("ActionReceiver", "No valid ID found in intent")
            pendingResult.finish()
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId.toInt())

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val reminderDao = db.reminderDao()
                val reminder = reminderDao.getById(reminderId)

                if (reminder != null) {
                    when (intent.action) {
                        ACTION_SNOOZE -> {
                            val calendar = Calendar.getInstance()
                            calendar.add(Calendar.MINUTE, 30)
                            val newTime = calendar.timeInMillis

                            val updatedReminder = reminder.copy(timeInMillis = newTime, isEnabled = true)
                            reminderDao.update(updatedReminder)
                            
                            if (Firebase.auth.currentUser != null) {
                                try {
                                    FirestoreRepository().updateReminder(updatedReminder)
                                } catch (e: Exception) {
                                    Log.e("ActionReceiver", "Firestore update failed: ${e.message}")
                                }
                            }

                            ReminderScheduler(context).schedule(updatedReminder)
                            Log.d("ActionReceiver", "Reminder $reminderId snoozed +30m")
                        }
                        ACTION_DISMISS -> {
                            // Выключаем напоминание в базе данных
                            val updatedReminder = reminder.copy(isEnabled = false)
                            reminderDao.update(updatedReminder)
                            
                            if (Firebase.auth.currentUser != null) {
                                try {
                                    FirestoreRepository().updateReminder(updatedReminder)
                                } catch (e: Exception) {
                                    Log.e("ActionReceiver", "Firestore update failed: ${e.message}")
                                }
                            }
                            
                            ReminderScheduler(context).cancel(updatedReminder)
                            Log.d("ActionReceiver", "Reminder $reminderId disabled")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ActionReceiver", "Error processing action: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
