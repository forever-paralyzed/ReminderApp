package com.example.reminderapp

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.reminderapp.data.FirestoreRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val reminderDao = AppDatabase.getDatabase(application).reminderDao()
    private val scheduler = ReminderScheduler(application)
    private val firestoreRepository = FirestoreRepository()

    private val _sortMode = MutableStateFlow(SortMode.CREATION_DATE_DESC)
    val sortMode: StateFlow<SortMode> = _sortMode

    private val _selectedCategory = MutableStateFlow(ReminderCategory.REMINDERS)
    val selectedCategory: StateFlow<ReminderCategory> = _selectedCategory

    private val _reminders = reminderDao.getAll()

    val allReminders: StateFlow<List<Reminder>> = _reminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sortedReminders: StateFlow<List<Reminder>> = combine(
        _reminders,
        _sortMode,
        _selectedCategory
    ) { reminders, mode, category ->
        val filtered = reminders.filter { it.category == category }
        sortReminders(filtered, mode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }

    val timeToNextReminderText: StateFlow<String?> = combine(sortedReminders, ticker) { reminders, currentTime ->
        val nextReminder = reminders
            .filter { it.isEnabled && it.timeInMillis > currentTime }
            .minByOrNull { it.timeInMillis }

        if (nextReminder != null) {
            formatTimeDiff(nextReminder.timeInMillis - currentTime)
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private fun formatTimeDiff(diffInMillis: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis) % 60
        
        val res = getApplication<Application>().resources
        return buildString {
            if (days > 0) append("$days ${res.getString(R.string.unit_day)} ")
            if (hours > 0 || days > 0) append("$hours ${res.getString(R.string.unit_hour)} ")
            if (minutes > 0 || hours > 0 || days > 0) append("$minutes ${res.getString(R.string.unit_min)} ")
            append("$seconds ${res.getString(R.string.unit_sec)}")
        }
    }

    init {
        Firebase.auth.addAuthStateListener { auth ->
            if (auth.currentUser != null) {
                viewModelScope.launch {
                    delay(3000)
                    val localReminders = reminderDao.getAllReminders()
                    if (localReminders.isEmpty()) {
                        Log.d("ViewModel", "Auto-sync: Phone empty, restoring from cloud...")
                        val cloudData = firestoreRepository.getRemindersOnce()
                        if (cloudData.isNotEmpty()) {
                            reminderDao.insertAll(cloudData)
                            cloudData.forEach { if (it.isEnabled) scheduler.schedule(it) }
                        }
                    }
                }
            } else {
                viewModelScope.launch {
                    val allLocal = reminderDao.getAllReminders()
                    allLocal.forEach { scheduler.cancel(it) }
                    reminderDao.deleteAll()
                }
            }
        }
    }

    // ПОЛНАЯ СИНХРОНИЗАЦИЯ: Облако станет точной копией телефона
    fun syncAccount() = viewModelScope.launch {
        if (Firebase.auth.currentUser == null) return@launch
        Toast.makeText(getApplication(), "Syncing...", Toast.LENGTH_SHORT).show()
        
        try {
            var localData = reminderDao.getAllReminders()
            val cloudData = firestoreRepository.getRemindersOnce()

            if (localData.isEmpty() && cloudData.isNotEmpty()) {
                // Если телефон пустой (переустановка) - скачиваем всё
                reminderDao.insertAll(cloudData)
                cloudData.forEach { if (it.isEnabled) scheduler.schedule(it) }
                Toast.makeText(getApplication(), "Restored!", Toast.LENGTH_SHORT).show()
            } else {
                // Синхронизируем: Телефон -> Облако
                for (reminder in localData) {
                    if (reminder.firestoreId.isEmpty()) {
                        val fId = firestoreRepository.createReminder(reminder)
                        reminderDao.update(reminder.copy(firestoreId = fId))
                    } else {
                        firestoreRepository.updateReminder(reminder)
                    }
                }
                
                localData = reminderDao.getAllReminders()
                val localFIds = localData.mapNotNull { it.firestoreId.ifEmpty { null } }.toSet()
                
                // Удаляем из облака то, чего нет на телефоне
                for (cloudItem in cloudData) {
                    if (cloudItem.firestoreId !in localFIds) {
                        firestoreRepository.deleteReminder(cloudItem.firestoreId)
                    }
                }
                
                // Докачиваем новое из облака (если есть)
                val newFromCloud = cloudData.filter { it.firestoreId !in localFIds }
                if (newFromCloud.isNotEmpty()) {
                    reminderDao.insertAll(newFromCloud)
                    newFromCloud.forEach { if (it.isEnabled) scheduler.schedule(it) }
                }
                
                Toast.makeText(getApplication(), "Synced!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) { Log.e("ViewModel", "Sync failed: ${e.message}") }
    }

    fun addOrUpdateReminder(reminder: Reminder) = viewModelScope.launch {
        val id = if (reminder.localId == 0L) reminderDao.insert(reminder) else {
            reminderDao.update(reminder)
            reminder.localId
        }
        val updated = reminder.copy(localId = id)
        scheduleIfNeeded(updated)
        
        if (Firebase.auth.currentUser != null) {
            launch {
                try {
                    if (updated.firestoreId.isEmpty()) {
                        val fId = firestoreRepository.createReminder(updated)
                        reminderDao.update(updated.copy(firestoreId = fId))
                    } else {
                        firestoreRepository.updateReminder(updated)
                    }
                } catch (e: Exception) { Log.e("ViewModel", "Bg sync fail") }
            }
        }
    }

    fun deleteReminder(reminder: Reminder) = viewModelScope.launch {
        scheduler.cancel(reminder)
        reminderDao.delete(reminder)
        if (Firebase.auth.currentUser != null && reminder.firestoreId.isNotEmpty()) {
            launch { try { firestoreRepository.deleteReminder(reminder.firestoreId) } catch (e: Exception) {} }
        }
    }

    fun toggleReminderState(reminder: Reminder) = viewModelScope.launch {
        val updatedReminder = reminder.copy(isEnabled = !reminder.isEnabled)
        addOrUpdateReminder(updatedReminder)
    }

    private fun scheduleIfNeeded(reminder: Reminder) {
        val currentTime = System.currentTimeMillis()
        if (reminder.isEnabled && (reminder.timeInMillis > currentTime || reminder.timeInMillis > currentTime - 60000)) {
            scheduler.schedule(reminder)
        } else {
            scheduler.cancel(reminder)
        }
    }

    fun changeSortMode(newMode: SortMode) { _sortMode.value = newMode }
    fun selectCategory(category: ReminderCategory) { _selectedCategory.value = category }

    private fun sortReminders(list: List<Reminder>, mode: SortMode): List<Reminder> {
        return when (mode) {
            SortMode.CREATION_DATE_DESC -> list.sortedByDescending { it.creationTimestamp }
            SortMode.CREATION_DATE_ASC -> list.sortedBy { it.creationTimestamp }
            SortMode.TRIGGER_DATE_ASC -> list.sortedBy { it.timeInMillis }
            SortMode.TRIGGER_DATE_DESC -> list.sortedByDescending { it.timeInMillis }
            SortMode.NAME_ASC -> list.sortedBy { it.title.lowercase() }
            SortMode.NAME_DESC -> list.sortedByDescending { it.title.lowercase() }
        }
    }
}
