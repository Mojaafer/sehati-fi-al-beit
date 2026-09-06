package com.example.data.supabase

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

object SupabaseAuthTokenProvider {

    private const val TIMEOUT_SECONDS = 10L

    /**
     * Firebase caches the ID token and refreshes it on its own once it is within five minutes of
     * expiring, so `forceRefresh = false` answers from memory almost every time. This is called
     * from an OkHttp interceptor on every PostgREST request; forcing a refresh there put a
     * blocking network round trip to Firebase in front of each one.
     */
    fun currentIdToken(): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            Tasks.await(user.getIdToken(false), TIMEOUT_SECONDS, TimeUnit.SECONDS).token
        } catch (_: Exception) {
            null
        }
    }

    fun hasSignedInUser(): Boolean = FirebaseAuth.getInstance().currentUser != null
}
