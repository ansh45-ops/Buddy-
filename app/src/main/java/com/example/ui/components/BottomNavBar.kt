package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CursedGold
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.SukunaRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.MainTab

@Composable
fun BottomNavBar(
    currentTab: MainTab,
    unreadCount: Int,
    vaultCount: Int,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .border(width = 0.5.dp, color = ObsidianBorder),
        containerColor = ObsidianBackground,
        tonalElevation = 0.dp
    ) {
        // Inbox Tab
        NavigationBarItem(
            selected = currentTab == MainTab.INBOX,
            onClick = { onTabSelected(MainTab.INBOX) },
            icon = {
                if (unreadCount > 0) {
                    BadgedBox(badge = {
                        Badge(containerColor = SukunaRed, contentColor = TextPrimary) {
                            Text(text = "$unreadCount", fontSize = 10.sp)
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Inbox, contentDescription = "Inbox")
                    }
                } else {
                    Icon(imageVector = Icons.Default.Inbox, contentDescription = "Inbox")
                }
            },
            label = {
                Text(
                    text = "Inbox",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == MainTab.INBOX) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SukunaRed,
                selectedTextColor = SukunaRed,
                indicatorColor = SukunaRed.copy(alpha = 0.15f),
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            ),
            modifier = Modifier.testTag("tab_inbox")
        )

        // Vault Tab
        NavigationBarItem(
            selected = currentTab == MainTab.VAULT,
            onClick = { onTabSelected(MainTab.VAULT) },
            icon = {
                if (vaultCount > 0) {
                    BadgedBox(badge = {
                        Badge(containerColor = CursedGold, contentColor = ObsidianBackground) {
                            Text(text = "$vaultCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Storage, contentDescription = "Vault")
                    }
                } else {
                    Icon(imageVector = Icons.Default.Storage, contentDescription = "Vault")
                }
            },
            label = {
                Text(
                    text = "Vault",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == MainTab.VAULT) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CursedGold,
                selectedTextColor = CursedGold,
                indicatorColor = CursedGold.copy(alpha = 0.15f),
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            ),
            modifier = Modifier.testTag("tab_vault")
        )

        // Servers Tab
        NavigationBarItem(
            selected = currentTab == MainTab.SERVERS,
            onClick = { onTabSelected(MainTab.SERVERS) },
            icon = {
                Icon(imageVector = Icons.Default.Dns, contentDescription = "Servers")
            },
            label = {
                Text(
                    text = "Servers",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == MainTab.SERVERS) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SukunaRed,
                selectedTextColor = SukunaRed,
                indicatorColor = SukunaRed.copy(alpha = 0.15f),
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            ),
            modifier = Modifier.testTag("tab_servers")
        )

        // Settings Tab
        NavigationBarItem(
            selected = currentTab == MainTab.SETTINGS,
            onClick = { onTabSelected(MainTab.SETTINGS) },
            icon = {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
            },
            label = {
                Text(
                    text = "Settings",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == MainTab.SETTINGS) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SukunaRed,
                selectedTextColor = SukunaRed,
                indicatorColor = SukunaRed.copy(alpha = 0.15f),
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            ),
            modifier = Modifier.testTag("tab_settings")
        )
    }
}
