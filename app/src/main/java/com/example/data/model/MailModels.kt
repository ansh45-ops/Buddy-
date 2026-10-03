package com.example.data.model

enum class MailServerNode(
    val title: String,
    val subtitle: String,
    val baseUrl: String,
    val iconEmoji: String,
    val isHydra: Boolean
) {
    MAIL_TM(
        title = "Mail.tm Core",
        subtitle = "Highly stable direct REST infrastructure node",
        baseUrl = "https://api.mail.tm/",
        iconEmoji = "⚡",
        isHydra = true
    ),
    MAIL_GW(
        title = "Mail.gw Proxy",
        subtitle = "Fast delivery open-source nexus proxy",
        baseUrl = "https://api.mail.gw/",
        iconEmoji = "🌀",
        isHydra = true
    ),
    SEC_MAIL(
        title = "1SecMail Gateway",
        subtitle = "Stateless zero-auth rapid ephemeral server",
        baseUrl = "https://www.1secmail.com/api/v1/",
        iconEmoji = "🔥",
        isHydra = false
    );

    companion object {
        fun fromName(name: String?): MailServerNode {
            return entries.firstOrNull { it.name == name } ?: MAIL_TM
        }
    }
}

data class AccountSession(
    val email: String,
    val password: String,
    val token: String? = null,
    val accountId: String? = null,
    val serverNode: MailServerNode,
    val domain: String,
    val createdAt: Long = System.currentTimeMillis(),
    val aliasName: String = "Malevolent Alias"
) {
    val username: String
        get() = email.substringBefore("@")
}

data class EmailMessage(
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
    val actionableLinks: List<String> = emptyList(),
    val hasAttachments: Boolean = false,
    val sizeBytes: Long = 0L
)
