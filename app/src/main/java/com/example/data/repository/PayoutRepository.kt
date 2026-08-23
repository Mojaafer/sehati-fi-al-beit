package com.example.data.repository

import com.example.data.model.OrderEntity
import com.example.data.model.OrderFees
import com.example.data.model.OrderStatus
import com.example.data.model.PayoutEntity
import com.example.data.model.PayoutStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface PayoutRepository {

    /** The signed-in provider's earnings ledger. */
    fun payoutsForProvider(providerId: String): Flow<List<PayoutEntity>>

    /** Every payout in the system. Only an admin may read this (see firestore.rules). */
    val allPayouts: Flow<List<PayoutEntity>>

    /**
     * Records what a completed visit owes the provider, as `payouts/{orderId}`. Idempotent:
     * if the entry already exists nothing is written and false comes back.
     */
    suspend fun accrueForCompletedOrder(order: OrderEntity): Boolean

    /** Marks an accrued payout as sent to the provider's Bankak account. */
    suspend fun markPaid(payoutId: String)
}

/** What a provider has earned, split into money still owed and money already sent. */
data class ProviderEarnings(
    val accruedSdg: Double,
    val paidSdg: Double,
    val accruedCount: Int
)

/** Folds a provider's ledger; pure so the split is verifiable without a backend. */
fun foldEarnings(payouts: List<PayoutEntity>): ProviderEarnings = ProviderEarnings(
    accruedSdg = payouts.filter { it.status == PayoutStatus.ACCRUED }.sumOf { it.amountSdg },
    paidSdg = payouts.filter { it.status == PayoutStatus.PAID }.sumOf { it.amountSdg },
    accruedCount = payouts.count { it.status == PayoutStatus.ACCRUED }
)

/** The platform-side picture: money in, commission kept, money owed out and already out. */
data class AdminFinanceSummary(
    val collectedSdg: Double,
    val commissionSdg: Double,
    val owedToProvidersSdg: Double,
    val paidOutSdg: Double
) {
    val netPositionSdg: Double get() = collectedSdg - owedToProvidersSdg - paidOutSdg
}

/**
 * Folds orders and payouts into the admin finance summary. Collection counts every order whose
 * payment was confirmed or whose visit completed; the owed side comes from the payout ledger,
 * not from orders, so an accrual that failed to write shows up as collected-but-not-owed rather
 * than silently vanishing.
 */
fun foldAdminFinance(orders: List<OrderEntity>, payouts: List<PayoutEntity>): AdminFinanceSummary {
    val collectedOrders = orders.filter {
        it.status == OrderStatus.PAYMENT_CONFIRMED || it.status == OrderStatus.COMPLETED
    }
    return AdminFinanceSummary(
        collectedSdg = collectedOrders.sumOf { it.priceSdg },
        commissionSdg = collectedOrders.sumOf { it.commissionSdg },
        owedToProvidersSdg = payouts.filter { it.status == PayoutStatus.ACCRUED }.sumOf { it.amountSdg },
        paidOutSdg = payouts.filter { it.status == PayoutStatus.PAID }.sumOf { it.amountSdg }
    )
}

/**
 * The one growth number a booking marketplace lives on: of the patients who ever finished a
 * visit, what share came back for another. Computed over paid/completed orders keyed by
 * patientUid; null when nobody has completed a visit yet, because "0% retention" on an empty
 * ledger would be a lie.
 */
fun repeatBookingRate(orders: List<OrderEntity>): Double? {
    val visitsPerPatient = orders
        .filter { it.status == OrderStatus.COMPLETED && it.patientUid.isNotBlank() }
        .groupingBy { it.patientUid }
        .eachCount()
    val returning = visitsPerPatient.values.count { it > 1 }
    val total = visitsPerPatient.size
    if (total == 0) return null
    return returning.toDouble() / total
}

class FirestorePayoutRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : PayoutRepository {

    override fun payoutsForProvider(providerId: String): Flow<List<PayoutEntity>> =
        payoutsQuery(firestore.collection("payouts").whereEqualTo("providerId", providerId))

    override val allPayouts: Flow<List<PayoutEntity>> =
        payoutsQuery(firestore.collection("payouts"))

    private fun payoutsQuery(query: Query): Flow<List<PayoutEntity>> = callbackFlow {
        val listener = query
            .orderBy("createdAtTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Same convention as the order feeds: close without rethrowing so a refused
                    // listen empties the list instead of taking down the process.
                    close()
                    return@addSnapshotListener
                }
                trySend(
                    snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(PayoutEntity::class.java)
                    } ?: emptyList()
                )
            }
        awaitClose { listener.remove() }
    }

    /**
     * The document id is the order id, which is what makes double accrual impossible — the
     * second write would need the doc to be absent inside this same transaction.
     */
    override suspend fun accrueForCompletedOrder(order: OrderEntity): Boolean {
        if (order.id.isEmpty() || order.providerId.isEmpty()) return false

        val payoutRef = firestore.collection("payouts").document(order.id)
        return firestore.runTransaction { transaction ->
            if (transaction.get(payoutRef).exists()) return@runTransaction false

            transaction.set(
                payoutRef,
                PayoutEntity(
                    id = order.id,
                    orderId = order.id,
                    orderNumber = order.orderNumber,
                    providerId = order.providerId,
                    providerName = order.providerName,
                    patientName = order.patientName,
                    amountSdg = if (order.providerPayoutSdg > 0.0) {
                        order.providerPayoutSdg
                    } else {
                        OrderFees.providerPayoutSdg(order.priceSdg)
                    },
                    status = PayoutStatus.ACCRUED,
                    createdAtTimestamp = System.currentTimeMillis()
                )
            )
            true
        }.await()
    }

    override suspend fun markPaid(payoutId: String) {
        require(payoutId.isNotEmpty()) { "تسجيل الدفع يحتاج رقم المستحق" }

        val payoutRef = firestore.collection("payouts").document(payoutId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(payoutRef)
            if (!snapshot.exists() || snapshot.getString("status") != PayoutStatus.ACCRUED) {
                return@runTransaction
            }
            transaction.update(
                payoutRef,
                mapOf(
                    "status" to PayoutStatus.PAID,
                    "paidAtTimestamp" to System.currentTimeMillis()
                )
            )
        }.await()
    }
}
