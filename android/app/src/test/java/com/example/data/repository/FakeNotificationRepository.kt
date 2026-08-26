package com.example.data.repository

import com.example.data.model.NotificationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [NotificationRepository] for unit tests, keyed by recipient uid. */
class FakeNotificationRepository : NotificationRepository {

    private val inboxes = mutableMapOf<String, MutableList<NotificationEntity>>()
    private val myInbox = MutableStateFlow<List<NotificationEntity>>(emptyList())
    private val adminInbox = MutableStateFlow<List<NotificationEntity>>(emptyList())

    /** Simulates the signed-in user, so [myNotifications] mirrors that uid's inbox. */
    var signedInUid: String = ""

    override val myNotifications: Flow<List<NotificationEntity>> = myInbox
    override val adminNotifications: Flow<List<NotificationEntity>> = adminInbox

    fun inboxOf(uid: String): List<NotificationEntity> = inboxes[uid].orEmpty()

    /** The shared admin inbox, which has no uid of its own. */
    fun adminInbox(): List<NotificationEntity> = adminInbox.value

    override suspend fun push(uid: String, notification: NotificationEntity) {
        if (uid.isEmpty()) return
        val stored = notification.copy(id = nextId(), scope = NotificationEntity.SCOPE_USER)
        inboxes.getOrPut(uid) { mutableListOf() }.add(stored)
        if (uid == signedInUid) myInbox.value = inboxes.getValue(uid).toList()
    }

    override suspend fun pushToAdmins(notification: NotificationEntity) {
        adminInbox.value = adminInbox.value +
            notification.copy(id = nextId(), scope = NotificationEntity.SCOPE_ADMIN)
    }

    override suspend fun markRead(notification: NotificationEntity) {
        if (notification.scope == NotificationEntity.SCOPE_ADMIN) {
            adminInbox.value = adminInbox.value.map {
                if (it.id == notification.id) it.copy(read = true) else it
            }
            return
        }
        val inbox = inboxes[signedInUid] ?: return
        inbox.replaceAll { if (it.id == notification.id) it.copy(read = true) else it }
        myInbox.value = inbox.toList()
    }

    override suspend fun markAllRead() {
        adminInbox.value = adminInbox.value.map { it.copy(read = true) }
        val inbox = inboxes[signedInUid] ?: return
        inbox.replaceAll { it.copy(read = true) }
        myInbox.value = inbox.toList()
    }

    private fun nextId(): String =
        "notif-${inboxes.values.sumOf { it.size } + adminInbox.value.size + 1}"
}
