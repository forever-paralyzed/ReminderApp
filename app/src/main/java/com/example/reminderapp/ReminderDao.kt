package com.example.reminderapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders")
    fun getAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders")
    suspend fun getAllReminders(): List<Reminder>

    // Находим по локальному ID (теперь он Long)
    @Query("SELECT * FROM reminders WHERE localId = :localId")
    suspend fun getById(localId: Long): Reminder?

    // Находим по firestoreId
    @Query("SELECT * FROM reminders WHERE firestoreId = :firestoreId")
    suspend fun getByFirestoreId(firestoreId: String): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long // Возвращает ID вставленной записи

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<Reminder>)

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
