package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_emails")
data class SavedEmailEntity(
    @PrimaryKey
    val id: String,
    val accountEmail: String,
    val senderAddress: String,
    val senderName: String,
    val subject: String,
    val intro: String,
    val bodyText: String,
    val bodyHtml: String,
    val receivedAt: Long,
    val isRead: Boolean,
    val isStarred: Boolean = false,
    val otpCode: String? = null,
    val actionableLinks: String = "" // comma separated
)
