package com.example.data.supabase

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

object SupabaseAuthTokenProvider {

    private const val TIMEOUT_SECONDS = 10L

    fun currentIdToken(): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            Tasks.await(user.getIdToken(true), TIMEOUT_SECONDS, TimeUnit.SECONDS).token
        } catch (_: Exception) {
            null
        }
    }

    fun hasSignedInUser(): Boolean = FirebaseAuth.getInstance().currentUser != null
}
