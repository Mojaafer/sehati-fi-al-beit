package com.example.data.repository

import com.example.data.model.OrderEntity
import com.example.data.model.OrderStatus
import com.example.data.model.ProviderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * In-memory [SehatiRepository] for unit tests. Mirrors the Firestore implementation's
 * behaviour: [createOrder] assigns the document id, and reads reflect writes immediately.
 */
class FakeSehatiRepository(
    providers: List<ProviderEntity> = emptyList(),
    orders: List<OrderEntity> = emptyList()
) : SehatiRepository {

    /** Lets a test drive the offline branch of an availability toggle. */
    var failAvailabilityWrites: Boolean = false

    /**
     * Mirrors a listen the security rules refuse: the cached rows still arrive, then the feed
     * ends instead of staying open, which is what leaves a screen holding stale data.
     */
    var denyOrderListens: Boolean = false

    private val providersFlow = MutableStateFlow(providers)
    private val ordersFlow = MutableStateFlow(orders)

    private var nextId = orders.size + 1

    override val allProviders: Flow<List<ProviderEntity>> = providersFlow

    override val allOrders: Flow<List<OrderEntity>>
        get() = orderFeed { it }

    override fun ordersForPatient(patientUid: String): Flow<List<OrderEntity>> =
        orderFeed { list -> list.filter { it.patientUid == patientUid } }

    override fun ordersForProvider(providerId: String): Flow<List<OrderEntity>> =
        orderFeed { list -> list.filter { it.providerId == providerId } }

    private fun orderFeed(
        select: (List<OrderEntity>) -> List<OrderEntity>
    ): Flow<List<OrderEntity>> =
        if (denyOrderListens) flow { emit(select(ordersFlow.value)) }
        else ordersFlow.map(select)

    override fun getProvidersByCategory(category: String): Flow<List<ProviderEntity>> =
        providersFlow.map { list ->
            if (category == "ALL") list else list.filter { it.serviceCategory == category }
        }

    override suspend fun getProviderById(id: String): ProviderEntity? =
        providersFlow.value.firstOrNull { it.id == id }

    override suspend fun setProviderAvailability(providerId: String, isAvailable: Boolean) {
        if (failAvailabilityWrites) throw IllegalStateException("offline")
        providersFlow.value = providersFlow.value.map {
            if (it.id == providerId) it.copy(isAvailableNow = isAvailable) else it
        }
    }

    override suspend fun getOrderById(id: String): OrderEntity? =
        ordersFlow.value.firstOrNull { it.id == id }

    override suspend fun createOrder(order: OrderEntity): String {
        val id = "order-${nextId++}"
        ordersFlow.value = listOf(order.copy(id = id)) + ordersFlow.value
        return id
    }

    override suspend fun updateOrder(order: OrderEntity) {
        ordersFlow.value = ordersFlow.value.map { if (it.id == order.id) order else it }
    }

    override suspend fun updateOrderStatus(id: String, status: String) {
        ordersFlow.value = ordersFlow.value.map {
            if (it.id == id) it.copy(status = status) else it
        }
    }

    override suspend fun cancelOrder(id: String, cancelledBy: String, reason: String) {
        ordersFlow.value = ordersFlow.value.map {
            if (it.id == id) {
                it.copy(status = OrderStatus.CANCELLED, cancelledBy = cancelledBy, cancelReason = reason)
            } else it
        }
    }
}
