package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.models.ExtensionItem
import com.example.ui.components.AdaptiveScreenContainer
import com.example.ui.components.CineStreamLoadingButton
import com.example.ui.components.bounceClick
import com.example.ui.theme.*
import com.example.viewmodels.ExtensionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    onBackClick: () -> Unit,
    viewModel: ExtensionsViewModel = viewModel()
) {
    val extensions by viewModel.extensions.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var extensionToDelete by remember { mutableStateOf<ExtensionItem?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extensions Hub", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .bounceClick(scaleDown = 0.88f) { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .bounceClick(scaleDown = 0.88f) { showAddDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Extension", tint = PrimaryBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Extension") },
                modifier = Modifier.bounceClick(scaleDown = 0.92f, shape = RoundedCornerShape(16.dp)) { showAddDialog = true }
            )
        },
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
                    .padding(horizontal = 16.dp)
            ) {
            Text(
                "Manage remote APK scraper plugins distributed to CineStream",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (extensions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Extension, contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No extensions registered yet", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        CineStreamLoadingButton(
                            onClick = { showAddDialog = true },
                            containerColor = PrimaryBlue,
                            shape = RoundedCornerShape(10.dp),
                            text = "Register First Extension"
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(extensions, key = { it.id }) { item ->
                        ExtensionCard(
                            item = item,
                            onToggle = { viewModel.toggleExtension(item) },
                            onDelete = { extensionToDelete = item }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

        extensionToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { extensionToDelete = null },
                title = { Text("Delete Extension?", color = Color.White) },
                text = {
                    Text(
                        "Are you sure you want to delete '${item.name}' (${item.packageName})? CineStream client apps will no longer be able to download or use this extension.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    CineStreamLoadingButton(
                        onClick = {
                            viewModel.deleteExtension(item.id)
                            extensionToDelete = null
                        },
                        containerColor = ErrorRed,
                        shape = RoundedCornerShape(10.dp),
                        text = "Delete"
                    )
                },
                dismissButton = {
                    TextButton(onClick = { extensionToDelete = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = DarkSurface
            )
        }

        if (showAddDialog) {
            AddExtensionDialog(
                onDismiss = { showAddDialog = false },
                onSave = { extension ->
                    viewModel.saveExtension(extension)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun ExtensionCard(
    item: ExtensionItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(14.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (item.enabled) PrimaryBlue.copy(alpha = 0.2f) else DarkBackground, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Extension, contentDescription = null, tint = if (item.enabled) PrimaryBlue else TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("${item.packageName} • v${item.versionName} (${item.versionCode})", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = item.enabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .bounceClick(scaleDown = 0.85f) { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed.copy(alpha = 0.8f))
                    }
                }
            }

            if (item.apkUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("APK: ${item.apkUrl}", style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
            }

            if (item.releaseNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.releaseNotes, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun AddExtensionDialog(
    onDismiss: () -> Unit,
    onSave: (ExtensionItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var versionCode by remember { mutableStateOf("1") }
    var versionName by remember { mutableStateOf("1.0.0") }
    var apkUrl by remember { mutableStateOf("") }
    var sha256 by remember { mutableStateOf("") }
    var releaseNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Extension APK", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Extension Name") },
                    placeholder = { Text("e.g. ArabSeed Scraper") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package Name") },
                    placeholder = { Text("com.cinestream.ext.arabseed") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = versionCode,
                        onValueChange = { versionCode = it },
                        label = { Text("Code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = versionName,
                        onValueChange = { versionName = it },
                        label = { Text("Name") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = apkUrl,
                    onValueChange = { apkUrl = it },
                    label = { Text("Direct APK URL (HTTPS)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sha256,
                    onValueChange = { sha256 = it },
                    label = { Text("SHA-256 Checksum") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = releaseNotes,
                    onValueChange = { releaseNotes = it },
                    label = { Text("Release Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            CineStreamLoadingButton(
                onClick = {
                    if (name.isNotBlank() && packageName.isNotBlank()) {
                        onSave(
                            ExtensionItem(
                                name = name.trim(),
                                packageName = packageName.trim(),
                                versionCode = versionCode.toIntOrNull() ?: 1,
                                versionName = versionName.trim(),
                                apkUrl = apkUrl.trim(),
                                sha256 = sha256.trim(),
                                releaseNotes = releaseNotes.trim(),
                                enabled = true
                            )
                        )
                    }
                },
                containerColor = PrimaryBlue,
                shape = RoundedCornerShape(10.dp),
                text = "Save Extension"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}
