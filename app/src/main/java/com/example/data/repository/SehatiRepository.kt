package com.example.data.repository

import com.example.data.model.OrderEntity
import com.example.data.model.OrderStatus
import com.example.data.model.ProviderEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface SehatiRepository {
    val allProviders: Flow<List<ProviderEntity>>

    /** Every order in the system. Only an admin may read this. */
    val allOrders: Flow<List<OrderEntity>>

    fun ordersForPatient(patientUid: String): Flow<List<OrderEntity>>
    fun ordersForProvider(providerId: String): Flow<List<OrderEntity>>
    fun getProvidersByCategory(category: String): Flow<List<ProviderEntity>>
    suspend fun getProviderById(id: String): ProviderEntity?

    /**
     * Flips a provider's "متاح الآن" flag. Patients sort and filter the catalogue on this field,
     * so it has to reach Firestore — a switch that only moved on the provider's own screen would
     * keep sending them requests they cannot take.
     */
    suspend fun setProviderAvailability(providerId: String, isAvailable: Boolean)

    suspend fun getOrderById(id: String): OrderEntity?
    suspend fun createOrder(order: OrderEntity): String
    suspend fun updateOrder(order: OrderEntity)
    suspend fun updateOrderStatus(id: String, status: String)

    /** Records who walked away and why, so the other party sees an accurate reason. */
    suspend fun cancelOrder(id: String, cancelledBy: String, reason: String)
}

/** Newest-window cap applied to every order feed; see [FirestoreSehatiRepository]. */
private const val MAX_FEED_ORDERS = 200L

class FirestoreSehatiRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : SehatiRepository {

    // Every listener below closes without its error. Firestore rejects a listen whenever the
    // caller is signed out or outside the rule's scope, and `close(error)` rethrows that in the
    // collector's scope, which takes down the process rather than leaving the list empty.

    override val allProviders: Flow<List<ProviderEntity>> = callbackFlow {
        val listener = firestore.collection("providers")
            .whereEqualTo("status", "ACTIVE")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close()
                    return@addSnapshotListener
                }
                val providers = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ProviderEntity::class.java)
                } ?: emptyList()
                trySend(providers)
            }
        awaitClose { listener.remove() }
    }

    override val allOrders: Flow<List<OrderEntity>> =
        ordersQuery(firestore.collection("orders"))

    // Security rules are filters only in the sense that a query is rejected outright unless
    // every document it could match is readable, so patients and providers must narrow the
    // collection themselves rather than relying on the rules to trim the result.
    override fun ordersForPatient(patientUid: String): Flow<List<OrderEntity>> =
        ordersQuery(firestore.collection("orders").whereEqualTo("patientUid", patientUid))

    override fun ordersForProvider(providerId: String): Flow<List<OrderEntity>> =
        ordersQuery(firestore.collection("orders").whereEqualTo("providerId", providerId))

    private fun ordersQuery(query: Query): Flow<List<OrderEntity>> = callbackFlow {
        // Every feed is bounded: an admin listening to the whole collection forever degrades
        // as bookings grow, and no screen actually scrolls through years of rows. The newest
        // window wins; true cursor paging can replace this when something older is needed.
        val listener = query
            .orderBy("createdAtTimestamp", Query.Direction.DESCENDING)
            .limit(MAX_FEED_ORDERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close()
                    return@addSnapshotListener
                }
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(OrderEntity::class.java)
                } ?: emptyList()
                trySend(orders)
            }
        awaitClose { listener.remove() }
    }

    override fun getProvidersByCategory(category: String): Flow<List<ProviderEntity>> = callbackFlow {
        val query = if (category == "ALL") {
            firestore.collection("providers").whereEqualTo("status", "ACTIVE")
        } else {
            firestore.collection("providers")
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("serviceCategory", category)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close()
                return@addSnapshotListener
            }
            val providers = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ProviderEntity::class.java)
            } ?: emptyList()
            trySend(providers)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getProviderById(id: String): ProviderEntity? {
        return try {
            firestore.collection("providers").document(id).get().await()
                .toObject(ProviderEntity::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun setProviderAvailability(providerId: String, isAvailable: Boolean) {
        if (providerId.isEmpty()) return
        // The wire name is pinned by @PropertyName on the entity; writing "availableNow" here
        // would silently create a second, unread field.
        firestore.collection("providers").document(providerId)
            .update("isAvailableNow", isAvailable).await()
    }

    override suspend fun getOrderById(id: String): OrderEntity? {        return try {
            firestore.collection("orders").document(id).get().await()
                .toObject(OrderEntity::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun createOrder(order: OrderEntity): String {
        val docRef = firestore.collection("orders").document()
        // Security rules require patientUid to equal the caller's uid, so stamp it here
        // rather than trusting the caller to supply it.
        val orderWithId = order.copy(
            id = docRef.id,
            patientUid = auth.currentUser?.uid ?: order.patientUid
        )
        docRef.set(orderWithId).await()
        return docRef.id
    }

    override suspend fun updateOrder(order: OrderEntity) {
        if (order.id.isNotEmpty()) {
            firestore.collection("orders").document(order.id).set(order).await()
        }
    }

    override suspend fun updateOrderStatus(id: String, status: String) {
        if (id.isNotEmpty()) {
            firestore.collection("orders").document(id)
                .update("status", status).await()
        }
    }

    override suspend fun cancelOrder(id: String, cancelledBy: String, reason: String) {
        if (id.isEmpty()) return
        firestore.collection("orders").document(id).update(
            mapOf(
                "status" to OrderStatus.CANCELLED,
                "cancelledBy" to cancelledBy,
                "cancelReason" to reason
            )
        ).await()
    }
}
