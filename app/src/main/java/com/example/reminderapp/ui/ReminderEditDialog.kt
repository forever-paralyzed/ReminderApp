package com.example.reminderapp.ui
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reminderapp.R
import com.example.reminderapp.Reminder
import com.example.reminderapp.ReminderCategory
import com.example.reminderapp.ReminderViewModel
import com.example.reminderapp.RepeatMode
import com.example.reminderapp.customDaysToText
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("NewApi")
@Composable
fun ReminderEditDialog(
    reminderToEdit: Reminder?,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit
) {
    val viewModel: ReminderViewModel = viewModel()
    val currentCategory by viewModel.selectedCategory.collectAsState()

    var title by remember { mutableStateOf(reminderToEdit?.title ?: "") }
    var isImportant by remember { mutableStateOf(reminderToEdit?.isImportant ?: false) }

    val initialTimeCalendar = Calendar.getInstance().apply {
        if (reminderToEdit != null) timeInMillis = reminderToEdit.timeInMillis
        else { add(Calendar.HOUR_OF_DAY, 1); set(Calendar.MINUTE, 0) }
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    var selectedTimeCalendar by remember { mutableStateOf(initialTimeCalendar) }

    var selectedDateCalendar by remember {
        mutableStateOf(
            if (reminderToEdit != null) Calendar.getInstance().apply { timeInMillis = reminderToEdit.timeInMillis }
            else Calendar.getInstance()
        )
    }

    val isBirthdayMode = reminderToEdit?.category == ReminderCategory.BIRTHDAYS ||
            (reminderToEdit == null && currentCategory == ReminderCategory.BIRTHDAYS)

    var repeatMode by remember {
        mutableStateOf(
            if (isBirthdayMode) RepeatMode.YEARLY
            else reminderToEdit?.repeatMode ?: RepeatMode.ONCE
        )
    }
    var customDays by remember { mutableStateOf(reminderToEdit?.repeatDays ?: emptyList()) }
    var customMonthDays by remember { mutableStateOf(reminderToEdit?.repeatDaysOfMonth ?: emptyList()) }
    var showCustomDaysDialog by remember { mutableStateOf(false) }
    var showCustomMonthDaysDialog by remember { mutableStateOf(false) }
    var showAnnualDateDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current as Activity
    val currentLocale = ConfigurationCompat.getLocales(context.resources.configuration)[0]
    val timeFormatter = remember { SimpleDateFormat("HH:mm", currentLocale) }
    val dateFormatter = remember(repeatMode, currentLocale) {
        when (repeatMode) {
            RepeatMode.YEARLY -> SimpleDateFormat("dd MMMM", currentLocale)
            else -> SimpleDateFormat("dd.MM.yyyy (EEE)", currentLocale)
        }
    }

    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (reminderToEdit == null) stringResource(id = R.string.add_reminder) else stringResource(id = R.string.edit_reminder)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(id = R.string.title)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(id = R.string.trigger_time), style = MaterialTheme.typography.labelMedium)
                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    TimePickerDialog(context, { _, hourOfDay, minute ->
                        selectedTimeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        selectedTimeCalendar.set(Calendar.MINUTE, minute)
                    }, selectedTimeCalendar.get(Calendar.HOUR_OF_DAY), selectedTimeCalendar.get(Calendar.MINUTE), true).show()
                }) { Text(timeFormatter.format(selectedTimeCalendar.time)) }
                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(id = R.string.repeat_mode), style = MaterialTheme.typography.labelMedium)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (!isBirthdayMode) expanded = !expanded }) {
                    OutlinedTextField(
                        value = repeatMode.toRussian(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(id = R.string.repeat)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isBirthdayMode
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        RepeatMode.entries.filterNot { (isBirthdayMode && it != RepeatMode.YEARLY) || it == RepeatMode.WEEKDAYS }.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.toRussian()) },
                                onClick = {
                                    repeatMode = mode
                                    expanded = false
                                    if (mode == RepeatMode.CUSTOM_DAYS) showCustomDaysDialog = true
                                    if (mode == RepeatMode.MONTHLY) showCustomMonthDaysDialog = true
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (repeatMode == RepeatMode.CUSTOM_DAYS && customDays.isNotEmpty()) {
                    Text("${stringResource(R.string.week_days)}: ${customDaysToText(context, customDays)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
                }
                if (repeatMode == RepeatMode.MONTHLY && customMonthDays.isNotEmpty()) {
                    Text("${stringResource(R.string.month_days)}: ${customMonthDays.joinToString()}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
                }

                if (repeatMode == RepeatMode.ONCE) {
                    Text(stringResource(id = R.string.date), style = MaterialTheme.typography.labelMedium)
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        DatePickerDialog(context, { _, year, month, dayOfMonth ->
                            selectedDateCalendar.set(Calendar.YEAR, year)
                            selectedDateCalendar.set(Calendar.MONTH, month)
                            selectedDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        }, selectedDateCalendar.get(Calendar.YEAR), selectedDateCalendar.get(Calendar.MONTH), selectedDateCalendar.get(Calendar.DAY_OF_MONTH)).show()
                    }) { Text(dateFormatter.format(selectedDateCalendar.time)) }
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (repeatMode == RepeatMode.YEARLY) {
                    Text(stringResource(id = R.string.date), style = MaterialTheme.typography.labelMedium)
                    Button(modifier = Modifier.fillMaxWidth(), onClick = { showAnnualDateDialog = true }) {
                         Text(dateFormatter.format(selectedDateCalendar.time))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.very_important), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = isImportant, onCheckedChange = { isImportant = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) {
                    val finalCalendar = Calendar.getInstance()

                    if (repeatMode == RepeatMode.YEARLY) {
                         finalCalendar.set(Calendar.MONTH, selectedDateCalendar.get(Calendar.MONTH))
                         finalCalendar.set(Calendar.DAY_OF_MONTH, selectedDateCalendar.get(Calendar.DAY_OF_MONTH))
                     } else {
                         finalCalendar.time = selectedDateCalendar.time
                     }

                    finalCalendar.set(Calendar.HOUR_OF_DAY, selectedTimeCalendar.get(Calendar.HOUR_OF_DAY))
                    finalCalendar.set(Calendar.MINUTE, selectedTimeCalendar.get(Calendar.MINUTE))
                    finalCalendar.set(Calendar.SECOND, 0)

                    val finalTimeInMillis = finalCalendar.timeInMillis

                    // ИСПРАВЛЕНО: Для нового напоминания ID должен быть 0, чтобы Room сгенерировал его
                    val finalReminder = Reminder(
                        localId = reminderToEdit?.localId ?: 0L,
                        title = title,
                        timeInMillis = finalTimeInMillis,
                        repeatMode = repeatMode,
                        repeatDays = if (repeatMode == RepeatMode.CUSTOM_DAYS) customDays else emptyList(),
                        repeatDaysOfMonth = if (repeatMode == RepeatMode.MONTHLY) customMonthDays else emptyList(),
                        isEnabled = true,
                        isImportant = isImportant,
                        creationTimestamp = reminderToEdit?.creationTimestamp ?: System.currentTimeMillis(),
                        category = if (isBirthdayMode) ReminderCategory.BIRTHDAYS else currentCategory
                    )
                    onSave(finalReminder)
                } else {
                    Toast.makeText(context, context.getString(R.string.enter_reminder_name), Toast.LENGTH_SHORT).show()
                }
            }) { Text(stringResource(id = R.string.save)) }
        },
        dismissButton = { Button(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) } }
    )

    if (showAnnualDateDialog) {
        AnnualDateSelectorDialog(
            initialDate = selectedDateCalendar,
            onDismiss = { showAnnualDateDialog = false },
            onDateSelected = { day, month ->
                selectedDateCalendar.set(Calendar.DAY_OF_MONTH, day)
                selectedDateCalendar.set(Calendar.MONTH, month)
                showAnnualDateDialog = false
            }
        )
    }

    if (showCustomDaysDialog) {
        SelectWeekDaysDialog(
            selectedDays = customDays,
            onDismiss = { showCustomDaysDialog = false },
            onConfirm = { days ->
                customDays = days
                showCustomDaysDialog = false
            }
        )
    }

    if (showCustomMonthDaysDialog) {
        SelectMonthDaysDialog(selectedDays = customMonthDays, onDismiss = { showCustomMonthDaysDialog = false }) { selectedList ->
            customMonthDays = selectedList
            showCustomMonthDaysDialog = false
        }
    }
}

