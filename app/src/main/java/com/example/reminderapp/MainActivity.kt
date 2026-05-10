package com.example.reminderapp

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.example.reminderapp.ui.*
import com.example.reminderapp.ui.auth.AccountScreen
import com.example.reminderapp.ui.theme.ReminderAppTheme
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: ReminderViewModel by viewModels()

    companion object {
        private const val PREFS_NAME = "language_prefs"
        private const val KEY_LANGUAGE = "selected_language"

        fun setLanguage(context: Context, languageCode: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
        }

        fun applySelectedLanguage(context: Context): Context {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val languageCode = prefs.getString(KEY_LANGUAGE, "ru") ?: "ru"
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            val config = context.resources.configuration
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(applySelectedLanguage(newBase))
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification permission not granted", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)
        askForNotificationPermission()
        askForExactAlarmPermission()
        setContent {
            ReminderAppTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {}
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Toast.makeText(this, "Notification permission is required to show reminders", Toast.LENGTH_LONG).show()
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun askForExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent().also { intent ->
                    intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    startActivity(intent)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: ReminderViewModel) {
    val screens = listOf(Screen.Reminders, Screen.Birthdays, Screen.Calendar, Screen.More, Screen.Account)
    val pagerState = rememberPagerState { screens.size }
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }

    val currentScreen = screens[pagerState.currentPage]

    LaunchedEffect(currentScreen) {
        currentScreen.category?.let { viewModel.selectCategory(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = currentScreen.labelResId)) },
                actions = {
                    if (currentScreen == Screen.Reminders || currentScreen == Screen.Birthdays) {
                        SortAction(viewModel)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (currentScreen.category != null) {
                FloatingActionButton(
                    onClick = {
                        editingReminder = null
                        showDialog = true
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Add, stringResource(id = R.string.add_reminder), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            AppBottomBar(screens, pagerState) {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(it)
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding)
        ) { page ->
            when (val screen = screens[page]) {
                is Screen.Reminders, is Screen.Birthdays -> {
                    RemindersListScreen(
                        viewModel = viewModel,
                        onEditReminder = { reminder ->
                            editingReminder = reminder
                            showDialog = true
                        }
                    )
                }
                is Screen.Calendar -> {
                    CalendarScreen(viewModel = viewModel, onEditReminder = { reminder ->
                        editingReminder = reminder
                        showDialog = true
                    })
                }
                is Screen.More -> {
                    SettingsScreen()
                }
                is Screen.Account -> {
                    AccountScreen()
                }
            }
        }

        if (showDialog) {
            ReminderEditDialog(
                reminderToEdit = editingReminder,
                onDismiss = { showDialog = false },
                onSave = { reminder ->
                    viewModel.addOrUpdateReminder(reminder)
                    showDialog = false
                }
            )
        }
    }
}
