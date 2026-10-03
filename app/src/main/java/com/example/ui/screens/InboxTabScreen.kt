package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AccountSession
import com.example.data.model.EmailMessage
import com.example.ui.components.DomainBanner
import com.example.ui.components.EmailItemCard
import com.example.ui.components.OtpBanner
import com.example.ui.theme.CursedGold
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SukunaRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun InboxTabScreen(
    session: AccountSession?,
    emails: List<EmailMessage>,
    isSyncing: Boolean,
    heartbeatTicker: Boolean,
    isLoadingNewInbox: Boolean,
    lastDetectedOtp: String?,
    onCopyEmail: (String) -> Unit,
    onSyncNow: () -> Unit,
    onSummonNew: () -> Unit,
    onShowQr: () -> Unit,
    onDismantleAccount: () -> Unit,
    onEmailClick: (EmailMessage) -> Unit,
    onToggleStar: (String, Boolean) -> Unit,
    onCopyOtp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("inbox_lazy_column")
    ) {
        // Domain Banner Header
        item {
            DomainBanner(
                session = session,
                isSyncing = isSyncing,
                heartbeatTicker = heartbeatTicker,
                isLoadingNewInbox = isLoadingNewInbox,
                onCopyEmail = onCopyEmail,
                onSyncNow = onSyncNow,
                onSummonNew = onSummonNew,
                onShowQr = onShowQr,
                onDismantleAccount = onDismantleAccount
            )
        }

        // OTP Detection Banner
        item {
            OtpBanner(
                otpCode = lastDetectedOtp,
                onCopy = onCopyOtp
            )
        }

        // Live Packets Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "LIVE PACKETS",
                        color = CursedGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${emails.size})",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (isSyncing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = CursedGold,
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scanning...",
                            color = CursedGold,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    IconButton(
                        onClick = onSyncNow,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Empty state
        if (emails.isEmpty() && !isLoadingNewInbox) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(ObsidianSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "Inbox Empty",
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Mailbox Void: Awaiting Inbound Packets",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Send confirmation codes, newsletters, or signups to this address. Packets will appear here in real-time.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Message List
        items(emails, key = { it.id }) { email ->
            EmailItemCard(
                email = email,
                onClick = { onEmailClick(email) },
                onToggleStar = { onToggleStar(email.id, email.isStarred) },
                onCopyOtp = onCopyOtp
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
