package com.example.reminderapp.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminderapp.R
import com.example.reminderapp.Reminder
import com.example.reminderapp.ReminderCategory
import com.example.reminderapp.ReminderViewModel
import com.example.reminderapp.RepeatMode
import com.example.reminderapp.Screen
import com.example.reminderapp.SortMode
import com.example.reminderapp.customDaysToText
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SortAction(viewModel: ReminderViewModel) {
    var showSortMenu by remember { mutableStateOf(false) }

    IconButton(onClick = { showSortMenu = true }) {
        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(id = R.string.sort))
    }

    DropdownMenu(
        expanded = showSortMenu,
        onDismissRequest = { showSortMenu = false }
    ) {
        DropdownMenuItem(text = { Text(stringResource(id = R.string.sort_by_creation_date_newest)) }, onClick = {
            viewModel.changeSortMode(SortMode.CREATION_DATE_DESC)
            showSortMenu = false
        })
        DropdownMenuItem(text = { Text(stringResource(id = R.string.sort_by_creation_date_oldest)) }, onClick = {
            viewModel.changeSortMode(SortMode.CREATION_DATE_ASC)
            showSortMenu = false
        })
        DropdownMenuItem(text = { Text(stringResource(id = R.string.sort_by_trigger_date_nearest)) }, onClick = {
            viewModel.changeSortMode(SortMode.TRIGGER_DATE_ASC)
            showSortMenu = false
        })
        DropdownMenuItem(text = { Text(stringResource(id = R.string.sort_by_trigger_date_farthest)) }, onClick = {
            viewModel.changeSortMode(SortMode.TRIGGER_DATE_DESC)
            showSortMenu = false
        })
        DropdownMenuItem(text = { Text(stringResource(id = R.string.sort_by_name_asc)) }, onClick = {
            viewModel.changeSortMode(SortMode.NAME_ASC)
            showSortMenu = false
        })
        DropdownMenuItem(text = { Text(stringResource(id = R.string.sort_by_name_desc)) }, onClick = {
            viewModel.changeSortMode(SortMode.NAME_DESC)
            showSortMenu = false
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("NewApi")
@Composable
fun AnnualDateSelectorDialog(
    initialDate: Calendar,
    onDismiss: () -> Unit,
    onDateSelected: (day: Int, month: Int) -> Unit
) {
    val context = LocalContext.current
    val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.resources.configuration.locales[0]
    } else {
        context.resources.configuration.locale
    }
    val months = remember(currentLocale) {
        val format = SimpleDateFormat("LLLL", currentLocale)
        (0..11).map {
            format.format(Calendar.getInstance().apply { set(Calendar.MONTH, it) }.time).replaceFirstChar { it.uppercase() }
        }
    }

    var selectedMonthIndex by remember { mutableIntStateOf(initialDate.get(Calendar.MONTH)) }

    val daysInMonth = remember(selectedMonthIndex) {
        when (selectedMonthIndex) {
            1 -> 29
            3, 5, 8, 10 -> 30
            else -> 31
        }
    }

    var selectedDay by remember {
        mutableIntStateOf(initialDate.get(Calendar.DAY_OF_MONTH).coerceIn(1, daysInMonth))
    }

    var monthExpanded by remember { mutableStateOf(false) }
    var dayExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.select_month_days)) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = monthExpanded,
                    onExpandedChange = { monthExpanded = !monthExpanded },
                    modifier = Modifier.weight(1.5f)
                ) {
                    OutlinedTextField(
                        value = months[selectedMonthIndex],
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = monthExpanded,
                        onDismissRequest = { monthExpanded = false }
                    ) {
                        months.forEachIndexed { index, monthName ->
                            DropdownMenuItem(
                                text = { Text(monthName) },
                                onClick = {
                                    selectedMonthIndex = index
                                    if (selectedDay > when(index){ 1 -> 29; 3,5,8,10 -> 30; else -> 31}) selectedDay = when(index){ 1 -> 29; 3,5,8,10 -> 30; else -> 31}
                                    monthExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = dayExpanded,
                    onExpandedChange = { dayExpanded = !dayExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedDay.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(id = R.string.date)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = dayExpanded,
                        onDismissRequest = { dayExpanded = false }
                    ) {
                        (1..daysInMonth).forEach { day ->
                            DropdownMenuItem(
                                text = { Text(day.toString()) },
                                onClick = {
                                    selectedDay = day
                                    dayExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onDateSelected(selectedDay, selectedMonthIndex) }) { Text(stringResource(id = R.string.ok)) } },
        dismissButton = { Button(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) } }
    )
}

@Composable
fun SelectMonthDaysDialog(selectedDays: List<Int>, onDismiss: () -> Unit, onSave: (List<Int>) -> Unit) {
    val tempSelectedDays = remember { mutableStateListOf<Int>().apply { addAll(selectedDays) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.select_month_days)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 48.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items((1..31).toList()) { day ->
                    val isSelected = tempSelectedDays.contains(day)
                    Card(
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable {
                                if (isSelected) tempSelectedDays.remove(day) else tempSelectedDays.add(
                                    day
                                )
                            },
                        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                            Text(day.toString(), color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(tempSelectedDays.sorted()) }) { Text(stringResource(id = R.string.ok)) } },
        dismissButton = { Button(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) } }
    )
}

@Composable
fun PlaceholderScreen(screen: Screen) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = screen.labelResId),
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppBottomBar(screens: List<Screen>, pagerState: androidx.compose.foundation.pager.PagerState, onScreenSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        screens.forEachIndexed { index, screen ->
            val isSelected = pagerState.currentPage == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onScreenSelected(index) },
                icon = { Icon(screen.icon, contentDescription = stringResource(id = screen.labelResId)) },
                label = { Text(stringResource(id = screen.labelResId), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            )
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun ReminderCard(
    reminder: Reminder,
    onDeleteRequest: () -> Unit,
    onEdit: () -> Unit,
    onToggleEnabled: () -> Unit
) {
    val cardColor = if (reminder.isImportant) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
    } else if (reminder.isEnabled) {
        if (reminder.category == ReminderCategory.BIRTHDAYS) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    }
    val context = LocalContext.current
    val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.resources.configuration.locales[0]
    } else {
        context.resources.configuration.locale
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (reminder.category == ReminderCategory.BIRTHDAYS) Icons.Default.Cake else Icons.Default.Notifications,
                contentDescription = stringResource(id = R.string.category),
                tint = if (reminder.isImportant) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    reminder.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val repeatText = when (reminder.repeatMode) {
                    RepeatMode.ONCE -> stringResource(id = R.string.once) + ": ${SimpleDateFormat("dd.MM.yyyy", currentLocale).format(Date(reminder.timeInMillis))}"
                    RepeatMode.DAILY -> stringResource(id = R.string.daily)
                    RepeatMode.WEEKDAYS -> stringResource(id = R.string.weekdays)
                    RepeatMode.CUSTOM_DAYS -> stringResource(id = R.string.custom_days) + ": ${customDaysToText(context, reminder.repeatDays)}"
                    RepeatMode.MONTHLY -> stringResource(id = R.string.monthly) + ": ${if (reminder.repeatDaysOfMonth.isNotEmpty()) reminder.repeatDaysOfMonth.joinToString() else SimpleDateFormat("dd", currentLocale).format(Date(reminder.timeInMillis))}"
                    RepeatMode.YEARLY -> stringResource(id = R.string.yearly) + ": ${SimpleDateFormat("dd.MM", currentLocale).format(Date(reminder.timeInMillis))}"
                }
                Text(
                    stringResource(id = R.string.repeat) + ": $repeatText",
                    style = MaterialTheme.typography.bodySmall,
                    color = (if (reminder.isImportant) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stringResource(id = R.string.time) + " ${SimpleDateFormat("HH:mm", currentLocale).format(Date(reminder.timeInMillis))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = (if (reminder.isImportant) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = reminder.isEnabled,
                    onCheckedChange = { onToggleEnabled() }
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(id = R.string.edit_reminder))
                    }
                    IconButton(onClick = onDeleteRequest, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(id = R.string.delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
