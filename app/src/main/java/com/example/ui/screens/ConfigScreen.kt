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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AdaptiveScreenContainer
import com.example.ui.components.CineStreamLoadingButton
import com.example.ui.components.bounceClick
import com.example.ui.theme.*
import com.example.viewmodels.ConfigViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ConfigScreen(
    viewModel: ConfigViewModel = viewModel()
) {
    val config by viewModel.config.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // Local state synced with remote config
    var maintenanceEnabled by remember(config.maintenanceEnabled) { mutableStateOf(config.maintenanceEnabled) }
    var maintenanceTitle by remember(config.maintenanceTitle) { mutableStateOf(config.maintenanceTitle) }
    var maintenanceMessage by remember(config.maintenanceMessage) { mutableStateOf(config.maintenanceMessage) }
    var minVersionCode by remember(config.minimumVersionCode) { mutableStateOf(config.minimumVersionCode.toString()) }

    var latestVersionCode by remember(config.latestVersionCode) { mutableStateOf(config.latestVersionCode.toString()) }
    var latestVersionName by remember(config.latestVersionName) { mutableStateOf(config.latestVersionName) }
    var apkUrl by remember(config.apkUrl) { mutableStateOf(config.apkUrl) }
    var apkSha256 by remember(config.apkSha256) { mutableStateOf(config.apkSha256) }
    var mandatoryUpdate by remember(config.mandatoryUpdate) { mutableStateOf(config.mandatoryUpdate) }
    var releaseNotes by remember(config.releaseNotes) { mutableStateOf(config.releaseNotes) }

    var offlineDays by remember(config.defaultOfflineDays) { mutableIntStateOf(config.defaultOfflineDays) }
    var forcedAds by remember(config.defaultForcedAds) { mutableIntStateOf(config.defaultForcedAds) }
    var providersJson by remember(config.providersJson) { mutableStateOf(config.providersJson) }

    var showMaintenanceConfirmDialog by remember { mutableStateOf(false) }
    var showOtaConfirmDialog by remember { mutableStateOf(false) }
    var showDeployProvidersDialog by remember { mutableStateOf(false) }

    if (showMaintenanceConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showMaintenanceConfirmDialog = false },
            title = {
                Text(
                    if (maintenanceEnabled) "Enable Maintenance Mode?" else "Disable Maintenance Mode?",
                    color = Color.White
                )
            },
            text = {
                Text(
                    if (maintenanceEnabled)
                        "Enabling maintenance mode will immediately block CineStream users from playing media and accessing content. Are you sure?"
                    else
                        "Disabling maintenance mode will restore normal access to all CineStream users.",
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
                    containerColor = if (maintenanceEnabled) ErrorRed else PrimaryBlue,
                    shape = RoundedCornerShape(10.dp),
                    text = "Confirm"
                )
            },
            dismissButton = {
                TextButton(onClick = { showMaintenanceConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    if (showOtaConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showOtaConfirmDialog = false },
            title = { Text("Publish CineStream OTA Update?", color = Color.White) },
            text = {
                Text(
                    "This will publish version $latestVersionName (code $latestVersionCode) to all connected CineStream client devices. Mandatory: ${if (mandatoryUpdate) "Yes" else "No"}.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                CineStreamLoadingButton(
                    onClick = {
                        showOtaConfirmDialog = false
                        viewModel.updateOta(
                            versionCode = latestVersionCode.toIntOrNull() ?: 1,
                            versionName = latestVersionName,
                            apkUrl = apkUrl,
                            apkSha256 = apkSha256,
                            mandatory = mandatoryUpdate,
                            releaseNotes = releaseNotes
                        )
                    },
                    containerColor = PrimaryBlue,
                    shape = RoundedCornerShape(10.dp),
                    text = "Publish Now"
                )
            },
            dismissButton = {
                TextButton(onClick = { showOtaConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    if (showDeployProvidersDialog) {
        AlertDialog(
            onDismissRequest = { showDeployProvidersDialog = false },
            title = { Text("Deploy Dynamic Providers?", color = Color.White) },
            text = {
                Text(
                    "Deploying providers configuration will update the scraping/resolving rules on all CineStream clients. Ensure the JSON schema is correct.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                CineStreamLoadingButton(
                    onClick = {
                        showDeployProvidersDialog = false
                        viewModel.updateProvidersJson(providersJson)
                    },
                    containerColor = PrimaryBlue,
                    shape = RoundedCornerShape(10.dp),
                    text = "Deploy"
                )
            },
            dismissButton = {
                TextButton(onClick = { showDeployProvidersDialog = false }) {
                    Text("Cancel", color = TextSecondary)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Global Configuration",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Manage CineStream app parameters, OTA updates, and maintenance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            if (config.updatedAt > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(config.updatedAt))
                Text("Last synchronized: $dateStr", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. MAINTENANCE MODE CARD
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (maintenanceEnabled) ErrorRed.copy(alpha = 0.12f) else DarkSurface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (maintenanceEnabled) ErrorRed.copy(alpha = 0.5f) else DividerColor,
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (maintenanceEnabled) ErrorRed.copy(alpha = 0.2f) else PrimaryBlue.copy(alpha = 0.2f),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (maintenanceEnabled) Icons.Default.Warning else Icons.Default.Construction,
                                    contentDescription = null,
                                    tint = if (maintenanceEnabled) ErrorRed else PrimaryBlue
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Maintenance Mode",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    if (maintenanceEnabled) "Active - Users are blocked" else "Inactive - System operational",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (maintenanceEnabled) ErrorRed else SuccessGreen
                                )
                            }
                        }
                        Switch(
                            checked = maintenanceEnabled,
                            onCheckedChange = { maintenanceEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ErrorRed
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = maintenanceTitle,
                        onValueChange = { maintenanceTitle = it },
                        label = { Text("Maintenance Screen Title", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = maintenanceMessage,
                        onValueChange = { maintenanceMessage = it },
                        label = { Text("Notice Message for Users", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = customTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = minVersionCode,
                        onValueChange = { minVersionCode = it },
                        label = { Text("Minimum Version Allowed (Bypass)", color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CineStreamLoadingButton(
                        onClick = { showMaintenanceConfirmDialog = true },
                        isLoading = isSaving,
                        containerColor = if (maintenanceEnabled) ErrorRed else PrimaryBlue,
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = Icons.Default.Save,
                        text = "Save Maintenance Settings",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. OTA APP UPDATES CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(PrimaryBlue.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = PrimaryBlue)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "CineStream OTA App Updates",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                "Deploy in-app APK upgrades directly to user clients",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = latestVersionCode,
                            onValueChange = { latestVersionCode = it },
                            label = { Text("Version Code", color = TextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = customTextFieldColors()
                        )
                        OutlinedTextField(
                            value = latestVersionName,
                            onValueChange = { latestVersionName = it },
                            label = { Text("Version Name", color = TextSecondary) },
                            modifier = Modifier.weight(1f),
                            colors = customTextFieldColors()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = apkUrl,
                        onValueChange = { apkUrl = it },
                        label = { Text("Direct APK Download URL (HTTPS)", color = TextSecondary) },
                        placeholder = { Text("https://example.com/cinestream-v2.apk", color = TextSecondary.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = apkSha256,
                        onValueChange = { apkSha256 = it },
                        label = { Text("APK SHA-256 Checksum", color = TextSecondary) },
                        placeholder = { Text("Integrity verification hash", color = TextSecondary.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = releaseNotes,
                        onValueChange = { releaseNotes = it },
                        label = { Text("Release Notes (What's New)", color = TextSecondary) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Mandatory Force Update", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Prevents opening app until APK is installed", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = mandatoryUpdate,
                            onCheckedChange = { mandatoryUpdate = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    CineStreamLoadingButton(
                        onClick = { showOtaConfirmDialog = true },
                        isLoading = isSaving,
                        containerColor = PrimaryBlue,
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = Icons.Default.CloudUpload,
                        text = "Publish CineStream Update",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. OFFLINE DRM & AD RULES CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(SuccessGreen.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = SuccessGreen)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Offline DRM & Monetization Defaults",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                "Global playback limits for non-overridden user accounts",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    ConfigStepper(
                        title = "Default Offline DRM Duration",
                        subtitle = "Days allowed before re-validating license online",
                        value = offlineDays,
                        onValueChange = { offlineDays = it.coerceAtLeast(1) }
                    )

                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 16.dp))

                    ConfigStepper(
                        title = "Default Forced Ads to Unlock Offline",
                        subtitle = "Number of rewarded ads required for offline decryption",
                        value = forcedAds,
                        onValueChange = { forcedAds = it.coerceAtLeast(0) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CineStreamLoadingButton(
                        onClick = { viewModel.updateDrmSettings(offlineDays, forcedAds) },
                        isLoading = isSaving,
                        containerColor = SuccessGreen,
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = Icons.Default.Save,
                        text = "Save DRM & Ad Rules",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. DYNAMIC PROVIDERS JSON CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(PrimaryBlue.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = PrimaryBlue)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Dynamic Providers & Scrapers JSON",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    "OTA endpoints and repository links loaded by CineStream",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = providersJson,
                        onValueChange = { providersJson = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 8,
                        maxLines = 14,
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF80D8FF)
                        ),
                        colors = customTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CineStreamLoadingButton(
                        onClick = { showDeployProvidersDialog = true },
                        isLoading = isSaving,
                        containerColor = PrimaryBlue,
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = Icons.Default.Send,
                        text = "Deploy Dynamic Providers to CineStream",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
}

@Composable
fun ConfigStepper(
    title: String,
    subtitle: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick = { if (value > 0) onValueChange(value - 1) },
                enabled = value > 0,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = DarkBackground),
                modifier = Modifier
                    .size(36.dp)
                    .bounceClick(scaleDown = 0.85f)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = if (value > 0) Color.White else TextSecondary, modifier = Modifier.size(18.dp))
            }
            Text(
                text = value.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            FilledIconButton(
                onClick = { onValueChange(value + 1) },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = DarkBackground),
                modifier = Modifier
                    .size(36.dp)
                    .bounceClick(scaleDown = 0.85f)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = DarkBackground,
    focusedContainerColor = DarkBackground,
    unfocusedBorderColor = DividerColor,
    focusedBorderColor = PrimaryBlue,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)
