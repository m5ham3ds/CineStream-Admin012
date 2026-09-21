package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdatesScreen(
    viewModel: ConfigViewModel = viewModel()
) {
    val currentLang by AppSettings.language.collectAsState()
    val config by viewModel.config.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val apkUploadState by viewModel.apkUploadState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var latestVersionCode by remember { mutableStateOf("") }
    var latestVersionName by remember { mutableStateOf("") }
    var apkUrl by remember { mutableStateOf("") }
    var apkSha256 by remember { mutableStateOf("") }
    var mandatoryUpdate by remember { mutableStateOf(false) }
    var minVersionCode by remember { mutableStateOf("") }
    var releaseNotes by remember { mutableStateOf("") }
    var broadcastNotification by remember { mutableStateOf(true) }

    var showConfirmPublishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.handleSelectedApk(context, uri) { sha256, extName, extCode, uploadedUrl ->
                apkSha256 = sha256
                if (!extName.isNullOrBlank()) latestVersionName = extName
                if (extCode != null && extCode > 0) latestVersionCode = extCode.toString()
                if (!uploadedUrl.isNullOrBlank()) apkUrl = uploadedUrl
            }
        }
    }

    LaunchedEffect(config) {
        latestVersionCode = config.latestVersionCode.toString()
        latestVersionName = config.latestVersionName
        apkUrl = config.apkUrl
        apkSha256 = config.apkSha256
        mandatoryUpdate = config.mandatoryUpdate
        minVersionCode = config.minimumVersionCode.toString()
        releaseNotes = config.releaseNotes
    }

    if (showConfirmPublishDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmPublishDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = CineStreamRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        AppStrings.confirmPublishDialogTitle(currentLang),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        AppStrings.confirmPublishDialogMsg(latestVersionName, latestVersionCode, currentLang),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    if (mandatoryUpdate) {
                        Text(
                            text = if (currentLang == AppLanguage.ARABIC) 
                                "• التحديث إجباري (الحد الأدنى: ${minVersionCode.ifBlank { latestVersionCode }})" 
                            else 
                                "• Mandatory update (Minimum Build: ${minVersionCode.ifBlank { latestVersionCode }})",
                            color = MetricRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (broadcastNotification) {
                        Text(
                            text = if (currentLang == AppLanguage.ARABIC) 
                                "• سيتم إرسال إشعار فوري لجميع الأجهزة النشطة عبر FCM" 
                            else 
                                "• Instant push broadcast will be dispatched to all users via FCM",
                            color = MetricGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                CineStreamLoadingButton(
                    onClick = {
                        showConfirmPublishDialog = false
                        viewModel.updateOta(
                            versionCode = latestVersionCode.toIntOrNull() ?: 1,
                            versionName = latestVersionName,
                            apkUrl = apkUrl,
                            apkSha256 = apkSha256,
                            mandatory = mandatoryUpdate,
                            releaseNotes = releaseNotes,
                            minimumVersionCode = if (mandatoryUpdate) minVersionCode.toIntOrNull() else null,
                            broadcastNotification = broadcastNotification
                        )
                    },
                    containerColor = CineStreamRed,
                    shape = RoundedCornerShape(10.dp),
                    text = AppStrings.publishReleaseNow(currentLang)
                )
            },
            dismissButton = {
                TextButton(onClick = { showConfirmPublishDialog = false }) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AppStrings.appUpdates(currentLang),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = AppStrings.appUpdatesSubtitle(currentLang),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                // Current version badge
                Box(
                    modifier = Modifier
                        .background(CineStreamRed.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, CineStreamRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "v${config.latestVersionName.ifBlank { "1.0.0" }}",
                        color = CineStreamRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Production Release Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkCardBorder, DarkCardBorder))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MetricGreenBg.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = MetricGreen, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    AppStrings.currentRelease(currentLang),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    "${config.latestVersionName} (Build: ${config.latestVersionCode})",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (config.mandatoryUpdate) {
                            Box(
                                modifier = Modifier
                                    .background(MetricRedBg.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    AppStrings.mandatoryUpdate(currentLang),
                                    color = MetricRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (config.apkUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = config.apkUrl,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(DarkSurface, RoundedCornerShape(8.dp))
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                                    .bounceClick(scaleDown = 0.88f, shape = RoundedCornerShape(8.dp)) {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("APK URL", config.apkUrl))
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(AppStrings.copied(currentLang))
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = AppStrings.copy(currentLang), tint = CineStreamRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Configure & Publish New Release Form
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = CineStreamRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                AppStrings.configureNewRelease(currentLang),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        TextButton(
                            onClick = {
                                latestVersionCode = config.latestVersionCode.toString()
                                latestVersionName = config.latestVersionName
                                apkUrl = config.apkUrl
                                apkSha256 = config.apkSha256
                                mandatoryUpdate = config.mandatoryUpdate
                                minVersionCode = config.minimumVersionCode.toString()
                                releaseNotes = config.releaseNotes
                                viewModel.resetApkUploadState()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.restoreCurrentValues(currentLang), color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    // APK Picker & Auto SHA-256 Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = AppStrings.uploadApkFromDevice(currentLang),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = AppStrings.uploadApkDesc(currentLang),
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                val isProcessingApk = apkUploadState is com.example.media.ApkUploadState.Processing ||
                                                      apkUploadState is com.example.media.ApkUploadState.Uploading

                                Button(
                                    onClick = {
                                        apkPickerLauncher.launch("*/*")
                                    },
                                    enabled = !isProcessingApk,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CineStreamRed,
                                        contentColor = Color.White,
                                        disabledContainerColor = DarkCardBorder
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    if (isProcessingApk) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.FileOpen,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            if (currentLang == AppLanguage.ARABIC) "اختيار ملف" else "Browse",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Live APK processing status pill
                            when (val state = apkUploadState) {
                                is com.example.media.ApkUploadState.Processing -> {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MetricBlueBg, RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = MetricBlue, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(AppStrings.analyzingApk(currentLang), color = MetricBlue, fontSize = 11.sp)
                                    }
                                }
                                is com.example.media.ApkUploadState.Uploading -> {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(WarningOrangeBg, RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = WarningOrange, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(AppStrings.uploadingApk(currentLang), color = WarningOrange, fontSize = 11.sp)
                                    }
                                }
                                is com.example.media.ApkUploadState.Success -> {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MetricGreenBg, RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MetricGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "${AppStrings.uploadApkSuccess(currentLang)} (${state.info.sizeFormatted})",
                                            color = MetricGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                is com.example.media.ApkUploadState.HashCalculated -> {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MetricGreenBg, RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MetricGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "${state.info.fileName} (${state.info.sizeFormatted}) • SHA-256 ✓",
                                            color = MetricGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                is com.example.media.ApkUploadState.Error -> {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MetricRedBg, RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MetricRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(state.errorMessage, color = MetricRed, fontSize = 11.sp)
                                    }
                                }
                                else -> {}
                            }
                        }
                    }

                    // Version Name & Build Code
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = latestVersionName,
                            onValueChange = { latestVersionName = it },
                            label = { Text(AppStrings.versionNameField(currentLang), color = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f),
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
                            value = latestVersionCode,
                            onValueChange = { latestVersionCode = it.filter { ch -> ch.isDigit() } },
                            label = { Text(AppStrings.versionCodeField(currentLang), color = TextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
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

                    // Direct APK Download URL
                    OutlinedTextField(
                        value = apkUrl,
                        onValueChange = { apkUrl = it },
                        label = { Text(AppStrings.apkDownloadUrlField(currentLang), color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = TextSecondary) },
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

                    // SHA-256 Checksum
                    OutlinedTextField(
                        value = apkSha256,
                        onValueChange = { apkSha256 = it },
                        label = { Text(AppStrings.apkSha256Field(currentLang), color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = TextSecondary) },
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

                    // Mandatory Update Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                AppStrings.mandatoryUpdate(currentLang),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                AppStrings.mandatoryUpdateDesc(currentLang),
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = mandatoryUpdate,
                            onCheckedChange = { mandatoryUpdate = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CineStreamRed
                            )
                        )
                    }

                    // Minimum Supported Version Code
                    if (mandatoryUpdate) {
                        OutlinedTextField(
                            value = minVersionCode,
                            onValueChange = { minVersionCode = it.filter { ch -> ch.isDigit() } },
                            label = { Text(AppStrings.minVersionCodeField(currentLang), color = TextSecondary) },
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

                    // Release Notes
                    OutlinedTextField(
                        value = releaseNotes,
                        onValueChange = { releaseNotes = it },
                        label = { Text(AppStrings.releaseNotesField(currentLang), color = TextSecondary) },
                        minLines = 3,
                        maxLines = 6,
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

                    // Broadcast Notification Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                AppStrings.broadcastNotificationToggle(currentLang),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                AppStrings.broadcastNotificationDesc(currentLang),
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = broadcastNotification,
                            onCheckedChange = { broadcastNotification = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CineStreamRed
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Publish Button (CineStream Red) with tactile bounce & loading spinner
                    CineStreamLoadingButton(
                        onClick = { showConfirmPublishDialog = true },
                        isLoading = isSaving,
                        loadingText = AppStrings.saving(currentLang),
                        enabled = latestVersionName.isNotBlank() && latestVersionCode.isNotBlank(),
                        leadingIcon = Icons.Default.Publish,
                        containerColor = CineStreamRed,
                        shape = RoundedCornerShape(12.dp),
                        text = AppStrings.publishReleaseNow(currentLang),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
}
