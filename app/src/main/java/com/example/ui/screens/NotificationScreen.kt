package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.models.NotificationRequest
import com.example.state.AppLanguage
import com.example.state.AppSettings
import com.example.state.AppStrings
import com.example.ui.components.AdaptiveScreenContainer
import com.example.ui.components.CineStreamLoadingButton
import com.example.ui.components.bounceClick
import com.example.ui.theme.*
import com.example.viewmodels.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel = viewModel()
) {
    val currentLang by AppSettings.language.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val recentNotifications by viewModel.recentNotifications.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showSendConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    if (showSendConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSendConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = CineStreamRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        AppStrings.confirmSendNotifTitle(currentLang),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    AppStrings.confirmSendNotifMsg(currentLang),
                    color = TextSecondary
                )
            },
            confirmButton = {
                CineStreamLoadingButton(
                    onClick = {
                        showSendConfirmDialog = false
                        viewModel.sendNotification()
                    },
                    containerColor = CineStreamRed,
                    shape = RoundedCornerShape(10.dp),
                    text = AppStrings.sendNow(currentLang)
                )
            },
            dismissButton = {
                TextButton(onClick = { showSendConfirmDialog = false }) {
                    Text(AppStrings.cancel(currentLang), color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                // Title and Subtitle
                Text(
                    text = AppStrings.notifications(currentLang),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = AppStrings.notificationsSubtitle(currentLang),
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            // Create Broadcast Card
            item {
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
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = CineStreamRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                AppStrings.sendBroadcast(currentLang),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        // Title field
                        OutlinedTextField(
                            value = uiState.title,
                            onValueChange = { viewModel.updateTitle(it) },
                            label = { Text(AppStrings.notifTitleField(currentLang), color = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CineStreamRed,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant
                            )
                        )

                        // Body message field
                        OutlinedTextField(
                            value = uiState.message,
                            onValueChange = { viewModel.updateMessage(it) },
                            label = { Text(AppStrings.notifBodyField(currentLang), color = TextSecondary) },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CineStreamRed,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant
                            )
                        )

                        // Notification Type Selector
                        Text(
                            if (currentLang == AppLanguage.ARABIC) "نوع الإشعار" else "Notification Type",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val types = listOf(
                                "SYSTEM" to if (currentLang == AppLanguage.ARABIC) "نظام" else "System",
                                "ANNOUNCEMENT" to if (currentLang == AppLanguage.ARABIC) "إعلان" else "Announce",
                                "ALERT" to if (currentLang == AppLanguage.ARABIC) "تنبيه" else "Alert",
                                "PROMO" to if (currentLang == AppLanguage.ARABIC) "عرض" else "Promo"
                            )
                            types.forEach { (typeKey, typeLabel) ->
                                AudienceChip(
                                    label = typeLabel,
                                    isSelected = uiState.type == typeKey,
                                    onClick = { viewModel.updateType(typeKey) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Target Audience Selector
                        Text(
                            AppStrings.targetAudience(currentLang),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AudienceChip(
                                label = AppStrings.targetAllUsers(currentLang),
                                isSelected = uiState.targetType == NotificationRequest.TargetType.ALL,
                                onClick = { viewModel.updateTargetType(NotificationRequest.TargetType.ALL) },
                                modifier = Modifier.weight(1f)
                            )
                            AudienceChip(
                                label = if (currentLang == AppLanguage.ARABIC) "مشتركو Pro" else "PRO Users",
                                isSelected = uiState.targetType == NotificationRequest.TargetType.PRO,
                                onClick = { viewModel.updateTargetType(NotificationRequest.TargetType.PRO) },
                                modifier = Modifier.weight(1f)
                            )
                            AudienceChip(
                                label = if (currentLang == AppLanguage.ARABIC) "مستخدم (UID)" else "Specific UID",
                                isSelected = uiState.targetType == NotificationRequest.TargetType.UID,
                                onClick = { viewModel.updateTargetType(NotificationRequest.TargetType.UID) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (uiState.targetType == NotificationRequest.TargetType.UID) {
                            OutlinedTextField(
                                value = uiState.targetUid,
                                onValueChange = { viewModel.updateTargetUid(it) },
                                label = { Text(AppStrings.uidField(currentLang), color = TextSecondary) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CineStreamRed,
                                    unfocusedBorderColor = DarkCardBorder,
                                    focusedContainerColor = DarkSurfaceVariant,
                                    unfocusedContainerColor = DarkSurfaceVariant
                                )
                            )
                        }

                        // FCM Backend Notice
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MetricBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (currentLang == AppLanguage.ARABIC)
                                        "يتم حفظ الإشعار في /notifications وتقوم Cloud Functions بإرسال إشعارات الدفع (FCM) للأجهزة المستهدفة."
                                    else
                                        "Broadcasts are saved to /notifications; background Cloud Functions trigger real-time FCM push delivery to clients.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        // Send Button (CineStream Red) with tactile bounce & loading spinner
                        CineStreamLoadingButton(
                            onClick = { showSendConfirmDialog = true },
                            isLoading = isSending,
                            loadingText = AppStrings.sending(currentLang),
                            enabled = uiState.title.isNotBlank() && uiState.message.isNotBlank(),
                            leadingIcon = Icons.Default.Send,
                            containerColor = CineStreamRed,
                            shape = RoundedCornerShape(10.dp),
                            text = AppStrings.sendNow(currentLang),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        )
                    }
                }
            }

            // Notification History Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        AppStrings.notificationHistory(currentLang),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Box(
                        modifier = Modifier
                            .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "${recentNotifications.size}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (recentNotifications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(AppStrings.noNotifHistory(currentLang), color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                items(recentNotifications) { notif ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkCardBorder, DarkCardBorder))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = notif.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(notif.createdAt)),
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = notif.message,
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
}

@Composable
private fun AudienceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .background(
                if (isSelected) CineStreamRed else DarkSurfaceVariant,
                shape
            )
            .border(
                1.dp,
                if (isSelected) CineStreamRed else DarkCardBorder,
                shape
            )
            .bounceClick(scaleDown = 0.92f, shape = shape) { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
        )
    }
}
