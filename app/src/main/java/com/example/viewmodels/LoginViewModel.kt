package com.example.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diagnostics.AppLogger
import com.example.models.User
import com.example.repository.AdminRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val isResetLoading: Boolean = false,
    val resetMessage: String? = null,
    val resetError: String? = null
)

class LoginViewModel(private val repository: AdminRepository = AdminRepository()) : ViewModel() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    init {
        try {
            val currentUser = try {
                FirebaseAuth.getInstance().currentUser
            } catch (e: Exception) {
                AppLogger.w("LoginViewModel", "FirebaseAuth currentUser check exception: ${e.message}")
                null
            }

            if (currentUser != null) {
                viewModelScope.launch {
                    try {
                        val isAdmin = repository.checkIsAdmin(currentUser)
                        if (isAdmin) {
                            _uiState.value = _uiState.value.copy(isLoggedIn = true)
                        } else {
                            try { auth.signOut() } catch (_: Exception) {}
                            _uiState.value = _uiState.value.copy(
                                isLoggedIn = false,
                                error = "Access Denied: You do not have administrator permissions."
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            isLoggedIn = false,
                            error = "Error verifying admin rights: ${e.message}"
                        )
                    }
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoggedIn = false,
                    error = com.example.AppState.firebaseInitError
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Init: ${com.example.AppState.firebaseInitError ?: e.message}"
            )
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun login() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password.trim()

        if (email.isEmpty() || password.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please fill in all fields")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        try {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        checkAndCreateUserProfile(task.result?.user ?: auth.currentUser)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = task.exception?.message ?: "Login failed"
                        )
                    }
                }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Login Error: ${e.message}"
            )
        }
    }

    fun signInWithGoogleToken(idToken: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    checkAndCreateUserProfile(auth.currentUser)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = task.exception?.message ?: "Google Sign-in failed"
                    )
                }
            }
    }

    fun setGoogleSignInLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isGoogleLoading = isLoading)
    }

    fun setError(error: String) {
        _uiState.value = _uiState.value.copy(error = error, isLoading = false, isGoogleLoading = false)
    }

    private fun checkAndCreateUserProfile(user: FirebaseUser?) {
        if (user == null) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "User is null")
            return
        }

        viewModelScope.launch {
            try {
                // Critical Security Check: Ensure user has admin privileges
                val isAdmin = repository.checkIsAdmin(user)
                if (!isAdmin) {
                    auth.signOut()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = false,
                        error = "Access Denied: You do not have administrator permissions to access CineStream Admin."
                    )
                    return@launch
                }

                // Immediately transition user to authenticated admin state
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    error = null
                )

                // Background sync of user profile and audit logging
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val userRef = firestore.collection("users").document(user.uid)
                    val doc = userRef.get().await()

                    if (doc.exists()) {
                        userRef.update(
                            mapOf(
                                "lastLoginTimestamp" to System.currentTimeMillis(),
                                "lastActiveAt" to System.currentTimeMillis()
                            )
                        ).await()
                    } else {
                        val newUser = User(
                            id = user.uid,
                            username = user.displayName ?: user.email?.substringBefore("@") ?: "Admin",
                            email = user.email ?: "",
                            role = "admin",
                            lastLoginTimestamp = System.currentTimeMillis(),
                            lastActiveAt = System.currentTimeMillis(),
                            createdAt = System.currentTimeMillis()
                        )
                        userRef.set(newUser, SetOptions.merge()).await()
                    }

                    repository.logAudit(
                        action = "ADMIN_LOGIN",
                        targetType = "AUTH",
                        targetId = user.uid,
                        details = "Admin logged in successfully (${user.email})"
                    )
                } catch (e: Exception) {
                    com.example.diagnostics.AppLogger.w("LoginViewModel", "Background sync notice: ${e.message}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Admin Verification Error: ${e.message}"
                )
            }
        }
    }

    fun clearResetMessages() {
        _uiState.value = _uiState.value.copy(resetMessage = null, resetError = null)
    }

    fun resetPassword(email: String) {
        val emailTrimmed = email.trim()
        if (emailTrimmed.isEmpty()) {
            _uiState.value = _uiState.value.copy(resetError = "Please enter your email address")
            return
        }

        _uiState.value = _uiState.value.copy(isResetLoading = true, resetError = null, resetMessage = null)

        try {
            auth.sendPasswordResetEmail(emailTrimmed)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _uiState.value = _uiState.value.copy(
                            isResetLoading = false,
                            resetMessage = "Password reset email sent"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isResetLoading = false,
                            resetError = task.exception?.message ?: "Failed to send reset email"
                        )
                    }
                }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isResetLoading = false,
                resetError = "Reset Error: ${e.message}"
            )
        }
    }
    
    fun logout() {
        auth.signOut()
        _uiState.value = LoginUiState(isLoggedIn = false)
    }
}
