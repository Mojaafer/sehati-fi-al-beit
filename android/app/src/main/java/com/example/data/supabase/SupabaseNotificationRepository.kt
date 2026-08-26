package com.example.data.supabase

import com.example.data.model.NotificationEntity
import com.example.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class SupabaseNotificationRepository(
    private val api: SupabaseApiService = SupabaseClient.api,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val pollIntervalMs: Long = 5000L
) : NotificationRepository {

    override val myNotifications: Flow<List<NotificationEntity>>
        get() {
            // The uid is captured once at subscription time; a feed never switches account mid-stream.
            val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
            return pollFeed(pollIntervalMs) {
                api.getNotifications(
                    mapOf(
                        "recipient_uid" to "eq.$uid",
                        "order" to "created_at_timestamp.desc",
                        "limit" to "50"
                    )
                ).map { it.toEntity(NotificationEntity.SCOPE_USER) }
            }
        }

    override val adminNotifications: Flow<List<NotificationEntity>>
        get() {
            if (auth.currentUser == null) return flowOf(emptyList())
            return pollFeed(pollIntervalMs) {
                api.getAdminNotifications(
                    mapOf(
                        "order" to "created_at_timestamp.desc",
                        "limit" to "50"
                    )
                ).map { it.toEntity(NotificationEntity.SCOPE_ADMIN) }
            }
        }

    override suspend fun push(uid: String, notification: NotificationEntity) {
        if (uid.isEmpty()) return
        try {
            api.insertNotification(
                SupabaseNotificationDto.fromEntity(
                    notification.copy(scope = NotificationEntity.SCOPE_USER),
                    recipientUid = uid
                )
            )
        } catch (e: Exception) {
            android.util.Log.w("SehatiNotify", "push to $uid failed", e)
        }
    }

    override suspend fun pushToAdmins(notification: NotificationEntity) {
        try {
            api.insertAdminNotification(
                SupabaseNotificationDto.fromEntity(
                    notification.copy(scope = NotificationEntity.SCOPE_ADMIN)
                )
            )
        } catch (e: Exception) {
            android.util.Log.w("SehatiNotify", "pushToAdmins failed", e)
        }
    }

    override suspend fun markRead(notification: NotificationEntity) {
        if (notification.id.isEmpty()) return
        try {
            if (notification.scope == NotificationEntity.SCOPE_ADMIN) {
                api.updateAdminNotification(
                    filter = "eq.${notification.id}",
                    updates = mapOf("read" to true)
                )
            } else {
                api.updateNotification(
                    filter = "eq.${notification.id}",
                    updates = mapOf("read" to true)
                )
            }
        } catch (e: Exception) {
            android.util.Log.w("SehatiNotify", "markRead(${notification.id}) failed", e)
        }
    }

    override suspend fun markAllRead() {
        val uid = auth.currentUser?.uid ?: return
        try {
            api.updateNotificationsByRecipient(
                filter = "eq.$uid",
                updates = mapOf("read" to true)
            )
        } catch (e: Exception) {
            android.util.Log.w("SehatiNotify", "markAllRead for $uid failed", e)
        }
    }
}
