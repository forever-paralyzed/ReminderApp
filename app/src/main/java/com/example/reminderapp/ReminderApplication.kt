
package com.example.reminderapp

import android.app.Application

class ReminderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LanguageManager.applyLanguage(this)
    }
}
