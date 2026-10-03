package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_accounts")
data class SavedAccountEntity(
    @PrimaryKey
    val email: String,
    val password: String,
    val token: String? = null,
    val accountId: String? = null,
    val serverNodeName: String,
    val domain: String,
    val createdAt: Long = System.currentTimeMillis(),
    val aliasName: String = "Malevolent Alias",
    val isCurrent: Boolean = false
)
