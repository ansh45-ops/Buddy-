package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SecMailMessageSummary(
    @Json(name = "id") val id: Int,
    @Json(name = "from") val from: String,
    @Json(name = "subject") val subject: String,
    @Json(name = "date") val date: String
)

@JsonClass(generateAdapter = true)
data class SecMailMessageDetail(
    @Json(name = "id") val id: Int,
    @Json(name = "from") val from: String,
    @Json(name = "subject") val subject: String,
    @Json(name = "date") val date: String,
    @Json(name = "body") val body: String? = null,
    @Json(name = "textBody") val textBody: String? = null,
    @Json(name = "htmlBody") val htmlBody: String? = null
)
