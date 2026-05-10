package com.example.reminderapp

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit

class AlarmActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(getLocalizedContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val title = intent.getStringExtra("REMINDER_TITLE") ?: getString(R.string.reminders)
        val reminderIdLong = intent.getLongExtra("REMINDER_ID", -1L)
        val reminderId = if (reminderIdLong == -1L) intent.getIntExtra("REMINDER_ID", -1).toLong() else reminderIdLong

        val reminderJson = intent.getStringExtra("REMINDER_JSON")
        val reminder = if (reminderJson != null) Gson().fromJson(reminderJson, Reminder::class.java) else null
        val audioPath = reminder?.audioPath
        val triggerTime = reminder?.timeInMillis ?: System.currentTimeMillis()

        playAlarmSound(audioPath)

        setContent {
            AlarmScreen(
                title = title,
                triggerTime = triggerTime,
                onSnooze = {
                    stopAlarmSound()
                    if (reminderId != -1L) {
                        val snoozeIntent = Intent(this, NotificationActionReceiver::class.java).apply {
                            action = NotificationActionReceiver.ACTION_SNOOZE
                            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
                        }
                        sendBroadcast(snoozeIntent)
                    }
                    finish()
                },
                onDismiss = {
                    stopAlarmSound()
                    if (reminderId != -1L) {
                        val dismissIntent = Intent(this, NotificationActionReceiver::class.java).apply {
                            action = NotificationActionReceiver.ACTION_DISMISS
                            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
                        }
                        sendBroadcast(dismissIntent)
                    }
                    finish()
                }
            )
        }
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

    private fun playAlarmSound(path: String?) {
        try {
            val soundUri: Uri = if (!path.isNullOrEmpty()) Uri.parse(path) else getRingtone(applicationContext)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            try {
                val defaultUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, defaultUri)
                    prepare()
                    start()
                }
            } catch (ex: Exception) { ex.printStackTrace() }
        }
    }

    private fun stopAlarmSound() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
    }
}

@Composable
fun AlarmScreen(title: String, triggerTime: Long, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    var remainingTime by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(key1 = triggerTime) {
        while (true) {
            val now = System.currentTimeMillis()
            val diff = triggerTime - now
            if (diff > 0) {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
                remainingTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                remainingTime = "00:00:00"
            }
            delay(1000)
        }
    }

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (remainingTime.isNotEmpty() && remainingTime != "00:00:00") {
                    Text(text = remainingTime, fontSize = 48.sp, style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.height(24.dp))
                }
                // ИСПОЛЬЗУЕМ РЕСУРСЫ ДЛЯ ЛОКАЛИЗАЦИИ
                Text(text = context.getString(R.string.time), fontSize = 24.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = title, fontSize = 32.sp, style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.height(48.dp))
                Button(onClick = onSnooze, modifier = Modifier.width(240.dp).height(60.dp)) {
                    Text("+30 m", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.width(240.dp).height(60.dp)) {
                    Text(context.getString(R.string.dismiss_alarm), fontSize = 20.sp)
                }
            }
        }
    }
}
