package com.example.data.remote.api

import com.example.data.remote.model.AccountDto
import com.example.data.remote.model.CreateAccountRequest
import com.example.data.remote.model.DomainDto
import com.example.data.remote.model.HydraResponse
import com.example.data.remote.model.MessageDetailDto
import com.example.data.remote.model.MessageSummaryDto
import com.example.data.remote.model.TokenRequest
import com.example.data.remote.model.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MailHydraApi {

    @GET("domains")
    suspend fun getDomains(): HydraResponse<DomainDto>

    @POST("accounts")
    suspend fun createAccount(
        @Body request: CreateAccountRequest
    ): Response<AccountDto>

    @POST("token")
    suspend fun getToken(
        @Body request: TokenRequest
    ): Response<TokenResponse>

    @GET("me")
    suspend fun getMe(
        @Header("Authorization") authHeader: String
    ): Response<AccountDto>

    @GET("messages")
    suspend fun getMessages(
        @Header("Authorization") authHeader: String,
        @Query("page") page: Int = 1
    ): HydraResponse<MessageSummaryDto>

    @GET("messages/{id}")
    suspend fun getMessageDetail(
        @Header("Authorization") authHeader: String,
        @Path("id") messageId: String
    ): MessageDetailDto

    @DELETE("messages/{id}")
    suspend fun deleteMessage(
        @Header("Authorization") authHeader: String,
        @Path("id") messageId: String
    ): Response<Unit>

    @DELETE("accounts/{id}")
    suspend fun deleteAccount(
        @Header("Authorization") authHeader: String,
        @Path("id") accountId: String
    ): Response<Unit>
}
