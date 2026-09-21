package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.state.AppSettings
import com.example.state.AppStrings
import com.example.ui.components.AdaptiveScreenContainer
import com.example.ui.components.CineStreamLoadingButton
import com.example.ui.components.bounceClick
import com.example.ui.theme.*
import com.example.viewmodels.UserDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UserDetailScreen(
    userId: String,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val currentLang by AppSettings.language.collectAsState()
    var showRoleConfirmDialog by remember { mutableStateOf(false) }

    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UserDetailViewModel(userId) as T
        }
    }
    val viewModel: UserDetailViewModel = viewModel(factory = factory)
    val user by viewModel.user.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    if (showRoleConfirmDialog && user != null) {
        val u = user!!
        val willBeAdmin = u.role != "admin"
        AlertDialog(
            onDismissRequest = { showRoleConfirmDialog = false },
            title = {
                Text(
                    text = AppStrings.roleAndSubscription(currentLang),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (willBeAdmin) AppStrings.grantAdminToggle(currentLang) else AppStrings.isAdministrator(currentLang),
                    color = TextSecondary
                )
            },
            confirmButton = {
                CineStreamLoadingButton(
                    onClick = {
                        showRoleConfirmDialog = false
                        viewModel.toggleRole()
                    },
                    containerColor = if (willBeAdmin) CineStreamRed else MetricPurple,
                    shape = RoundedCornerShape(10.dp),
                    text = AppStrings.confirm(currentLang)
                )
            },
            dismissButton = {
                TextButton(onClick = { showRoleConfirmDialog = false }) {
                    Text(AppStrings.cancel(currentLang), color = TextSecondary)
                }
            },
            containerColor = DarkSurface
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
            if (user == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CineStreamRed)
                }
            } else {
                val u = user!!
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
                            text = AppStrings.userDetailTitle(currentLang),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = AppStrings.userDetailSubtitle(currentLang),
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    // User Status Badge
                    Box(
                        modifier = Modifier
                            .background(
                                if (u.isPremium) MetricPurpleBg.copy(alpha = 0.5f) else DarkSurfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (u.isPremium) MetricPurple else DarkCardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (u.isPremium) AppStrings.proBadge(currentLang) else "FREE",
                            color = if (u.isPremium) MetricPurple else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Account & Profile Details Card
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
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF202532), CircleShape)
                                    .border(1.dp, if (u.isPremium) MetricPurple else DarkCardBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (u.photoUrl.isNotBlank()) {
                                    coil.compose.AsyncImage(
                                        model = u.photoUrl,
                                        contentDescription = "User Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = (u.username.firstOrNull() ?: 'U').uppercaseChar().toString(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = u.username.ifBlank { "User" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = u.email,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        HorizontalDivider(color = DarkCardBorder)

                        // UID Row with Copy
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(AppStrings.uidField(currentLang), color = TextSecondary, fontSize = 11.sp)
                                Text(u.id, color = Color.White, fontSize = 12.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .bounceClick(scaleDown = 0.85f) {
                                        clipboardManager.setText(AnnotatedString(u.id))
                                        Toast.makeText(context, AppStrings.copied(currentLang), Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = AppStrings.copy(currentLang), tint = CineStreamRed, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Created Date
                        val createdDateStr = if (u.createdAt > 0L) {
                            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(u.createdAt))
                        } else "-"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(AppStrings.createdAtField(currentLang), color = TextSecondary, fontSize = 12.sp)
                            Text(createdDateStr, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Permissions Section (Default ALL Enabled except Admin and PRO)
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
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = CineStreamRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                AppStrings.userPermissionsSection(currentLang),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        // 1. Watching & Streaming Access (Enabled by default, canWatch == !watchBan)
                        PermissionToggleRow(
                            title = AppStrings.canWatch(currentLang),
                            description = AppStrings.canWatchDesc(currentLang),
                            icon = Icons.Default.PlayCircle,
                            iconTint = MetricGreen,
                            isEnabled = u.isWatchAllowed,
                            onToggle = { viewModel.togglePermission("watch", u.isWatchAllowed) }
                        )

                        // 2. Offline Downloads Access (Enabled by default, canDownload == !downloadBan)
                        PermissionToggleRow(
                            title = AppStrings.canDownload(currentLang),
                            description = AppStrings.canDownloadDesc(currentLang),
                            icon = Icons.Default.FileDownload,
                            iconTint = MetricBlue,
                            isEnabled = u.isDownloadAllowed,
                            onToggle = { viewModel.togglePermission("download", u.isDownloadAllowed) }
                        )

                        // 3. Chat & Community Access (Enabled by default, canChat == !chatBan)
                        PermissionToggleRow(
                            title = AppStrings.canChat(currentLang),
                            description = AppStrings.canChatDesc(currentLang),
                            icon = Icons.Default.Chat,
                            iconTint = WarningOrange,
                            isEnabled = u.isChatAllowed,
                            onToggle = { viewModel.togglePermission("chat", u.isChatAllowed) }
                        )

                        // 4. Story & Clips Access (Enabled by default, canStory == !storyBan)
                        PermissionToggleRow(
                            title = AppStrings.canStory(currentLang),
                            description = AppStrings.canStoryDesc(currentLang),
                            icon = Icons.Default.CameraAlt,
                            iconTint = MetricPurple,
                            isEnabled = u.isStoryAllowed,
                            onToggle = { viewModel.togglePermission("story", u.isStoryAllowed) }
                        )

                        // 5. P2P Sharing Access (Enabled by default, canP2P == !p2pBan)
                        PermissionToggleRow(
                            title = AppStrings.canP2P(currentLang),
                            description = AppStrings.canP2PDesc(currentLang),
                            icon = Icons.Default.Share,
                            iconTint = MetricGreen,
                            isEnabled = u.isP2PAllowed,
                            onToggle = { viewModel.togglePermission("p2p", u.isP2PAllowed) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Role & Subscription Section (Administrator Privileges and Premium PRO Status)
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
                            Icon(Icons.Default.Security, contentDescription = null, tint = MetricPurple)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                AppStrings.roleAndSubscription(currentLang),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        // Premium PRO Status Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    AppStrings.isProSubscriber(currentLang),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    if (u.isPremium) AppStrings.revokePro(currentLang) else AppStrings.upgradeToPro(currentLang),
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = u.isPremium,
                                onCheckedChange = { viewModel.togglePremium() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MetricPurple
                                )
                            )
                        }

                        HorizontalDivider(color = DarkCardBorder)

                        // Administrator Privileges Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    AppStrings.isAdministrator(currentLang),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    if (u.role == "admin") "Full Admin Portal Access" else "Standard User Access",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = u.role == "admin",
                                onCheckedChange = { showRoleConfirmDialog = true },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CineStreamRed
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
}

@Composable
private fun PermissionToggleRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(description, color = TextSecondary, fontSize = 11.sp)
            }
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MetricGreen
            )
        )
    }
}
