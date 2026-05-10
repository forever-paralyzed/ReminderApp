package com.example.reminderapp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.firebase.firestore.Exclude
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0, // Локальный ID для Room
    
    var firestoreId: String = "", // ID из Firestore, будет пустым для локальных
    
    var title: String = "",
    var timeInMillis: Long = 0L,
    var repeatMode: RepeatMode = RepeatMode.ONCE,
    var repeatDays: List<Int> = emptyList(),
    var repeatDaysOfMonth: List<Int> = emptyList(),
    var isEnabled: Boolean = true,
    var isImportant: Boolean = false,
    var creationTimestamp: Long = System.currentTimeMillis(),
    var category: ReminderCategory = ReminderCategory.REMINDERS,
    var audioPath: String? = null
) {
    @Exclude
    fun getUniqueId(): String = if (firestoreId.isNotEmpty()) firestoreId else localId.toString()
}


enum class ReminderCategory {
    REMINDERS,
    BIRTHDAYS
}

enum class RepeatMode {
    ONCE, DAILY, WEEKDAYS, CUSTOM_DAYS, MONTHLY, YEARLY
}

enum class SortMode {
    CREATION_DATE_DESC,
    CREATION_DATE_ASC,
    TRIGGER_DATE_ASC,
    TRIGGER_DATE_DESC,
    NAME_ASC,
    NAME_DESC
}

sealed class Screen(val route: String, val labelResId: Int, val icon: ImageVector, val category: ReminderCategory?) {
    object Reminders : Screen("reminders", R.string.reminders, Icons.Default.Notifications, ReminderCategory.REMINDERS)
    object Birthdays : Screen("birthdays", R.string.birthdays, Icons.Default.Cake, ReminderCategory.BIRTHDAYS)
    object Calendar : Screen("calendar", R.string.calendar, Icons.Default.CalendarToday, null)
    object More : Screen("more", R.string.more, Icons.Default.MoreVert, null)
    object Account : Screen("account", R.string.account, Icons.Default.AccountCircle, null)
}

class Converters {
    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        val listType = object : TypeToken<List<Int>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromRepeatMode(value: RepeatMode): String {
        return value.name
    }

    @TypeConverter
    fun toRepeatMode(value: String): RepeatMode {
        return RepeatMode.valueOf(value)
    }

    @TypeConverter
    fun fromReminderCategory(value: ReminderCategory): String {
        return value.name
    }

    @TypeConverter
    fun toReminderCategory(value: String): ReminderCategory {
        return ReminderCategory.valueOf(value)
    }
}
