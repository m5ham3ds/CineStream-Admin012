package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.state.AppLanguage
import com.example.state.AppSettings
import com.example.state.AppStrings
import com.example.ui.components.AdaptiveScreenContainer
import com.example.ui.components.CineStreamLoadingButton
import com.example.ui.components.bounceClick
import com.example.ui.theme.*
import com.example.viewmodels.ConfigViewModel

@Composable
fun GlobalConfigScreen(
    viewModel: ConfigViewModel = viewModel()
) {
    val config by viewModel.config.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val currentLang by AppSettings.language.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    var maintenanceEnabled by remember(config.maintenanceEnabled) { mutableStateOf(config.maintenanceEnabled) }
    var maintenanceTitle by remember(config.maintenanceTitle) { mutableStateOf(config.maintenanceTitle) }
    var maintenanceMessage by remember(config.maintenanceMessage) { mutableStateOf(config.maintenanceMessage) }
    var minVersionCode by remember(config.minimumVersionCode) { mutableStateOf(config.minimumVersionCode.toString()) }

    var offlineDays by remember(config.defaultOfflineDays) { mutableIntStateOf(config.defaultOfflineDays) }
    var forcedAds by remember(config.defaultForcedAds) { mutableIntStateOf(config.defaultForcedAds) }
    var cloudName by remember(config.cloudinaryCloudName) { mutableStateOf(config.cloudinaryCloudName) }
    var uploadPreset by remember(config.cloudinaryUploadPreset) { mutableStateOf(config.cloudinaryUploadPreset) }

    var showMaintenanceConfirmDialog by remember { mutableStateOf(false) }

    if (showMaintenanceConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showMaintenanceConfirmDialog = false },
            title = {
                Text(
                    AppStrings.maintenanceSection(currentLang),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    AppStrings.maintenanceToggleDesc(currentLang),
                    color = TextSecondary
                )
            },
            confirmButton = {
                CineStreamLoadingButton(
                    onClick = {
                        showMaintenanceConfirmDialog = false
                        viewModel.updateMaintenance(
                            enabled = maintenanceEnabled,
                            title = maintenanceTitle,
                            message = maintenanceMessage,
                            minVersion = minVersionCode.toIntOrNull() ?: 1
                        )
                    },
                    containerColor = if (maintenanceEnabled) MetricRed else CineStreamRed,
                    shape = RoundedCornerShape(10.dp),
                    text = AppStrings.confirm(currentLang)
                )
            },
            dismissButton = {
                TextButton(onClick = { showMaintenanceConfirmDialog = false }) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
            // Header Title
            Text(
                text = AppStrings.globalConfig(currentLang),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = AppStrings.globalConfigSubtitle(currentLang),
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Section 1: Maintenance Mode Card
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (maintenanceEnabled) MetricRedBg else DarkSurfaceVariant, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Engineering,
                                    contentDescription = null,
                                    tint = if (maintenanceEnabled) MetricRed else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    AppStrings.maintenanceSection(currentLang),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    AppStrings.maintenanceToggleDesc(currentLang),
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = maintenanceEnabled,
                            onCheckedChange = {
                                maintenanceEnabled = it
                                showMaintenanceConfirmDialog = true
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MetricRed
                            )
                        )
                    }

                    if (maintenanceEnabled) {
                        OutlinedTextField(
                            value = maintenanceTitle,
                            onValueChange = { maintenanceTitle = it },
                            label = { Text(AppStrings.maintenanceTitleField(currentLang), color = TextSecondary) },
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

                        OutlinedTextField(
                            value = maintenanceMessage,
                            onValueChange = { maintenanceMessage = it },
                            label = { Text(AppStrings.maintenanceMessageField(currentLang), color = TextSecondary) },
                            minLines = 2,
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

                    OutlinedTextField(
                        value = minVersionCode,
                        onValueChange = { minVersionCode = it.filter { ch -> ch.isDigit() } },
                        label = { Text(AppStrings.minClientVersionField(currentLang), color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Security & DRM Policy Card
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
                        Icon(Icons.Default.Security, contentDescription = null, tint = CineStreamRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            AppStrings.securityDrmSection(currentLang),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Anti-Tamper Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(AppStrings.antiTamperToggle(currentLang), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(AppStrings.antiTamperDesc(currentLang), color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = true,
                            onCheckedChange = {},
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CineStreamRed
                            )
                        )
                    }

                    // Screen Recording & Screenshot Blocking
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(AppStrings.screenshotBlockToggle(currentLang), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(AppStrings.screenshotBlockDesc(currentLang), color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = true,
                            onCheckedChange = {},
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CineStreamRed
                            )
                        )
                    }

                    // Root Detection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(AppStrings.rootDetectionToggle(currentLang), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(AppStrings.rootDetectionDesc(currentLang), color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = true,
                            onCheckedChange = {},
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CineStreamRed
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 3: Offline & Ads Policy Card
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
                        Icon(Icons.Default.DownloadForOffline, contentDescription = null, tint = MetricBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            AppStrings.offlinePolicySection(currentLang),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    OutlinedTextField(
                        value = offlineDays.toString(),
                        onValueChange = { offlineDays = it.toIntOrNull() ?: offlineDays },
                        label = { Text(AppStrings.downloadExpiryField(currentLang), color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                    HorizontalDivider(color = DarkCardBorder)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = MetricPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            AppStrings.adPolicySection(currentLang),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    OutlinedTextField(
                        value = forcedAds.toString(),
                        onValueChange = { forcedAds = it.toIntOrNull() ?: forcedAds },
                        label = { Text(AppStrings.adIntervalField(currentLang), color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 4: Cloudinary Media Storage Card
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
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = CineStreamRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentLang == AppLanguage.ARABIC) "تخزين الوسائط (Cloudinary)" else "Cloudinary Media Storage",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Text(
                        text = if (currentLang == AppLanguage.ARABIC) "النظام المعتمد لتخزين وتوزيع وسائط CineStream (الصور والخلفيات) بدلاً من Firebase Storage" else "Authoritative media storage and delivery engine for CineStream images and avatars",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = cloudName,
                        onValueChange = { cloudName = it },
                        label = { Text("Cloud Name", color = TextSecondary) },
                        placeholder = { Text("e.g. cinestream", color = TextSecondary.copy(alpha = 0.5f)) },
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

                    OutlinedTextField(
                        value = uploadPreset,
                        onValueChange = { uploadPreset = it },
                        label = { Text("Upload Preset (Unsigned)", color = TextSecondary) },
                        placeholder = { Text("e.g. cinestream_unsigned", color = TextSecondary.copy(alpha = 0.5f)) },
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
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Policies Button with tactile animation & loading spinner
            CineStreamLoadingButton(
                onClick = {
                    viewModel.updateMaintenance(
                        enabled = maintenanceEnabled,
                        title = maintenanceTitle,
                        message = maintenanceMessage,
                        minVersion = minVersionCode.toIntOrNull() ?: 1
                    )
                    viewModel.updateDrmSettings(days = offlineDays, ads = forcedAds)
                    viewModel.updateCloudinaryConfig(cloudName = cloudName, uploadPreset = uploadPreset)
                },
                isLoading = isSaving,
                loadingText = AppStrings.saving(currentLang),
                leadingIcon = Icons.Default.Save,
                containerColor = CineStreamRed,
                shape = RoundedCornerShape(12.dp),
                text = AppStrings.saveGlobalPolicies(currentLang),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
}
