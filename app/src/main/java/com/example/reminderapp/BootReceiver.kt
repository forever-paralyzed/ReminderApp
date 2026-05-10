package com.example.reminderapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                val db = AppDatabase.getDatabase(context)
                val reminders = db.reminderDao().getAllReminders()
                val scheduler = ReminderScheduler(context)
                
                reminders.forEach { reminder ->
                    if (reminder.isEnabled) {
                        scheduler.schedule(reminder)
                    }
                }
            }
        }
    }
}
