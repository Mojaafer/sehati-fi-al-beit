package com.example.data.supabase

import com.example.data.model.OrderEntity
import com.example.data.model.PayoutEntity
import com.example.data.model.PayoutStatus
import com.example.data.model.ProviderEntity
import com.example.data.model.RatingEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseMappingTest {

    @Test
    fun providerDto_roundTrip_preservesFields() {
        val entity = ProviderEntity(
            id = "prov-123",
            name = "د. أحمد علي",
            title = "أخصائي تمريض",
            experienceYears = 8,
            rating = 4.8,
            reviewsCount = 25,
            priceSdg = 15000.0,
            isAvailableNow = true,
            serviceCategory = "NURSING",
            area = "الخرطوم, الرياض",
            phoneNumber = "+249912345678",
            ownerUid = "uid-owner-1",
            status = "ACTIVE",
            reviewNote = "",
            ratingSum = 120.0,
            ratingCount = 25,
            about = "خبرة واسعة في الرعاية المنزلية"
        )

        val dto = SupabaseProviderDto.fromEntity(entity)
        val convertedBack = dto.toEntity()

        assertEquals("prov-123", convertedBack.id)
        assertEquals("د. أحمد علي", convertedBack.name)
        assertEquals("أخصائي تمريض", convertedBack.title)
        assertEquals(8, convertedBack.experienceYears)
        assertEquals(4.8, convertedBack.rating, 0.01)
        assertEquals(25, convertedBack.reviewsCount)
        assertEquals(15000.0, convertedBack.priceSdg, 0.01)
        assertTrue(convertedBack.isAvailableNow)
        assertEquals("NURSING", convertedBack.serviceCategory)
        assertEquals("الخرطوم, الرياض", convertedBack.area)
        assertEquals("+249912345678", convertedBack.phoneNumber)
        assertEquals("uid-owner-1", convertedBack.ownerUid)
        assertEquals("ACTIVE", convertedBack.status)
        assertTrue(convertedBack.isVerified)
    }

    @Test
    fun orderDto_roundTrip_preservesMoneyAndTerms() {
        val entity = OrderEntity(
            id = "ord-999",
            orderNumber = "ORD-2026-001",
            serviceTitle = "LAB_DRAW",
            serviceDetails = "فحص دم شامل",
            patientName = "محمد عثمان",
            patientPhone = "+249123456789",
            providerName = "د. فاطمة",
            providerTitle = "مختبرات طبية",
            providerPhone = "+249987654321",
            areaLocation = "بحري, الصافية",
            visitDate = "2026-08-25",
            visitTime = "10:00 AM",
            priceSdg = 20000.0,
            payableAmountSdg = 20042.0,
            providerPayoutSdg = 17000.0,
            commissionSdg = 3000.0,
            status = "ORDER_SENT",
            patientUid = "pat-123",
            providerId = "prov-456"
        )

        val dto = SupabaseOrderDto.fromEntity(entity)
        val convertedBack = dto.toEntity()

        assertEquals("ord-999", convertedBack.id)
        assertEquals("ORD-2026-001", convertedBack.orderNumber)
        assertEquals("LAB_DRAW", convertedBack.serviceTitle)
        assertEquals("محمد عثمان", convertedBack.patientName)
        assertEquals(20000.0, convertedBack.priceSdg, 0.01)
        assertEquals(20042.0, convertedBack.payableAmountSdg, 0.01)
        assertEquals(17000.0, convertedBack.providerPayoutSdg, 0.01)
        assertEquals(3000.0, convertedBack.commissionSdg, 0.01)
        assertEquals("ORDER_SENT", convertedBack.status)
        assertEquals("pat-123", convertedBack.patientUid)
        assertEquals("prov-456", convertedBack.providerId)
    }

    @Test
    fun payoutDto_roundTrip_preservesLedgerFields() {
        val entity = PayoutEntity(
            id = "ord-100",
            orderId = "ord-100",
            orderNumber = "ORD-2026-100",
            providerId = "prov-77",
            amountSdg = 25500.0,
            status = PayoutStatus.ACCRUED,
            createdAtTimestamp = 1756000000000L,
            paidAtTimestamp = 0L
        )

        val dto = SupabasePayoutDto.fromEntity(entity)
        val convertedBack = dto.toEntity()

        assertEquals("ord-100", convertedBack.id)
        assertEquals("ord-100", convertedBack.orderId)
        assertEquals("ORD-2026-100", convertedBack.orderNumber)
        assertEquals("prov-77", convertedBack.providerId)
        assertEquals(25500.0, convertedBack.amountSdg, 0.01)
        assertEquals(PayoutStatus.ACCRUED, convertedBack.status)
    }

    @Test
    fun ratingDto_roundTrip_preservesRatingValues() {
        val entity = RatingEntity(
            id = "ord-200",
            orderId = "ord-200",
            providerId = "prov-88",
            patientUid = "pat-55",
            patientName = "سارة أحمد",
            stars = 5,
            comment = "خدمة ممتازة ودقة في المواعيد",
            createdAtTimestamp = 1756000000000L
        )

        val dto = SupabaseRatingDto.fromEntity(entity)
        val convertedBack = dto.toEntity()

        assertEquals("ord-200", convertedBack.id)
        assertEquals("ord-200", convertedBack.orderId)
        assertEquals("prov-88", convertedBack.providerId)
        assertEquals("pat-55", convertedBack.patientUid)
        assertEquals("سارة أحمد", convertedBack.patientName)
        assertEquals(5, convertedBack.stars)
        assertEquals("خدمة ممتازة ودقة في المواعيد", convertedBack.comment)
    }

    /**
     * The live project URL and anon key used to sit in [SupabaseConfig] as defaults, so a build
     * with no credentials reached the real project with a key committed to source. An unconfigured
     * build must now fail loudly instead.
     */
    @Test
    fun supabaseConfig_hasNoBakedInFallback() {
        if (SupabaseConfig.isConfigured) {
            assertTrue(SupabaseConfig.url.startsWith("http"))
            assertTrue(SupabaseConfig.anonKey.isNotEmpty())
        } else {
            assertThrows(IllegalStateException::class.java) { SupabaseConfig.url }
            assertThrows(IllegalStateException::class.java) { SupabaseConfig.anonKey }
        }
    }
}
