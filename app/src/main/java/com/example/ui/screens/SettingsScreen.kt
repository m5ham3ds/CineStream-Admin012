package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.state.AppLanguage
import com.example.state.AppSettings
import com.example.state.AppStrings
import com.example.ui.components.AdaptiveScreenContainer
import com.example.ui.components.CineStreamLoadingButton
import com.example.ui.components.CineStreamOutlinedLoadingButton
import com.example.ui.components.bounceClick
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val isDarkTheme by AppSettings.isDarkTheme.collectAsState()
    val currentLang by AppSettings.language.collectAsState()
    val notificationsEnabled by AppSettings.notificationsEnabled.collectAsState()
    val soundEnabled by AppSettings.soundEnabled.collectAsState()
    val vibrationEnabled by AppSettings.vibrationEnabled.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser = remember {
        try {
            FirebaseAuth.getInstance().currentUser
        } catch (e: Exception) {
            null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground
    ) { paddingValues ->
        AdaptiveScreenContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AppStrings.settings(currentLang),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = AppStrings.settingsSubtitle(currentLang),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(DarkSurface, RoundedCornerShape(10.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                        .bounceClick(scaleDown = 0.88f, shape = RoundedCornerShape(10.dp)) {
                            onNavigateToDiagnostics()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = AppStrings.diagnostics(currentLang),
                        tint = CineStreamRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Appearance & Theme Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkCardBorder, DarkCardBorder))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = CineStreamRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            AppStrings.appearance(currentLang),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Dark / Light Theme Toggle Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Dark Theme Choice
                        ThemeOptionBox(
                            title = AppStrings.darkTheme(currentLang),
                            isSelected = isDarkTheme,
                            icon = Icons.Default.DarkMode,
                            onClick = { AppSettings.setDarkTheme(true) },
                            modifier = Modifier.weight(1f)
                        )

                        // Light Theme Choice
                        ThemeOptionBox(
                            title = AppStrings.lightTheme(currentLang),
                            isSelected = !isDarkTheme,
                            icon = Icons.Default.LightMode,
                            onClick = { AppSettings.setDarkTheme(false) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Language Selection Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkCardBorder, DarkCardBorder))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = MetricBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                AppStrings.language(currentLang),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                AppStrings.languageDesc(currentLang),
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LanguageOptionBox(
                            title = "العربية (RTL)",
                            isSelected = currentLang == AppLanguage.ARABIC,
                            onClick = { AppSettings.setLanguage(AppLanguage.ARABIC) },
                            modifier = Modifier.weight(1f)
                        )

                        LanguageOptionBox(
                            title = "English (LTR)",
                            isSelected = currentLang == AppLanguage.ENGLISH,
                            onClick = { AppSettings.setLanguage(AppLanguage.ENGLISH) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notification Preferences Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkCardBorder, DarkCardBorder))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MetricGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            AppStrings.notificationPreferences(currentLang),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // In-app Notifications
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(AppStrings.enableNotifications(currentLang), color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { AppSettings.setNotificationsEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CineStreamRed
                            )
                        )
                    }

                    // Sound alerts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(AppStrings.soundAlerts(currentLang), color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { AppSettings.setSoundEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CineStreamRed
                            )
                        )
                    }

                    // Haptic Vibration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(AppStrings.vibration(currentLang), color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = { AppSettings.setVibrationEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CineStreamRed
                            )
                        )
                    }

                    CineStreamOutlinedLoadingButton(
                        onClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(AppStrings.testNotifSentMsg(currentLang))
                            }
                        },
                        text = AppStrings.testNotification(currentLang),
                        leadingIcon = Icons.Default.Send,
                        contentColor = CineStreamRed,
                        borderColor = CineStreamRed,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // System Information Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkCardBorder, DarkCardBorder))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            AppStrings.systemInfo(currentLang),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    InfoRow(label = AppStrings.appVersion(currentLang), value = "v2.5.0-pro (CineStream)")
                    InfoRow(label = AppStrings.connectedProject(currentLang), value = "cinestream-production")
                    currentUser?.email?.let { email ->
                        InfoRow(label = AppStrings.adminAccount(currentLang), value = email)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sign Out Button with tactile feedback
            CineStreamLoadingButton(
                onClick = onLogoutClick,
                containerColor = MetricRedBg,
                contentColor = MetricRed,
                leadingIcon = Icons.Default.Logout,
                text = AppStrings.logout(currentLang),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
}

@Composable
private fun ThemeOptionBox(
    title: String,
    isSelected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .background(
                if (isSelected) CineStreamRed.copy(alpha = 0.15f) else DarkSurfaceVariant,
                shape
            )
            .border(
                1.dp,
                if (isSelected) CineStreamRed else DarkCardBorder,
                shape
            )
            .bounceClick(scaleDown = 0.94f, shape = shape) { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) CineStreamRed else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun LanguageOptionBox(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .background(
                if (isSelected) CineStreamRed.copy(alpha = 0.15f) else DarkSurfaceVariant,
                shape
            )
            .border(
                1.dp,
                if (isSelected) CineStreamRed else DarkCardBorder,
                shape
            )
            .bounceClick(scaleDown = 0.94f, shape = shape) { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    }
}
