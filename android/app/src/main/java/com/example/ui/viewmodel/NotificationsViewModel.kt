package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.NotificationEntity
import com.example.data.repository.NotificationRepository
import com.example.data.supabase.RepositoryFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<NotificationEntity> = emptyList()
) {
    val unreadCount: Int get() = notifications.count { !it.read }
}

class NotificationsViewModel @JvmOverloads constructor(
    private val repository: NotificationRepository = RepositoryFactory.createNotificationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private var inboxJob: Job? = null

    init {
        observeInbox(isAdmin = false)
    }

    /**
     * Re-subscribes after sign-in. The Firestore feed is scoped to the current uid, which is
     * null while the login screen is up, so the first subscription would otherwise stay empty.
     *
     * [isAdmin] adds the shared admin inbox. It is passed in rather than discovered here because
     * only an admin may read that collection, and subscribing regardless would log a rejected
     * listen on every patient's device.
     */
    fun refresh(isAdmin: Boolean = false) {
        observeInbox(isAdmin)
    }

    fun clearSession() {
        inboxJob?.cancel()
        inboxJob = null
        _uiState.value = NotificationsUiState()
    }

    private fun observeInbox(isAdmin: Boolean) {
        // Signing in re-subscribes, and the old listener would otherwise keep overwriting the
        // list with the previous account's feed.
        inboxJob?.cancel()
        inboxJob = viewModelScope.launch {
            val admin = if (isAdmin) repository.adminNotifications else flowOf(emptyList())
            combine(repository.myNotifications, admin) { mine, shared ->
                (mine + shared).sortedByDescending { it.createdAtTimestamp }
            }
                .catch { _uiState.value = NotificationsUiState() }
                .collectLatest { list ->
                    _uiState.value = _uiState.value.copy(notifications = list)
                }
        }
    }

    fun markRead(notification: NotificationEntity) {
        viewModelScope.launch { repository.markRead(notification) }
    }

    fun markAllRead() {
        viewModelScope.launch { repository.markAllRead() }
    }
}
