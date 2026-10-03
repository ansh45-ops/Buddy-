package com.example.data.remote

import com.example.data.remote.api.MailHydraApi
import com.example.data.remote.api.SecMailApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val hydraApiCache = mutableMapOf<String, MailHydraApi>()
    private var secMailApiInstance: SecMailApi? = null

    fun getHydraApi(baseUrl: String): MailHydraApi {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return hydraApiCache.getOrPut(normalizedUrl) {
            Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(MailHydraApi::class.java)
        }
    }

    fun getSecMailApi(): SecMailApi {
        return secMailApiInstance ?: synchronized(this) {
            secMailApiInstance ?: Retrofit.Builder()
                .baseUrl("https://www.1secmail.com/api/v1/")
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(SecMailApi::class.java).also { secMailApiInstance = it }
        }
    }
}
