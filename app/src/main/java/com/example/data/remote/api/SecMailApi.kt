package com.example.data.remote.api

import com.example.data.remote.model.SecMailMessageDetail
import com.example.data.remote.model.SecMailMessageSummary
import retrofit2.http.GET
import retrofit2.http.Query

interface SecMailApi {

    @GET("?action=getDomainList")
    suspend fun getDomainList(): List<String>

    @GET("?action=genRandomMailbox")
    suspend fun genRandomMailbox(
        @Query("count") count: Int = 1
    ): List<String>

    @GET("?action=getMessages")
    suspend fun getMessages(
        @Query("login") login: String,
        @Query("domain") domain: String
    ): List<SecMailMessageSummary>

    @GET("?action=readMessage")
    suspend fun readMessage(
        @Query("login") login: String,
        @Query("domain") domain: String,
        @Query("id") id: Int
    ): SecMailMessageDetail
}
