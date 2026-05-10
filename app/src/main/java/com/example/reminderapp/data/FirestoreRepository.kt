package com.example.reminderapp.data

import android.util.Log
import com.example.reminderapp.Reminder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private fun getRemindersCollection() = 
        auth.currentUser?.let { user ->
            db.collection("users").document(user.uid).collection("reminders")
        }

    // Разовый запрос всех напоминаний из облака (надежнее для Restore/Backup)
    suspend fun getRemindersOnce(): List<Reminder> {
        return try {
            val collection = getRemindersCollection() ?: return emptyList()
            val snapshot = collection.get().await()
            snapshot.documents.mapNotNull { it.toObject(Reminder::class.java) }
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error getting reminders: ${e.message}")
            emptyList()
        }
    }

    suspend fun createReminder(reminder: Reminder): String {
        val collection = getRemindersCollection()
        val documentRef = collection?.add(reminder)?.await()
        val firestoreId = documentRef?.id ?: ""
        collection?.document(firestoreId)?.update("firestoreId", firestoreId)?.await()
        return firestoreId
    }

    suspend fun updateReminder(reminder: Reminder) {
        if (reminder.firestoreId.isEmpty()) return
        getRemindersCollection()?.document(reminder.firestoreId)?.set(reminder)?.await()
    }

    suspend fun deleteReminder(firestoreId: String) {
        if (firestoreId.isEmpty()) return
        getRemindersCollection()?.document(firestoreId)?.delete()?.await()
    }

    fun getRemindersFlow(): Flow<List<Reminder>> = callbackFlow {
        val collection = getRemindersCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val reminders = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Reminder::class.java)
                }
                trySend(reminders)
            }
        }
        awaitClose { listener.remove() }
    }
}
