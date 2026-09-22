package com.ntando.ivu.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton client configuring and providing the Retrofit instance for the IVU application.
 * Configures OkHttpClient with [AuthInterceptor] for JWT Bearer token authentication,
 * [HttpLoggingInterceptor] for HTTP request/response inspection, and extended timeouts
 * (60s) to handle Render free-tier cold starts.
 */
object ApiClient {

    private const val TAG = "ApiClient"
    private const val BASE_URL = "https://ivu.onrender.com/"

    /**
     * HTTP Logging interceptor configured to log full request and response bodies.
     */
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("OkHttp", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * Custom [OkHttpClient] equipped with authentication and logging interceptors,
     * along with 60-second timeouts to accommodate Render server sleep wake-ups.
     */
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .addInterceptor(loggingInterceptor)
        // Render free tier can take 30-60s to wake from sleep - allow for that
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Lazy-initialized instance of [IvuApiService] created via Retrofit.
     */
    val apiService: IvuApiService by lazy {
        Log.i(TAG, "Initializing Retrofit IvuApiService with BASE_URL=$BASE_URL")
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IvuApiService::class.java)
    }
}
