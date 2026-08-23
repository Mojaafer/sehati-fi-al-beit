package com.example.data.repository

import com.example.data.model.OrderEntity
import com.example.data.model.ProviderEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SehatiRepositoryTest {

    private val providers = listOf(
        ProviderEntity(
            id = "p1",
            name = "محمد عبدالرحمن",
            serviceCategory = "LAB_DRAW",
            priceSdg = 15000.0
        ),
        ProviderEntity(
            id = "p2",
            name = "سارة عثمان",
            serviceCategory = "NURSING",
            priceSdg = 18000.0
        )
    )

    @Test
    fun testGetProvidersByCategoryFiltersByServiceCategory() = runTest {
        val repository = FakeSehatiRepository(providers = providers)

        val labProviders = repository.getProvidersByCategory("LAB_DRAW").first()
        assertEquals(1, labProviders.size)
        assertEquals("محمد عبدالرحمن", labProviders.first().name)
    }

    @Test
    fun testAllCategoryReturnsEveryProvider() = runTest {
        val repository = FakeSehatiRepository(providers = providers)

        assertEquals(2, repository.getProvidersByCategory("ALL").first().size)
    }

    @Test
    fun testGetProviderByIdReturnsNullForUnknownId() = runTest {
        val repository = FakeSehatiRepository(providers = providers)

        assertEquals("سارة عثمان", repository.getProviderById("p2")?.name)
        assertNull(repository.getProviderById("missing"))
    }

    @Test
    fun testCreateOrderAssignsIdAndAppearsInAllOrders() = runTest {
        val repository = FakeSehatiRepository()

        val newId = repository.createOrder(
            OrderEntity(orderNumber = "HM-9001", serviceTitle = "سحب عينات منزلية", status = "ORDER_SENT")
        )

        assertTrue(newId.isNotEmpty())
        val stored = repository.getOrderById(newId)
        assertNotNull(stored)
        assertEquals(newId, stored?.id)
        assertEquals("HM-9001", stored?.orderNumber)
        assertEquals(1, repository.allOrders.first().size)
    }

    @Test
    fun testUpdateOrderStatusPersistsNewStatus() = runTest {
        val repository = FakeSehatiRepository()
        val id = repository.createOrder(OrderEntity(orderNumber = "HM-9002", status = "PAYMENT_UNDER_REVIEW"))

        repository.updateOrderStatus(id, "PAYMENT_CONFIRMED")

        assertEquals("PAYMENT_CONFIRMED", repository.getOrderById(id)?.status)
    }

    @Test
    fun testUpdateOrderReplacesFullDocument() = runTest {
        val repository = FakeSehatiRepository()
        val id = repository.createOrder(OrderEntity(orderNumber = "HM-9003", status = "ORDER_SENT"))
        val order = repository.getOrderById(id)!!

        repository.updateOrder(
            order.copy(
                status = "PAYMENT_UNDER_REVIEW",
                transferSenderName = "أحمد الرفاعي",
                transferRefNum = "5821049"
            )
        )

        val updated = repository.getOrderById(id)
        assertEquals("PAYMENT_UNDER_REVIEW", updated?.status)
        assertEquals("أحمد الرفاعي", updated?.transferSenderName)
        assertEquals("5821049", updated?.transferRefNum)
    }
}
