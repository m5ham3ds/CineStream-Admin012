package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.models.ExtensionItem
import com.example.state.AppLanguage
import com.example.state.AppSettings
import com.example.state.AppStrings
import com.example.ui.components.AdaptiveScreenContainer
import com.example.ui.components.CineStreamLoadingButton
import com.example.ui.components.bounceClick
import com.example.ui.theme.*
import com.example.viewmodels.ConfigViewModel
import com.example.viewmodels.ExtensionsViewModel
import kotlinx.coroutines.launch

@Composable
fun ExtensionsUpdatesScreen(
    onBackClick: () -> Unit = {},
    extensionsViewModel: ExtensionsViewModel = viewModel(),
    configViewModel: ConfigViewModel = viewModel()
) {
    val extensions by extensionsViewModel.extensions.collectAsState()
    val extStatusMessage by extensionsViewModel.statusMessage.collectAsState()
    val config by configViewModel.config.collectAsState()
    val isSavingConfig by configViewModel.isSaving.collectAsState()
    val configStatusMessage by configViewModel.statusMessage.collectAsState()
    val currentLang by AppSettings.language.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(extStatusMessage) {
        extStatusMessage?.let {
            snackbarHostState.showSnackbar(it)
            extensionsViewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(configStatusMessage) {
        configStatusMessage?.let {
            snackbarHostState.showSnackbar(it)
            configViewModel.clearStatusMessage()
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var extensionToEdit by remember { mutableStateOf<ExtensionItem?>(null) }
    var extensionToDelete by remember { mutableStateOf<ExtensionItem?>(null) }
    var showJsonEditorDialog by remember { mutableStateOf(false) }
    var providersJsonText by remember(config.providersJson) { mutableStateOf(config.providersJson) }

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

                // Title and Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppStrings.extensionsTitle(currentLang),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = AppStrings.extensionsSubtitle(currentLang),
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(DarkSurface, RoundedCornerShape(10.dp))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                                .bounceClick(scaleDown = 0.88f, shape = RoundedCornerShape(10.dp)) {
                                    showJsonEditorDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Code, contentDescription = "JSON", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        CineStreamLoadingButton(
                            onClick = { showAddDialog = true },
                            containerColor = CineStreamRed,
                            shape = RoundedCornerShape(10.dp),
                            leadingIcon = Icons.Default.Add,
                            text = AppStrings.add(currentLang),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        AppStrings.registeredScrapers(currentLang),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Box(
                        modifier = Modifier
                            .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "${extensions.size}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (extensions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ExtensionOff, contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(AppStrings.noExtensionsFound(currentLang), color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(extensions, key = { it.id }) { ext ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(14.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkCardBorder, DarkCardBorder))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(if (ext.enabled) CineStreamRed.copy(alpha = 0.15f) else DarkSurfaceVariant, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Extension,
                                            contentDescription = null,
                                            tint = if (ext.enabled) CineStreamRed else TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = ext.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "${ext.packageName} • v${ext.versionName}",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Switch(
                                    checked = ext.enabled,
                                    onCheckedChange = { extensionsViewModel.toggleExtension(ext) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = CineStreamRed
                                    )
                                )
                            }

                            if (ext.apkUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = ext.apkUrl,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .bounceClick(scaleDown = 0.85f) {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("URL", ext.apkUrl))
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(AppStrings.copied(currentLang))
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = AppStrings.copy(currentLang), tint = CineStreamRed, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { extensionToEdit = ext },
                                    modifier = Modifier.bounceClick(scaleDown = 0.9f) { extensionToEdit = ext },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(AppStrings.edit(currentLang), color = TextSecondary, fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                TextButton(
                                    onClick = { extensionToDelete = ext },
                                    modifier = Modifier.bounceClick(scaleDown = 0.9f) { extensionToDelete = ext },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MetricRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(AppStrings.delete(currentLang), color = MetricRed, fontSize = 12.sp)
                                }
                            }
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

    // Add Extension Dialog
    if (showAddDialog) {
        ExtensionFormDialog(
            title = AppStrings.addExtension(currentLang),
            initialExtension = null,
            onDismiss = { showAddDialog = false },
            onSave = { ext ->
                extensionsViewModel.saveExtension(ext)
                showAddDialog = false
            }
        )
    }

    // Edit Extension Dialog
    extensionToEdit?.let { ext ->
        ExtensionFormDialog(
            title = AppStrings.editExtension(currentLang),
            initialExtension = ext,
            onDismiss = { extensionToEdit = null },
            onSave = { updated ->
                extensionsViewModel.saveExtension(updated)
                extensionToEdit = null
            }
        )
    }

    // Confirm Delete Dialog
    extensionToDelete?.let { ext ->
        AlertDialog(
            onDismissRequest = { extensionToDelete = null },
            title = {
                Text(AppStrings.confirmDeleteExtTitle(currentLang), color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(AppStrings.confirmDeleteExtMsg(ext.name, currentLang), color = TextSecondary)
            },
            confirmButton = {
                CineStreamLoadingButton(
                    onClick = {
                        extensionsViewModel.deleteExtension(ext.id)
                        extensionToDelete = null
                    },
                    containerColor = MetricRed,
                    shape = RoundedCornerShape(10.dp),
                    text = AppStrings.delete(currentLang)
                )
            },
            dismissButton = {
                TextButton(onClick = { extensionToDelete = null }) {
                    Text(AppStrings.cancel(currentLang), color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // JSON Editor Dialog
    if (showJsonEditorDialog) {
        AlertDialog(
            onDismissRequest = { showJsonEditorDialog = false },
            title = {
                Text(AppStrings.directJsonEditor(currentLang), color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = providersJsonText,
                    onValueChange = { providersJsonText = it },
                    minLines = 8,
                    maxLines = 14,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedBorderColor = CineStreamRed,
                        unfocusedBorderColor = DarkCardBorder
                    )
                )
            },
            confirmButton = {
                CineStreamLoadingButton(
                    onClick = {
                        configViewModel.updateProvidersJson(providersJsonText)
                        showJsonEditorDialog = false
                    },
                    isLoading = isSavingConfig,
                    containerColor = CineStreamRed,
                    shape = RoundedCornerShape(10.dp),
                    text = AppStrings.saveJsonConfig(currentLang)
                )
            },
            dismissButton = {
                TextButton(onClick = { showJsonEditorDialog = false }) {
                    Text(AppStrings.cancel(currentLang), color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun ExtensionFormDialog(
    title: String,
    initialExtension: ExtensionItem?,
    onDismiss: () -> Unit,
    onSave: (ExtensionItem) -> Unit
) {
    val currentLang by AppSettings.language.collectAsState()
    var name by remember { mutableStateOf(initialExtension?.name ?: "") }
    var packageName by remember { mutableStateOf(initialExtension?.packageName ?: "") }
    var versionName by remember { mutableStateOf(initialExtension?.versionName ?: "1.0.0") }
    var versionCode by remember { mutableStateOf(initialExtension?.versionCode?.toString() ?: "1") }
    var apkUrl by remember { mutableStateOf(initialExtension?.apkUrl ?: "") }
    var apkSha256 by remember { mutableStateOf(initialExtension?.apkSha256 ?: initialExtension?.sha256 ?: "") }
    var enabled by remember { mutableStateOf(initialExtension?.enabled ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(AppStrings.extensionNameField(currentLang), color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CineStreamRed,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text(AppStrings.extensionPackageField(currentLang), color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CineStreamRed,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = versionName,
                        onValueChange = { versionName = it },
                        label = { Text(AppStrings.extensionVersionField(currentLang), color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
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
                        value = versionCode,
                        onValueChange = { versionCode = it.filter { ch -> ch.isDigit() } },
                        label = { Text(AppStrings.versionCodeField(currentLang), color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
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
                    value = apkUrl,
                    onValueChange = { apkUrl = it },
                    label = { Text(AppStrings.extensionApkUrlField(currentLang), color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CineStreamRed,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = apkSha256,
                    onValueChange = { apkSha256 = it },
                    label = { Text(if (currentLang == AppLanguage.ARABIC) "SHA-256 للتحقق (اختياري)" else "SHA-256 Hash (Optional)", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CineStreamRed,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(AppStrings.extensionEnabledToggle(currentLang), color = Color.White, fontSize = 13.sp)
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CineStreamRed
                        )
                    )
                }
            }
        },
        confirmButton = {
            CineStreamLoadingButton(
                onClick = {
                    val resolvedSha = apkSha256.trim()
                    val finalItem = (initialExtension ?: ExtensionItem()).copy(
                        name = name.trim(),
                        packageName = packageName.trim(),
                        versionName = versionName.trim(),
                        versionCode = versionCode.toIntOrNull() ?: 1,
                        apkUrl = apkUrl.trim(),
                        apkSha256 = resolvedSha,
                        sha256 = resolvedSha,
                        enabled = enabled
                    )
                    onSave(finalItem)
                },
                enabled = name.isNotBlank() && packageName.isNotBlank(),
                containerColor = CineStreamRed,
                shape = RoundedCornerShape(10.dp),
                text = AppStrings.save(currentLang)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.cancel(currentLang), color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}
