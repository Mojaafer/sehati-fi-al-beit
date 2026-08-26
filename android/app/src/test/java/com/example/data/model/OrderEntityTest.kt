package com.example.data.model

import org.junit.Assert.*
import org.junit.Test

class OrderEntityTest {

    @Test
    fun testOrderEntityFieldsAndDefaults() {
        val order = OrderEntity(
            orderNumber = "HM-5555",
            serviceTitle = "كشف منزلي",
            serviceDetails = "مريض واحد",
            patientName = "محمد علي",
            providerName = "د. فاطمة",
            providerTitle = "طبيبة عامة",
            areaLocation = "الخرطوم",
            visitDate = "اليوم",
            visitTime = "5:00 مساءً",
            notes = "عاجل",
            priceSdg = 35000.0,
            paymentMethod = "بنكك (Bankak)",
            status = "ORDER_SENT"
        )

        assertEquals("HM-5555", order.orderNumber)
        assertEquals("كشف منزلي", order.serviceTitle)
        assertEquals("محمد علي", order.patientName)
        assertEquals("د. فاطمة", order.providerName)
        assertEquals(35000.0, order.priceSdg, 0.01)
        assertEquals("ORDER_SENT", order.status)
        assertTrue(order.createdAtTimestamp > 0)
    }

    @Test
    fun testOrderEntityStatusUpdate() {
        val initialOrder = OrderEntity(
            id = "order-10",
            orderNumber = "HM-1234",
            serviceTitle = "علاج طبيعي",
            patientName = "سارة أحمد",
            status = "ORDER_SENT"
        )

        val updatedOrder = initialOrder.copy(
            status = "PAYMENT_UNDER_REVIEW",
            transferSenderName = "سارة أحمد",
            transferRefNum = "9876543"
        )

        assertEquals("order-10", updatedOrder.id)
        assertEquals("PAYMENT_UNDER_REVIEW", updatedOrder.status)
        assertEquals("سارة أحمد", updatedOrder.transferSenderName)
        assertEquals("9876543", updatedOrder.transferRefNum)
    }

    @Test
    fun testProviderEntityFields() {
        val provider = ProviderEntity(
            id = "provider-5",
            name = "أحمد الطيب",
            title = "أخصائي علاج طبيعي",
            experienceYears = 12,
            rating = 4.7,
            reviewsCount = 71,
            distanceKm = 3.2,
            priceSdg = 28000.0,
            isAvailableNow = true,
            serviceCategory = "PHYSIO",
            area = "ود مدني - حي النخيل"
        )

        assertEquals("provider-5", provider.id)
        assertEquals("أحمد الطيب", provider.name)
        assertEquals("PHYSIO", provider.serviceCategory)
        assertTrue(provider.isAvailableNow)
        assertEquals(4.7, provider.rating, 0.01)
    }
}
