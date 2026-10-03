package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HydraResponse<T>(
    @Json(name = "hydra:member") val member: List<T> = emptyList(),
    @Json(name = "hydra:totalItems") val totalItems: Int = 0
)

@JsonClass(generateAdapter = true)
data class DomainDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "domain") val domain: String = "",
    @Json(name = "isActive") val isActive: Boolean = true,
    @Json(name = "isPrivate") val isPrivate: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CreateAccountRequest(
    @Json(name = "address") val address: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class AccountDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "address") val address: String = "",
    @Json(name = "quota") val quota: Long = 0L,
    @Json(name = "used") val used: Long = 0L,
    @Json(name = "isDisabled") val isDisabled: Boolean = false,
    @Json(name = "isDeleted") val isDeleted: Boolean = false,
    @Json(name = "createdAt") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class TokenRequest(
    @Json(name = "address") val address: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "id") val id: String = "",
    @Json(name = "token") val token: String = ""
)

@JsonClass(generateAdapter = true)
data class EmailSenderDto(
    @Json(name = "address") val address: String = "",
    @Json(name = "name") val name: String? = null
)

@JsonClass(generateAdapter = true)
data class EmailRecipientDto(
    @Json(name = "address") val address: String = "",
    @Json(name = "name") val name: String? = null
)

@JsonClass(generateAdapter = true)
data class MessageSummaryDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "accountId") val accountId: String? = null,
    @Json(name = "msgid") val msgid: String? = null,
    @Json(name = "from") val from: EmailSenderDto = EmailSenderDto(),
    @Json(name = "to") val to: List<EmailRecipientDto> = emptyList(),
    @Json(name = "subject") val subject: String = "",
    @Json(name = "intro") val intro: String = "",
    @Json(name = "seen") val seen: Boolean = false,
    @Json(name = "isDeleted") val isDeleted: Boolean = false,
    @Json(name = "hasAttachments") val hasAttachments: Boolean = false,
    @Json(name = "size") val size: Long = 0L,
    @Json(name = "createdAt") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class MessageAttachmentDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "filename") val filename: String = "",
    @Json(name = "contentType") val contentType: String = "",
    @Json(name = "size") val size: Long = 0L,
    @Json(name = "downloadUrl") val downloadUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class MessageDetailDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "accountId") val accountId: String? = null,
    @Json(name = "msgid") val msgid: String? = null,
    @Json(name = "from") val from: EmailSenderDto = EmailSenderDto(),
    @Json(name = "to") val to: List<EmailRecipientDto> = emptyList(),
    @Json(name = "subject") val subject: String = "",
    @Json(name = "intro") val intro: String = "",
    @Json(name = "seen") val seen: Boolean = false,
    @Json(name = "isDeleted") val isDeleted: Boolean = false,
    @Json(name = "hasAttachments") val hasAttachments: Boolean = false,
    @Json(name = "size") val size: Long = 0L,
    @Json(name = "downloadUrl") val downloadUrl: String? = null,
    @Json(name = "text") val text: String? = null,
    @Json(name = "html") val html: List<String>? = null,
    @Json(name = "attachments") val attachments: List<MessageAttachmentDto>? = null,
    @Json(name = "createdAt") val createdAt: String = ""
)