@Composable
fun SelectWeekDaysDialog(
    selectedDays: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit
) {
    val tempSelectedDays = remember { mutableStateListOf<Int>().also { it.addAll(selectedDays) } }
    val daysOfWeek = (1..7).toList() // 1 = Понедельник, ..., 7 = Воскресенье

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_days_of_week)) },
        text = {
            Column {
                daysOfWeek.forEach { day ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = tempSelectedDays.contains(day),
                            onCheckedChange = {
                                if (tempSelectedDays.contains(day)) {
                                    tempSelectedDays.remove(day)
                                } else {
                                    tempSelectedDays.add(day)
                                }
                            }
                        )
                        Text(text = dayOfWeekToString(LocalContext.current, day))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(tempSelectedDays.sorted()) }) {
                Text(stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

// Вспомогательная функция для дней недели
fun dayOfWeekToString(context: android.content.Context, day: Int): String {
    return when (day) {
        1 -> context.getString(R.string.monday)
        2 -> context.getString(R.string.tuesday)
        3 -> context.getString(R.string.wednesday)
        4 -> context.getString(R.string.thursday)
        5 -> context.getString(R.string.friday)
        6 -> context.getString(R.string.saturday)
        7 -> context.getString(R.string.sunday)
        else -> ""
    }
}

@Composable
fun RepeatMode.toRussian(): String {
    return when (this) {
        RepeatMode.ONCE -> stringResource(R.string.once)
        RepeatMode.DAILY -> stringResource(R.string.daily)
        RepeatMode.WEEKDAYS -> stringResource(R.string.weekdays)
        RepeatMode.MONTHLY -> stringResource(R.string.monthly)
        RepeatMode.YEARLY -> stringResource(R.string.yearly)
        RepeatMode.CUSTOM_DAYS -> stringResource(R.string.custom_days)
    }
}
