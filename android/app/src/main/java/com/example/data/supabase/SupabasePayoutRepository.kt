package com.example.data.supabase

import com.example.data.model.OrderEntity
import com.example.data.model.OrderFees
import com.example.data.model.PayoutEntity
import com.example.data.model.PayoutStatus
import com.example.data.repository.PayoutRepository
import kotlinx.coroutines.flow.Flow

class SupabasePayoutRepository(
    private val api: SupabaseApiService = SupabaseClient.api,
    private val pollIntervalMs: Long = 4000L
) : PayoutRepository {

    override fun payoutsForProvider(providerId: String): Flow<List<PayoutEntity>> = pollFeed(pollIntervalMs) {
        api.getPayouts(
            mapOf(
                "provider_id" to "eq.$providerId",
                "order" to "created_at_timestamp.desc"
            )
        ).map { it.toEntity() }
    }

    override val allPayouts: Flow<List<PayoutEntity>> = pollFeed(pollIntervalMs) {
        api.getPayouts(
            mapOf("order" to "created_at_timestamp.desc")
        ).map { it.toEntity() }
    }

    override suspend fun accrueForCompletedOrder(order: OrderEntity): Boolean {
        if (order.id.isEmpty() || order.providerId.isEmpty()) return false
        val payoutAmount = OrderFees.effectiveProviderPayoutSdg(order)
        if (payoutAmount <= 0.0) return false

        val payout = SupabasePayoutDto(
            id = order.id,
            orderId = order.id,
            orderNumber = order.orderNumber,
            providerId = order.providerId,
            providerPayoutSdg = payoutAmount,
            amountSdg = payoutAmount,
            status = PayoutStatus.ACCRUED,
            createdAtTimestamp = System.currentTimeMillis()
        )

        return try {
            val response = api.insertPayout(payout)
            response.isSuccessful
        } catch (e: Exception) {
            android.util.Log.w("SehatiPayout", "payout accrual failed for order=${order.id}", e)
            false
        }
    }

    override suspend fun markPaid(payoutId: String) {
        if (payoutId.isEmpty()) return
        try {
            api.updatePayout(
                filter = "eq.$payoutId",
                updates = mapOf(
                    "status" to PayoutStatus.PAID,
                    "paid_at_timestamp" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            android.util.Log.w("SehatiPayout", "markPaid failed for payout=$payoutId", e)
        }
    }
}
