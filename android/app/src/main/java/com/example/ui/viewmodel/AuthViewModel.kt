package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthResult
import com.example.data.auth.emailProblem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val emailLinkSentTo: String? = null,
    val pendingEmailLink: String? = null,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val uid: String = "",
    val email: String = "",
    val role: UserRole = UserRole.PATIENT,
    val providerId: String = "",
    val displayName: String = "",
    val savedAddress: String = "",
    val isGuest: Boolean = false
)

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        val user = repository.currentUser
        if (user != null) {
            _uiState.value = _uiState.value.copy(
                isAuthenticated = true,
                uid = user.uid,
                email = user.email ?: ""
            )
            loadRole()
        }
    }

    fun sendEmailLink(email: String, context: Context) {
        val problem = emailProblem(email)
        if (problem != null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = problem
            )
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, email = email.trim())
        viewModelScope.launch {
            when (val result = repository.sendEmailLink(email, context)) {
                is AuthResult.EmailLinkSent -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    emailLinkSentTo = result.email
                )
                is AuthResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                else -> Unit
            }
        }
    }

    fun signInWithPassword(email: String, password: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch { finish(repository.signInWithPassword(email, password)) }
    }

    fun signUpWithPassword(email: String, password: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch { finish(repository.signUpWithPassword(email, password)) }
    }

    fun handleEmailLink(link: String, context: Context) {
        if (!repository.isEmailSignInLink(link)) return
        val savedEmail = repository.pendingEmail(context)
        if (savedEmail.isBlank()) {
            _uiState.value = _uiState.value.copy(pendingEmailLink = link, errorMessage = null)
            return
        }
        completeEmailLink(savedEmail, link, context)
    }

    fun completePendingEmailLink(email: String, context: Context) {
        val link = _uiState.value.pendingEmailLink ?: return
        val problem = emailProblem(email)
        if (problem != null) {
            _uiState.value = _uiState.value.copy(errorMessage = problem)
            return
        }
        completeEmailLink(email, link, context)
    }

    fun changeEmail() {
        _uiState.value = _uiState.value.copy(emailLinkSentTo = null, errorMessage = null)
    }

    private fun completeEmailLink(email: String, link: String, context: Context) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = repository.completeEmailLink(email, link, context)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        pendingEmailLink = null,
                        emailLinkSentTo = null,
                        email = result.user.email ?: email
                    )
                    loadRole()
                }
                is AuthResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                else -> Unit
            }
        }
    }

    fun signInAsGuest() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = repository.signInAnonymously()) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true
                    )
                    loadRole()
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                else -> {}
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch { finish(repository.signInWithGoogle(context)) }
    }

    private fun finish(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    email = result.user.email ?: _uiState.value.email
                )
                loadRole()
            }
            is AuthResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
            // Dismissing the account chooser is a choice, not a failure.
            AuthResult.Cancelled -> {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            else -> {}
        }
    }

    private fun loadRole() {
        val user = repository.currentUser ?: return
        viewModelScope.launch {
            val role = when (repository.fetchUserRole(user.uid)) {
                "PROVIDER" -> UserRole.PROVIDER
                "ADMIN" -> UserRole.ADMIN
                else -> UserRole.PATIENT
            }
            val (name, address) = repository.fetchUserProfile(user.uid)
            val providerId =
                if (role == UserRole.PROVIDER) repository.fetchProviderId(user.uid) else ""
            _uiState.value = _uiState.value.copy(
                uid = user.uid,
                role = role,
                providerId = providerId,
                displayName = name,
                savedAddress = address,
                isGuest = user.isAnonymous
            )
        }
    }

    fun saveProfile(name: String, address: String) {
        val uid = repository.currentUser?.uid ?: return
        _uiState.value = _uiState.value.copy(displayName = name, savedAddress = address)
        viewModelScope.launch {
            try {
                repository.updateUserProfile(uid, name, address)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "تعذر حفظ البيانات")
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _uiState.value = AuthUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
