package com.example.data.supabase

import com.example.BuildConfig

/**
 * Supabase connection settings, supplied at build time only.
 *
 * There is deliberately no fallback: a hardcoded default meant a build with no
 * `SUPABASE_URL` / `SUPABASE_ANON_KEY` silently talked to the live project with a key
 * committed to source. Failing loudly is the point — pass `-PsupabaseUrl=` /
 * `-PsupabaseAnonKey=` or export the environment variables (see `android/.env.example`).
 */
object SupabaseConfig {

    val url: String
        get() = BuildConfig.SUPABASE_URL.ifEmpty { missing("SUPABASE_URL", "supabaseUrl") }

    val anonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY.ifEmpty { missing("SUPABASE_ANON_KEY", "supabaseAnonKey") }

    /** True when this build can reach Supabase at all; lets callers skip instead of throwing. */
    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotEmpty() && BuildConfig.SUPABASE_ANON_KEY.isNotEmpty()

    private fun missing(envName: String, propertyName: String): Nothing = throw IllegalStateException(
        "$envName is not set for this build. Pass -P$propertyName=... to Gradle or export $envName."
    )

    private var userAuthToken: String? = null

    fun setAuthToken(token: String?) {
        userAuthToken = token
    }

    fun getAuthToken(): String? = userAuthToken
}
