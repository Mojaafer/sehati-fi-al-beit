package com.example.data.supabase

import com.example.BuildConfig

object SupabaseConfig {
    const val DEFAULT_URL = "https://wolngyvenfyuaigjxajs.supabase.co"
    const val DEFAULT_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndvbG5neXZlbmZ5dWFpZ2p4YWpzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODc0ODMyNzQsImV4cCI6MjEwMzA1OTI3NH0.0OYLx4XOASh88gGCGVdHoozZVSPY1l-XH20kjyrDUn8"

    val url: String
        get() = BuildConfig.SUPABASE_URL.ifEmpty { DEFAULT_URL }

    val anonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY.ifEmpty { DEFAULT_ANON_KEY }

    private var userAuthToken: String? = null

    fun setAuthToken(token: String?) {
        userAuthToken = token
    }

    fun getAuthToken(): String? = userAuthToken
}
