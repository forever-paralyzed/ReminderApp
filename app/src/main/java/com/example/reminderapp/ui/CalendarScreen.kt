package com.example.reminderapp.ui

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.reminderapp.R
import com.example.reminderapp.Reminder
import com.example.reminderapp.ReminderViewModel
import com.example.reminderapp.occursOnDate
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*
import kotlin.math.min

@Composable
fun CalendarScreen(viewModel: ReminderViewModel, onEditReminder: (Reminder) -> Unit) {
    val reminders: List<Reminder> by viewModel.allReminders.collectAsState(initial = emptyList())

    val currentMonth = YearMonth.now()
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }

    val remindersForSelectedDate = remember(reminders, selectedDate) {
        selectedDate?.let { date ->
            reminders.filter { it.occursOnDate(date) }
        }.orEmpty()
    }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        HorizontalCalendar(
            state = state,
            dayContent = { day ->
                val eventsOnDay = remember(reminders, day.date) {
                    reminders.filter { it.occursOnDate(day.date) }
                }
                Day(
                    day = day,
                    isSelected = selectedDate == day.date,
                    events = eventsOnDay
                ) { clickedDay ->
                    selectedDate = clickedDay.date
                }
            },
            monthHeader = {
                MonthHeader(it.yearMonth)
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(visible = selectedDate != null, enter = fadeIn(), exit = fadeOut()) {
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (remindersForSelectedDate.isNotEmpty()) {
                    items(remindersForSelectedDate) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onDeleteRequest = { viewModel.deleteReminder(reminder) },
                            onEdit = { onEditReminder(reminder) },
                            onToggleEnabled = { viewModel.toggleReminderState(reminder) }
                        )
                    }
                } else {
                    item {
                        Text(
                            text = stringResource(R.string.no_events_for_selected_date),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun MonthHeader(yearMonth: YearMonth) {
    val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        LocalContext.current.resources.configuration.locales[0]
    } else {
        @Suppress("DEPRECATION")
        LocalContext.current.resources.configuration.locale
    }
    val monthName =
        yearMonth.month.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase() }
    val year = yearMonth.year

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$monthName $year",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            val daysOfWeek = remember { DayOfWeek.entries.sortedBy { it.value } }
            for (dayOfWeek in daysOfWeek) {
                Text(
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun Day(
    day: CalendarDay,
    isSelected: Boolean,
    events: List<Reminder>,
    onDateClick: (CalendarDay) -> Unit
) {
    val eventCount = events.size
    
    // Цвет точки теперь не зависит от темы.
    // 1 событие -> Черный (Black)
    // Чем больше событий -> тем краснее (Red)
    val dotColor = if (eventCount > 0) {
        // Считаем, что при 10 событиях достигается максимально красный цвет
        val fraction = ((eventCount - 1) / 9f).coerceIn(0f, 1f)
        lerp(Color.Black, Color.Red, fraction)
    } else {
        Color.Transparent
    }
    
    val dotSize = (4 + min(eventCount, 8)).dp

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    day.date == LocalDate.now() -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else -> Color.Transparent
                }
            )
            .clickable(
                enabled = day.position == DayPosition.MonthDate,
                onClick = { onDateClick(day) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            val textColor = when (day.position) {
                DayPosition.MonthDate -> if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                DayPosition.InDate, DayPosition.OutDate -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            }
            Text(
                text = day.date.dayOfMonth.toString(),
                color = textColor,
                fontWeight = FontWeight.Medium
            )
            if (eventCount > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}
