package com.example.data.supabase

import com.google.firebase.auth.FirebaseUser

class SupabaseUserRepository(
    private val api: SupabaseApiService = SupabaseClient.api
) {
    suspend fun ensureUserDocument(user: FirebaseUser) {
        try {
            val existing = api.getUser("eq.${user.uid}")
            if (existing.isEmpty()) {
                val newUser = SupabaseUserDto(
                    uid = user.uid,
                    role = "PATIENT",
                    phoneNumber = user.phoneNumber.orEmpty(),
                    displayName = user.displayName.orEmpty(),
                    status = "ACTIVE",
                    createdAtTimestamp = System.currentTimeMillis(),
                    updatedAtTimestamp = System.currentTimeMillis()
                )
                api.upsertUser(newUser)
            }
        } catch (_: Exception) {
        }
    }

    suspend fun fetchUserRole(uid: String): String {
        return try {
            val users = api.getUser("eq.$uid")
            users.firstOrNull()?.role ?: "PATIENT"
        } catch (_: Exception) {
            "PATIENT"
        }
    }

    suspend fun fetchProviderId(uid: String): String {
        return try {
            val users = api.getUser("eq.$uid")
            users.firstOrNull()?.providerId.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun fetchUserProfile(uid: String): Pair<String, String> {
        return try {
            val users = api.getUser("eq.$uid")
            val user = users.firstOrNull()
            (user?.displayName.orEmpty()) to (user?.address.orEmpty())
        } catch (_: Exception) {
            "" to ""
        }
    }

    suspend fun updateUserProfile(uid: String, name: String, address: String) {
        try {
            api.updateUser(
                filter = "eq.$uid",
                updates = mapOf(
                    "display_name" to name,
                    "address" to address,
                    "updated_at_timestamp" to System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {
        }
    }

    suspend fun updateFcmToken(uid: String, token: String) {
        try {
            api.updateUser(
                filter = "eq.$uid",
                updates = mapOf(
                    "fcm_token" to token,
                    "updated_at_timestamp" to System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {
        }
    }
}
