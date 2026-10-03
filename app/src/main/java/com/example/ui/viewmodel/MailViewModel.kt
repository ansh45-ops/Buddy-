package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.SavedAccountEntity
import com.example.data.local.entity.SavedEmailEntity
import com.example.data.model.AccountSession
import com.example.data.model.EmailMessage
import com.example.data.model.MailServerNode
import com.example.data.repository.MailRepository
import com.example.util.CursedPrefixType
import com.example.util.SukunaExtractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class MainTab {
    INBOX,
    VAULT,
    SERVERS,
    SETTINGS
}

data class MailUiState(
    val currentSession: AccountSession? = null,
    val emails: List<EmailMessage> = emptyList(),
    val isSyncing: Boolean = false,
    val heartbeatTicker: Boolean = false,
    val isAutoPolling: Boolean = true,
    val pollingIntervalSec: Int = 4,
    val selectedServer: MailServerNode = MailServerNode.MAIL_TM,
    val availableDomains: List<String> = emptyList(),
    val selectedDomain: String = "",
    val prefixType: CursedPrefixType = CursedPrefixType.RANDOM_CRYPTIC,
    val customPrefix: String = "",
    val selectedEmail: EmailMessage? = null,
    val autoCopyOtp: Boolean = true,
    val hapticEnabled: Boolean = true,
    val lastDetectedOtp: String? = null,
    val toastMessage: String? = null,
    val currentTab: MainTab = MainTab.INBOX,
    val isLoadingNewInbox: Boolean = false,
    val isDomainsLoading: Boolean = false
)

class MailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MailRepository(application)
    private val _uiState = MutableStateFlow(MailUiState())
    val uiState: StateFlow<MailUiState> = _uiState.asStateFlow()

    val savedAccounts: StateFlow<List<SavedAccountEntity>> = repository.savedAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val starredEmails: StateFlow<List<SavedEmailEntity>> = repository.starredEmails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var pollingJob: Job? = null
    private val processedMessageIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            // Check for previous session
            val session = repository.initDefaultSession()
            if (session != null) {
                _uiState.value = _uiState.value.copy(
                    currentSession = session,
                    selectedServer = session.serverNode,
                    selectedDomain = session.domain
                )
                loadDomainsForServer(session.serverNode)
                observeEmails()
                startPolling()
            } else {
                // Initial generation on first boot
                loadDomainsForServer(_uiState.value.selectedServer) { domains ->
                    if (domains.isNotEmpty()) {
                        generateNewInbox(domains.first())
                    }
                }
            }
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    fun selectServer(server: MailServerNode) {
        _uiState.value = _uiState.value.copy(selectedServer = server)
        loadDomainsForServer(server)
    }

    fun selectDomain(domain: String) {
        _uiState.value = _uiState.value.copy(selectedDomain = domain)
    }

    fun setPrefixType(type: CursedPrefixType) {
        _uiState.value = _uiState.value.copy(prefixType = type)
    }

    fun setCustomPrefix(prefix: String) {
        _uiState.value = _uiState.value.copy(customPrefix = prefix)
    }

    fun setAutoPolling(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAutoPolling = enabled)
        if (enabled) {
            startPolling()
        } else {
            pollingJob?.cancel()
        }
    }

    fun setPollingInterval(seconds: Int) {
        _uiState.value = _uiState.value.copy(pollingIntervalSec = seconds)
        if (_uiState.value.isAutoPolling) {
            startPolling()
        }
    }

    fun setAutoCopyOtp(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoCopyOtp = enabled)
    }

    fun setHapticEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(hapticEnabled = enabled)
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    fun selectEmail(email: EmailMessage?) {
        _uiState.value = _uiState.value.copy(selectedEmail = email)
        if (email != null && !email.isRead) {
            viewModelScope.launch {
                repository.markAsRead(email.id)
            }
        }
    }

    fun loadDomainsForServer(server: MailServerNode, onLoaded: ((List<String>) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDomainsLoading = true)
            val domains = repository.getAvailableDomains(server)
            val chosenDomain = if (domains.contains(_uiState.value.selectedDomain)) {
                _uiState.value.selectedDomain
            } else {
                domains.firstOrNull().orEmpty()
            }
            _uiState.value = _uiState.value.copy(
                availableDomains = domains,
                selectedDomain = chosenDomain,
                isDomainsLoading = false
            )
            onLoaded?.invoke(domains)
        }
    }

    fun generateNewInbox(overrideDomain: String? = null) {
        val targetDomain = overrideDomain ?: _uiState.value.selectedDomain.ifBlank {
            _uiState.value.availableDomains.firstOrNull() ?: "mail.tm"
        }
        val server = _uiState.value.selectedServer
        val prefix = _uiState.value.prefixType
        val custom = _uiState.value.customPrefix

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingNewInbox = true)
            val result = repository.createNewAccount(
                serverNode = server,
                targetDomain = targetDomain,
                prefixType = prefix,
                customPrefix = custom,
                aliasTitle = if (custom.isNotBlank()) custom else prefix.label
            )

            result.onSuccess { session ->
                processedMessageIds.clear()
                _uiState.value = _uiState.value.copy(
                    currentSession = session,
                    isLoadingNewInbox = false,
                    selectedEmail = null,
                    toastMessage = "Domain expansion active: ${session.email}"
                )
                observeEmails()
                startPolling()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoadingNewInbox = false,
                    toastMessage = "Summoning failed: ${err.localizedMessage}"
                )
            }
        }
    }

    fun switchAccount(email: String) {
        viewModelScope.launch {
            val res = repository.switchAccount(email)
            res.onSuccess { session ->
                processedMessageIds.clear()
                _uiState.value = _uiState.value.copy(
                    currentSession = session,
                    selectedServer = session.serverNode,
                    selectedDomain = session.domain,
                    selectedEmail = null,
                    currentTab = MainTab.INBOX,
                    toastMessage = "Switched to: ${session.email}"
                )
                observeEmails()
                startPolling()
            }.onFailure {
                _uiState.value = _uiState.value.copy(toastMessage = "Failed to switch vessel: ${it.message}")
            }
        }
    }

    fun deleteCurrentAccount() {
        viewModelScope.launch {
            val res = repository.deleteCurrentAccount()
            res.onSuccess {
                _uiState.value = _uiState.value.copy(
                    currentSession = null,
                    selectedEmail = null,
                    toastMessage = "Domain dismantled safely."
                )
                // Automatically summon new inbox
                loadDomainsForServer(_uiState.value.selectedServer) { domains ->
                    if (domains.isNotEmpty()) {
                        generateNewInbox(domains.first())
                    }
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(toastMessage = "Purge error: ${it.message}")
            }
        }
    }

    fun syncMessages(manual: Boolean = false) {
        viewModelScope.launch {
            if (manual) {
                _uiState.value = _uiState.value.copy(isSyncing = true)
            }
            val res = repository.syncMessages()
            res.onSuccess { messages ->
                // Check for new incoming packets to trigger OTP detection & clipboard injection
                for (msg in messages) {
                    if (!processedMessageIds.contains(msg.id)) {
                        processedMessageIds.add(msg.id)
                        
                        val otp = msg.otpCode
                        if (!otp.isNullOrBlank()) {
                            _uiState.value = _uiState.value.copy(lastDetectedOtp = otp)
                            if (_uiState.value.autoCopyOtp) {
                                copyToClipboard(otp, "OTP Code")
                            }
                            if (_uiState.value.hapticEnabled) {
                                triggerHaptic()
                            }
                        }
                    }
                }
                if (manual) {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        toastMessage = if (messages.isEmpty()) "Mailbox empty (No new packets)" else "${messages.size} packets synchronized"
                    )
                }
            }.onFailure {
                if (manual) {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        toastMessage = "Resync warning: ${it.localizedMessage}"
                    )
                }
            }
        }
    }

    fun toggleStar(emailId: String, currentStarred: Boolean) {
        viewModelScope.launch {
            repository.toggleStar(emailId, currentStarred)
            // Update selected email if open
            if (_uiState.value.selectedEmail?.id == emailId) {
                _uiState.value = _uiState.value.copy(
                    selectedEmail = _uiState.value.selectedEmail?.copy(isStarred = !currentStarred)
                )
            }
        }
    }

    fun deleteEmail(emailId: String) {
        viewModelScope.launch {
            repository.deleteEmail(emailId)
            if (_uiState.value.selectedEmail?.id == emailId) {
                _uiState.value = _uiState.value.copy(selectedEmail = null)
            }
            _uiState.value = _uiState.value.copy(toastMessage = "Packet destroyed.")
        }
    }

    private fun observeEmails() {
        viewModelScope.launch {
            repository.getEmailsFlowForCurrentAccount().collect { list ->
                _uiState.value = _uiState.value.copy(emails = list)
            }
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive && _uiState.value.isAutoPolling) {
                _uiState.value = _uiState.value.copy(
                    heartbeatTicker = !_uiState.value.heartbeatTicker
                )
                if (_uiState.value.currentSession != null) {
                    repository.syncMessages().onSuccess { messages ->
                        for (msg in messages) {
                            if (!processedMessageIds.contains(msg.id)) {
                                processedMessageIds.add(msg.id)
                                val otp = msg.otpCode
                                if (!otp.isNullOrBlank()) {
                                    _uiState.value = _uiState.value.copy(lastDetectedOtp = otp)
                                    if (_uiState.value.autoCopyOtp) {
                                        copyToClipboard(otp, "OTP Code")
                                    }
                                    if (_uiState.value.hapticEnabled) {
                                        triggerHaptic()
                                    }
                                }
                            }
                        }
                    }
                }
                delay((_uiState.value.pollingIntervalSec * 1000L).coerceAtLeast(2000L))
            }
        }
    }

    fun copyToClipboard(text: String, label: String = "Copied text") {
        try {
            val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            _uiState.value = _uiState.value.copy(toastMessage = "Copied $label to clipboard!")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerHaptic() {
        try {
            val app = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(100)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
