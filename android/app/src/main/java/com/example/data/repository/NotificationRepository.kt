package com.example.data.repository

import com.example.data.model.NotificationEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

interface NotificationRepository {
    /** Live feed for the signed-in user; empty while nobody is signed in. */
    val myNotifications: Flow<List<NotificationEntity>>

    /**
     * Live feed of the shared admin inbox. Only an admin may read it, so for anyone else the
     * listen is refused and the feed simply ends — callers gate on the role rather than relying
     * on that, to keep a rejected listen out of the logs on every patient's device.
     */
    val adminNotifications: Flow<List<NotificationEntity>>

    /**
     * Writes a notification into [uid]'s inbox. Called by whoever changes an order's
     * status, which is why the rules allow any signed-in user to create — but only
     * the owner to read.
     */
    suspend fun push(uid: String, notification: NotificationEntity)

    /**
     * Writes into the shared admin inbox. Used for events that need an admin's decision but
     * originate from someone who cannot discover an admin's uid — filing a provider
     * application, or handing in a transfer receipt.
     */
    suspend fun pushToAdmins(notification: NotificationEntity)

    /** Routes on [NotificationEntity.scope], since the two inboxes are separate collections. */
    suspend fun markRead(notification: NotificationEntity)
    suspend fun markAllRead()
}

class FirestoreNotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : NotificationRepository {

    private fun inbox(uid: String) =
        firestore.collection("users").document(uid).collection("notifications")

    private fun adminInbox() = firestore.collection("adminNotifications")

    override val myNotifications: Flow<List<NotificationEntity>>
        get() {
            val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
            return feed(inbox(uid), NotificationEntity.SCOPE_USER)
        }

    override val adminNotifications: Flow<List<NotificationEntity>>
        get() {
            if (auth.currentUser == null) return flowOf(emptyList())
            return feed(adminInbox(), NotificationEntity.SCOPE_ADMIN)
        }

    /**
     * [expectedScope] is stamped on every document read back, because documents written before
     * the field existed default to USER and would otherwise be marked read against the wrong
     * collection.
     */
    private fun feed(collection: CollectionReference, expectedScope: String) =
        callbackFlow {
            val listener = collection
                .orderBy("createdAtTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Closing with the error would rethrow it in the collector's scope
                        // and take down the process; a rejected listen just ends the feed.
                        close()
                        return@addSnapshotListener
                    }
                    trySend(
                        snapshot?.documents?.mapNotNull {
                            it.toObject(NotificationEntity::class.java)?.apply { scope = expectedScope }
                        } ?: emptyList()
                    )
                }
            awaitClose { listener.remove() }
        }

    override suspend fun push(uid: String, notification: NotificationEntity) {
        if (uid.isEmpty()) return
        inbox(uid).add(notification.copy(scope = NotificationEntity.SCOPE_USER)).await()
    }

    override suspend fun pushToAdmins(notification: NotificationEntity) {
        adminInbox().add(notification.copy(scope = NotificationEntity.SCOPE_ADMIN)).await()
    }

    override suspend fun markRead(notification: NotificationEntity) {
        if (notification.id.isEmpty()) return
        val document = if (notification.scope == NotificationEntity.SCOPE_ADMIN) {
            adminInbox().document(notification.id)
        } else {
            val uid = auth.currentUser?.uid ?: return
            inbox(uid).document(notification.id)
        }
        document.update("read", true).await()
    }

    override suspend fun markAllRead() {
        val uid = auth.currentUser?.uid ?: return
        val batch = firestore.batch()
        var pending = 0

        for (collection in listOf(inbox(uid), adminInbox())) {
            // An admin-inbox query is refused for everyone else, and that must not stop the
            // user's own inbox from being cleared.
            val unread = try {
                collection.whereEqualTo("read", false).get().await()
            } catch (e: Exception) {
                continue
            }
            for (doc in unread.documents) {
                batch.update(doc.reference, "read", true)
                pending++
            }
        }

        if (pending > 0) batch.commit().await()
    }
}
