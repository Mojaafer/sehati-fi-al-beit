package com.example.data.supabase

import com.example.data.model.OrderEntity
import com.example.data.model.OrderStatus
import com.example.data.model.ProviderEntity
import com.example.data.repository.SehatiRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class SupabaseSehatiRepository(
    private val api: SupabaseApiService = SupabaseClient.api,
    private val pollIntervalMs: Long = 4000L
) : SehatiRepository {

    override val allProviders: Flow<List<ProviderEntity>> = pollFeed(pollIntervalMs) {
        api.getProviders(
            mapOf(
                "status" to "eq.ACTIVE",
                "order" to "rating.desc"
            )
        ).map { it.toEntity() }
    }

    override val allOrders: Flow<List<OrderEntity>> = pollFeed(pollIntervalMs) {
        api.getOrders(
            mapOf(
                "order" to "created_at_timestamp.desc",
                "limit" to "200"
            )
        ).map { it.toEntity() }
    }

    override fun ordersForPatient(patientUid: String): Flow<List<OrderEntity>> = pollFeed(pollIntervalMs) {
        api.getOrders(
            mapOf(
                "patient_uid" to "eq.$patientUid",
                "order" to "created_at_timestamp.desc",
                "limit" to "200"
            )
        ).map { it.toEntity() }
    }

    override fun ordersForProvider(providerId: String): Flow<List<OrderEntity>> = pollFeed(pollIntervalMs) {
        api.getOrders(
            mapOf(
                "provider_id" to "eq.$providerId",
                "order" to "created_at_timestamp.desc",
                "limit" to "200"
            )
        ).map { it.toEntity() }
    }

    override fun getProvidersByCategory(category: String): Flow<List<ProviderEntity>> = pollFeed(pollIntervalMs) {
        val filters = if (category == "ALL") {
            mapOf("status" to "eq.ACTIVE", "order" to "rating.desc")
        } else {
            mapOf("status" to "eq.ACTIVE", "category" to "eq.$category", "order" to "rating.desc")
        }
        api.getProviders(filters).map { it.toEntity() }
    }

    override suspend fun getProviderById(id: String): ProviderEntity? {
        return try {
            val list = api.getProviders(mapOf("id" to "eq.$id", "limit" to "1"))
            list.firstOrNull()?.toEntity()
        } catch (e: Exception) {
            android.util.Log.w("SehatiRepo", "getProviderById($id) failed", e)
            null
        }
    }

    override suspend fun setProviderAvailability(providerId: String, isAvailable: Boolean) {
        if (providerId.isEmpty()) return
        try {
            api.updateProvider(
                filter = "eq.$providerId",
                updates = mapOf("is_available" to isAvailable)
            )
        } catch (e: Exception) {
            android.util.Log.w("SehatiRepo", "setProviderAvailability($providerId) failed", e)
        }
    }

    override suspend fun getOrderById(id: String): OrderEntity? {
        return try {
            val list = api.getOrders(mapOf("id" to "eq.$id", "limit" to "1"))
            list.firstOrNull()?.toEntity()
        } catch (e: Exception) {
            android.util.Log.w("SehatiRepo", "getOrderById($id) failed", e)
            null
        }
    }

    override suspend fun createOrder(order: OrderEntity): String {
        val orderId = order.id.ifEmpty { UUID.randomUUID().toString() }
        val orderDto = SupabaseOrderDto.fromEntity(order.copy(id = orderId))
        val inserted = api.insertOrder(orderDto)
        return inserted.firstOrNull()?.id ?: orderId
    }

    override suspend fun updateOrder(order: OrderEntity) {
        if (order.id.isEmpty()) return
        val current = getOrderById(order.id) ?: return
        require(current.status != order.status) {
            "Supabase order updates must use a validated lifecycle transition"
        }
        api.transitionOrder(
            TransitionOrderRpcRequest(
                orderId = order.id,
                targetStatus = order.status,
                reason = order.cancelReason,
                receiptImageUri = order.receiptImageUri.orEmpty()
            )
        )
    }

    override suspend fun updateOrderStatus(id: String, status: String) {
        if (id.isEmpty()) return
        api.transitionOrder(
            TransitionOrderRpcRequest(
                orderId = id,
                targetStatus = status
            )
        )
    }

    override suspend fun cancelOrder(id: String, cancelledBy: String, reason: String) {
        if (id.isEmpty()) return
        api.transitionOrder(
            TransitionOrderRpcRequest(
                orderId = id,
                targetStatus = OrderStatus.CANCELLED,
                cancelledBy = cancelledBy,
                reason = reason
            )
        )
    }
}
