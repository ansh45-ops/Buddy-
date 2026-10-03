package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.SavedAccountEntity
import com.example.data.local.entity.SavedEmailEntity
import com.example.data.model.AccountSession
import com.example.data.model.EmailMessage
import com.example.data.model.MailServerNode
import com.example.data.remote.NetworkClient
import com.example.data.remote.model.CreateAccountRequest
import com.example.data.remote.model.TokenRequest
import com.example.util.CursedPrefixType
import com.example.util.SukunaExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class MailRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val accountDao = db.savedAccountDao()
    private val emailDao = db.savedEmailDao()

    private val _currentSession = MutableStateFlow<AccountSession?>(null)
    val currentSession: StateFlow<AccountSession?> = _currentSession.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val savedAccounts: Flow<List<SavedAccountEntity>> = accountDao.getAllAccounts()
    val starredEmails: Flow<List<SavedEmailEntity>> = emailDao.getStarredEmails()

    suspend fun initDefaultSession(): AccountSession? = withContext(Dispatchers.IO) {
        val current = accountDao.getCurrentAccount()
        if (current != null) {
            val session = current.toSession()
            _currentSession.value = session
            return@withContext session
        }
        null
    }

    suspend fun getAvailableDomains(serverNode: MailServerNode): List<String> = withContext(Dispatchers.IO) {
        try {
            if (serverNode.isHydra) {
                val api = NetworkClient.getHydraApi(serverNode.baseUrl)
                val response = api.getDomains()
                val list = response.member.filter { it.isActive }.map { it.domain }
                if (list.isNotEmpty()) return@withContext list
            } else {
                val api = NetworkClient.getSecMailApi()
                val list = api.getDomainList()
                if (list.isNotEmpty()) return@withContext list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Fallback domains if network blips
        when (serverNode) {
            MailServerNode.MAIL_TM -> listOf("mail.tm", "bugfoo.com", "vintomaper.com")
            MailServerNode.MAIL_GW -> listOf("mail.gw", "wholesome.cloud")
            MailServerNode.SEC_MAIL -> listOf("1secmail.com", "1secmail.net", "1secmail.org")
        }
    }

    suspend fun createNewAccount(
        serverNode: MailServerNode,
        targetDomain: String,
        prefixType: CursedPrefixType = CursedPrefixType.RANDOM_CRYPTIC,
        customPrefix: String? = null,
        aliasTitle: String = "Malevolent Alias"
    ): Result<AccountSession> = withContext(Dispatchers.IO) {
        try {
            val username = SukunaExtractor.generateUsername(prefixType, customPrefix)
            val fullEmail = "$username@$targetDomain"
            val password = SukunaExtractor.generatePassword()

            if (serverNode.isHydra) {
                val api = NetworkClient.getHydraApi(serverNode.baseUrl)
                
                // 1. Create account
                val accountRes = api.createAccount(CreateAccountRequest(address = fullEmail, password = password))
                if (!accountRes.isSuccessful) {
                    val err = accountRes.errorBody()?.string() ?: "Failed to instantiate domain alias"
                    return@withContext Result.failure(Exception("Registration rejected: $err"))
                }
                val accountBody = accountRes.body()
                val accountId = accountBody?.id

                // 2. Fetch JWT token
                val tokenRes = api.getToken(TokenRequest(address = fullEmail, password = password))
                if (!tokenRes.isSuccessful) {
                    val err = tokenRes.errorBody()?.string() ?: "Failed to generate cursed token"
                    return@withContext Result.failure(Exception("Token failure: $err"))
                }
                val token = tokenRes.body()?.token ?: return@withContext Result.failure(Exception("Empty token returned"))

                val session = AccountSession(
                    email = fullEmail,
                    password = password,
                    token = token,
                    accountId = accountId,
                    serverNode = serverNode,
                    domain = targetDomain,
                    aliasName = aliasTitle
                )

                // Save to Room
                accountDao.clearCurrentFlags()
                accountDao.insertOrUpdate(session.toEntity(isCurrent = true))
                _currentSession.value = session
                Result.success(session)
            } else {
                // 1SecMail (stateless, zero-auth)
                val session = AccountSession(
                    email = fullEmail,
                    password = password,
                    token = null,
                    accountId = null,
                    serverNode = serverNode,
                    domain = targetDomain,
                    aliasName = aliasTitle
                )

                accountDao.clearCurrentFlags()
                accountDao.insertOrUpdate(session.toEntity(isCurrent = true))
                _currentSession.value = session
                Result.success(session)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun switchAccount(email: String): Result<AccountSession> = withContext(Dispatchers.IO) {
        try {
            val entity = accountDao.getAccountByEmail(email)
                ?: return@withContext Result.failure(Exception("Account not found"))
            accountDao.clearCurrentFlags()
            accountDao.setCurrentAccount(email)
            val session = entity.toSession()
            _currentSession.value = session
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getEmailsFlowForCurrentAccount(): Flow<List<EmailMessage>> {
        return emailDao.getEmailsForAccount(_currentSession.value?.email ?: "").map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun syncMessages(): Result<List<EmailMessage>> = withContext(Dispatchers.IO) {
        val session = _currentSession.value ?: return@withContext Result.failure(Exception("No active mailbox session"))
        _isSyncing.value = true

        try {
            val messages = mutableListOf<EmailMessage>()

            if (session.serverNode.isHydra) {
                val token = session.token
                if (token.isNullOrEmpty()) {
                    _isSyncing.value = false
                    return@withContext Result.failure(Exception("Missing session authorization token"))
                }
                val authHeader = "Bearer $token"
                val api = NetworkClient.getHydraApi(session.serverNode.baseUrl)
                val summaryList = api.getMessages(authHeader, page = 1).member

                for (summary in summaryList) {
                    // Check if already saved in Room
                    val existing = emailDao.getEmailById(summary.id)
                    if (existing != null && existing.bodyText.isNotEmpty()) {
                        messages.add(existing.toDomain())
                        continue
                    }

                    // Query deep content
                    var bodyText = summary.intro
                    var bodyHtml = ""
                    try {
                        val detail = api.getMessageDetail(authHeader, summary.id)
                        bodyText = detail.text?.ifBlank { detail.intro } ?: detail.intro
                        bodyHtml = detail.html?.joinToString("\n") ?: ""
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val sender = summary.from.address
                    val senderName = summary.from.name ?: sender.substringBefore("@")
                    val subject = summary.subject.ifBlank { "(No Subject)" }
                    val otp = SukunaExtractor.extractOtp(subject, bodyText)
                    val links = SukunaExtractor.extractLinks("$bodyText\n$bodyHtml")

                    val email = EmailMessage(
                        id = summary.id,
                        accountEmail = session.email,
                        senderAddress = sender,
                        senderName = senderName,
                        subject = subject,
                        intro = summary.intro,
                        bodyText = bodyText,
                        bodyHtml = bodyHtml,
                        receivedAt = parseIsoDate(summary.createdAt),
                        isRead = summary.seen,
                        isStarred = existing?.isStarred ?: false,
                        otpCode = otp,
                        actionableLinks = links,
                        hasAttachments = summary.hasAttachments,
                        sizeBytes = summary.size
                    )
                    messages.add(email)
                }
            } else {
                // 1SecMail
                val api = NetworkClient.getSecMailApi()
                val login = session.username
                val domain = session.domain
                val summaries = api.getMessages(login, domain)

                for (summary in summaries) {
                    val strId = "1sec_${summary.id}"
                    val existing = emailDao.getEmailById(strId)
                    if (existing != null && existing.bodyText.isNotEmpty()) {
                        messages.add(existing.toDomain())
                        continue
                    }

                    var bodyText = ""
                    var bodyHtml = ""
                    try {
                        val detail = api.readMessage(login, domain, summary.id)
                        bodyText = detail.textBody ?: detail.body ?: ""
                        bodyHtml = detail.htmlBody ?: ""
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val sender = summary.from
                    val senderName = sender.substringBefore("@")
                    val subject = summary.subject.ifBlank { "(No Subject)" }
                    val otp = SukunaExtractor.extractOtp(subject, bodyText)
                    val links = SukunaExtractor.extractLinks("$bodyText\n$bodyHtml")

                    val email = EmailMessage(
                        id = strId,
                        accountEmail = session.email,
                        senderAddress = sender,
                        senderName = senderName,
                        subject = subject,
                        intro = bodyText.take(120),
                        bodyText = bodyText,
                        bodyHtml = bodyHtml,
                        receivedAt = parseSecMailDate(summary.date),
                        isRead = false,
                        isStarred = existing?.isStarred ?: false,
                        otpCode = otp,
                        actionableLinks = links,
                        hasAttachments = false,
                        sizeBytes = 0L
                    )
                    messages.add(email)
                }
            }

            // Save to Room cache
            if (messages.isNotEmpty()) {
                emailDao.insertEmails(messages.map { it.toEntity() })
            }

            _isSyncing.value = false
            Result.success(messages)
        } catch (e: Exception) {
            e.printStackTrace()
            _isSyncing.value = false
            Result.failure(e)
        }
    }

    suspend fun toggleStar(messageId: String, currentStarred: Boolean) = withContext(Dispatchers.IO) {
        emailDao.updateStarred(messageId, !currentStarred)
    }

    suspend fun markAsRead(messageId: String) = withContext(Dispatchers.IO) {
        emailDao.markAsRead(messageId)
    }

    suspend fun deleteEmail(messageId: String) = withContext(Dispatchers.IO) {
        val session = _currentSession.value
        if (session != null && session.serverNode.isHydra && session.token != null) {
            try {
                val api = NetworkClient.getHydraApi(session.serverNode.baseUrl)
                api.deleteMessage("Bearer ${session.token}", messageId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        emailDao.deleteEmail(messageId)
    }

    suspend fun deleteCurrentAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        val session = _currentSession.value ?: return@withContext Result.failure(Exception("No active account"))
        try {
            if (session.serverNode.isHydra && session.token != null && session.accountId != null) {
                val api = NetworkClient.getHydraApi(session.serverNode.baseUrl)
                api.deleteAccount("Bearer ${session.token}", session.accountId)
            }
            emailDao.clearEmailsForAccount(session.email)
            accountDao.deleteAccount(session.email)

            val nextAccount = accountDao.getCurrentAccount()
            if (nextAccount != null) {
                _currentSession.value = nextAccount.toSession()
            } else {
                _currentSession.value = null
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAccountEmails(email: String) = withContext(Dispatchers.IO) {
        emailDao.clearEmailsForAccount(email)
    }

    private fun parseIsoDate(iso: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            format.parse(iso.substringBefore("."))?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun parseSecMailDate(dateStr: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            format.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun SavedAccountEntity.toSession(): AccountSession {
        return AccountSession(
            email = email,
            password = password,
            token = token,
            accountId = accountId,
            serverNode = MailServerNode.fromName(serverNodeName),
            domain = domain,
            createdAt = createdAt,
            aliasName = aliasName
        )
    }

    private fun AccountSession.toEntity(isCurrent: Boolean): SavedAccountEntity {
        return SavedAccountEntity(
            email = email,
            password = password,
            token = token,
            accountId = accountId,
            serverNodeName = serverNode.name,
            domain = domain,
            createdAt = createdAt,
            aliasName = aliasName,
            isCurrent = isCurrent
        )
    }

    private fun SavedEmailEntity.toDomain(): EmailMessage {
        val linksList = if (actionableLinks.isBlank()) emptyList() else actionableLinks.split(",,,")
        return EmailMessage(
            id = id,
            accountEmail = accountEmail,
            senderAddress = senderAddress,
            senderName = senderName,
            subject = subject,
            intro = intro,
            bodyText = bodyText,
            bodyHtml = bodyHtml,
            receivedAt = receivedAt,
            isRead = isRead,
            isStarred = isStarred,
            otpCode = otpCode,
            actionableLinks = linksList,
            hasAttachments = false,
            sizeBytes = 0L
        )
    }

    private fun EmailMessage.toEntity(): SavedEmailEntity {
        return SavedEmailEntity(
            id = id,
            accountEmail = accountEmail,
            senderAddress = senderAddress,
            senderName = senderName,
            subject = subject,
            intro = intro,
            bodyText = bodyText,
            bodyHtml = bodyHtml,
            receivedAt = receivedAt,
            isRead = isRead,
            isStarred = isStarred,
            otpCode = otpCode,
            actionableLinks = actionableLinks.joinToString(",,,")
        )
    }
}
