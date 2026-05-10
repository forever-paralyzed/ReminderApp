package com.example.reminderapp.ui.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        checkUserStatus()
    }

    private fun checkUserStatus() {
        val firebaseUser = Firebase.auth.currentUser
        if (firebaseUser != null) {
            _uiState.value = AuthUiState.SignedIn(
                userName = firebaseUser.displayName ?: "Anonymous",
                email = firebaseUser.email,
                photoUrl = firebaseUser.photoUrl?.toString()
            )
        } else {
            _uiState.value = AuthUiState.SignedOut
        }
    }

    fun signInWithGoogle(idToken: String) {
        _uiState.value = AuthUiState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        Firebase.auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = Firebase.auth.currentUser
                    _uiState.value = AuthUiState.SignedIn(
                        userName = user?.displayName ?: "Anonymous",
                        email = user?.email,
                        photoUrl = user?.photoUrl?.toString()
                    )
                } else {
                    _uiState.value = AuthUiState.SignedOut
                }
            }
    }

    fun signOut() {
        Firebase.auth.signOut()
        _uiState.value = AuthUiState.SignedOut
    }
}

sealed interface AuthUiState {
    object Loading : AuthUiState
    object SignedOut : AuthUiState
    data class SignedIn(
        val userName: String,
        val email: String? = null,
        val photoUrl: String? = null
    ) : AuthUiState
}