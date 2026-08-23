package com.example.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.util.CustomClassMapper
import org.junit.Test

/**
 * Firestore's mapper writes through setters and derives every name from the accessor, so a
 * `val` property is written but silently never read back, and a Kotlin `isX` property is
 * stored as `x`. Both failures are invisible until a real device reads a real document,
 * which is why they are pinned here.
 */
class FirestoreMappingTest {

    private fun assertMappable(instance: Any) {
        val clazz = instance.javaClass
        @Suppress("UNCHECKED_CAST")
        val serialized = CustomClassMapper.convertToPlainJavaTypes(instance) as Map<String, Any?>
        val setters = clazz.methods.filter { it.name.startsWith("set") }
            .map { setter ->
                setter.getAnnotation(PropertyName::class.java)?.value
                    ?: setter.name.removePrefix("set").replaceFirstChar { c -> c.lowercase() }
            }
            .toSet()

        val unreadable = serialized.keys.filterNot { it in setters }.sorted()
        check(unreadable.isEmpty()) {
            "${clazz.simpleName} writes fields Firestore can never read back: $unreadable\n" +
                "  serialized keys: ${serialized.keys.sorted()}\n" +
                "  setters:         ${setters.sorted()}"
        }
    }

    @Test
    fun `provider round trips`() = assertMappable(ProviderEntity(name = "n"))

    @Test
    fun `order round trips`() = assertMappable(OrderEntity(orderNumber = "HM-1"))

    @Test
    fun `notification round trips`() = assertMappable(NotificationEntity(title = "t"))

    @Test
    fun `rating round trips`() = assertMappable(RatingEntity(orderId = "o"))

    /** The seeded catalogue and the security rules hardcode these, so a rename must fail here. */
    @Test
    fun `boolean fields keep the names the seed data and rules use`() {
        @Suppress("UNCHECKED_CAST")
        val provider = CustomClassMapper.convertToPlainJavaTypes(
            ProviderEntity(isAvailableNow = true, isVerified = true)
        ) as Map<String, Any?>
        check(provider["isAvailableNow"] == true) { "expected isAvailableNow, got ${provider.keys}" }
        check(provider["isVerified"] == true) { "expected isVerified, got ${provider.keys}" }

        @Suppress("UNCHECKED_CAST")
        val order = CustomClassMapper.convertToPlainJavaTypes(
            OrderEntity(isRated = true)
        ) as Map<String, Any?>
        check(order["isRated"] == true) { "expected isRated, got ${order.keys}" }
    }
}
