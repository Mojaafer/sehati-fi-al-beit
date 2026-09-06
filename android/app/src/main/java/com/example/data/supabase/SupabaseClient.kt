package com.example.data.supabase

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import java.io.IOException

object SupabaseClient {

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = SupabaseConfig.getAuthToken()
            ?: SupabaseAuthTokenProvider.currentIdToken()
            ?: if (SupabaseAuthTokenProvider.hasSignedInUser()) {
                throw IOException("Authenticated Supabase token unavailable")
            } else {
                SupabaseConfig.anonKey
            }

        val request = original.newBuilder()
            .header("apikey", SupabaseConfig.anonKey)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build()

        chain.proceed(request)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .apply {
                // Release builds ship no logging at all. Debug builds log at BASIC — method, URL,
                // status and timing — deliberately not HEADERS or BODY, which would print the
                // bearer token and patient records into logcat.
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
                    )
                }
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val api: SupabaseApiService by lazy {
        val baseUrl = if (SupabaseConfig.url.endsWith("/")) {
            "${SupabaseConfig.url}rest/v1/"
        } else {
            "${SupabaseConfig.url}/rest/v1/"
        }

        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseApiService::class.java)
    }
}
