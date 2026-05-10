package com.example.reminderapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.reminderapp.R
import com.example.reminderapp.Reminder
import com.example.reminderapp.ReminderCategory
import com.example.reminderapp.ReminderViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RemindersListScreen(viewModel: ReminderViewModel, onEditReminder: (Reminder) -> Unit) {
    val reminders by viewModel.sortedReminders.collectAsState()
    val currentCategory by viewModel.selectedCategory.collectAsState()
    val timeToNextReminderText by viewModel.timeToNextReminderText.collectAsState()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var reminderToDelete by remember { mutableStateOf<Reminder?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp)
    ) {
        if (reminders.any { it.isEnabled } && timeToNextReminderText != null && currentCategory == ReminderCategory.REMINDERS) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Text(
                    text = stringResource(id = R.string.until_next_one) + " $timeToNextReminderText",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
        if (reminders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (currentCategory == ReminderCategory.REMINDERS) stringResource(id = R.string.no_reminders_yet) else stringResource(id = R.string.no_birthdays_yet),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reminders, key = { it.localId }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onDeleteRequest = {
                            reminderToDelete = reminder
                            showDeleteConfirmDialog = true
                        },
                        onEdit = { onEditReminder(reminder) },
                        onToggleEnabled = { viewModel.toggleReminderState(reminder) }
                    )
                }
            }
        }
    }

    if (showDeleteConfirmDialog && reminderToDelete != null) {
        DeleteConfirmationDialog(
            reminderTitle = reminderToDelete!!.title,
            onConfirm = {
                viewModel.deleteReminder(reminderToDelete!!)
                showDeleteConfirmDialog = false
                reminderToDelete = null
            },
            onDismiss = {
                showDeleteConfirmDialog = false
                reminderToDelete = null
            }
        )
    }
}
@Composable
fun DeleteConfirmationDialog(
    reminderTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_confirmation)) },
        text = { Text(stringResource(R.string.delete_reminder_confirmation, reminderTitle)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
