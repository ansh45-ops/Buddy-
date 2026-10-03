package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.QrCodeDialog
import com.example.ui.screens.EmailDetailScreen
import com.example.ui.screens.InboxTabScreen
import com.example.ui.screens.ServersTabScreen
import com.example.ui.screens.SettingsTabScreen
import com.example.ui.screens.VaultTabScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SukunaRed
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MailViewModel
import com.example.ui.viewmodel.MainTab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MailViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedAccounts by viewModel.savedAccounts.collectAsStateWithLifecycle()
    val starredEmails by viewModel.starredEmails.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showQrDialog by remember { mutableStateOf(false) }
    var showDismantleDialog by remember { mutableStateOf(false) }

    // Handle toast messages
    LaunchedEffect(uiState.toastMessage) {
        val msg = uiState.toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    // Detail Screen (full screen with BackHandler)
    if (uiState.selectedEmail != null) {
        EmailDetailScreen(
            email = uiState.selectedEmail!!,
            onBack = { viewModel.selectEmail(null) },
            onToggleStar = { id, isStarred -> viewModel.toggleStar(id, isStarred) },
            onDelete = { id -> viewModel.deleteEmail(id) },
            onCopyText = { text, label -> viewModel.copyToClipboard(text, label) }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val unreadCount = uiState.emails.count { !it.isRead }
            BottomNavBar(
                currentTab = uiState.currentTab,
                unreadCount = unreadCount,
                vaultCount = savedAccounts.size,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentTab) {
                MainTab.INBOX -> {
                    InboxTabScreen(
                        session = uiState.currentSession,
                        emails = uiState.emails,
                        isSyncing = uiState.isSyncing,
                        heartbeatTicker = uiState.heartbeatTicker,
                        isLoadingNewInbox = uiState.isLoadingNewInbox,
                        lastDetectedOtp = uiState.lastDetectedOtp,
                        onCopyEmail = { viewModel.copyToClipboard(it, "Email address") },
                        onSyncNow = { viewModel.syncMessages(manual = true) },
                        onSummonNew = { viewModel.generateNewInbox() },
                        onShowQr = { showQrDialog = true },
                        onDismantleAccount = { showDismantleDialog = true },
                        onEmailClick = { viewModel.selectEmail(it) },
                        onToggleStar = { id, isStarred -> viewModel.toggleStar(id, isStarred) },
                        onCopyOtp = { viewModel.copyToClipboard(it, "OTP Code") }
                    )
                }

                MainTab.VAULT -> {
                    VaultTabScreen(
                        currentEmail = uiState.currentSession?.email,
                        savedAccounts = savedAccounts,
                        starredEmails = starredEmails,
                        onSwitchAccount = { viewModel.switchAccount(it) },
                        onCopyEmail = { viewModel.copyToClipboard(it, "Email address") },
                        onEmailClick = { viewModel.selectEmail(it) },
                        onToggleStar = { id, isStarred -> viewModel.toggleStar(id, isStarred) },
                        onCopyOtp = { viewModel.copyToClipboard(it, "OTP Code") }
                    )
                }

                MainTab.SERVERS -> {
                    ServersTabScreen(
                        selectedServer = uiState.selectedServer,
                        availableDomains = uiState.availableDomains,
                        selectedDomain = uiState.selectedDomain,
                        prefixType = uiState.prefixType,
                        customPrefix = uiState.customPrefix,
                        isDomainsLoading = uiState.isDomainsLoading,
                        isLoadingNewInbox = uiState.isLoadingNewInbox,
                        onSelectServer = { viewModel.selectServer(it) },
                        onSelectDomain = { viewModel.selectDomain(it) },
                        onSelectPrefixType = { viewModel.setPrefixType(it) },
                        onCustomPrefixChange = { viewModel.setCustomPrefix(it) },
                        onRefreshDomains = { viewModel.loadDomainsForServer(uiState.selectedServer) },
                        onSummonInbox = { viewModel.generateNewInbox() }
                    )
                }

                MainTab.SETTINGS -> {
                    SettingsTabScreen(
                        isAutoPolling = uiState.isAutoPolling,
                        pollingIntervalSec = uiState.pollingIntervalSec,
                        autoCopyOtp = uiState.autoCopyOtp,
                        hapticEnabled = uiState.hapticEnabled,
                        savedAccountsCount = savedAccounts.size,
                        starredCount = starredEmails.size,
                        onSetAutoPolling = { viewModel.setAutoPolling(it) },
                        onSetPollingInterval = { viewModel.setPollingInterval(it) },
                        onSetAutoCopyOtp = { viewModel.setAutoCopyOtp(it) },
                        onSetHapticEnabled = { viewModel.setHapticEnabled(it) },
                        onPurgeAllMemory = { viewModel.deleteCurrentAccount() }
                    )
                }
            }
        }
    }

    // QR Code Dialog
    if (showQrDialog && uiState.currentSession != null) {
        QrCodeDialog(
            email = uiState.currentSession!!.email,
            onDismiss = { showQrDialog = false },
            onCopy = { viewModel.copyToClipboard(it, "Email address") }
        )
    }

    // Dismantle Confirmation Dialog
    if (showDismantleDialog) {
        AlertDialog(
            onDismissRequest = { showDismantleDialog = false },
            title = {
                Text(
                    text = "Dismantle Current Vessel?",
                    color = SukunaRed,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will delete the current temporary address from the remote server node and replace it with a new cursed alias.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDismantleDialog = false
                        viewModel.deleteCurrentAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SukunaRed)
                ) {
                    Text("Dismantle & Summon New", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDismantleDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }
}
