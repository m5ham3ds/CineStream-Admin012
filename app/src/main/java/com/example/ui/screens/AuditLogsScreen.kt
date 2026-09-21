package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.models.AuditLog
import com.example.state.AppSettings
import com.example.state.AppStrings
import com.example.ui.components.AdaptiveScreenContainer
import com.example.ui.components.CineStreamLoadingButton
import com.example.ui.components.bounceClick
import com.example.ui.theme.*
import com.example.viewmodels.AuditLogsViewModel
import com.example.viewmodels.LogCategory
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AuditLogsScreen(
    onBackClick: () -> Unit,
    viewModel: AuditLogsViewModel = viewModel()
) {
    val logs by viewModel.logs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.category.collectAsState()
    val currentLang by AppSettings.language.collectAsState()
    var selectedLogForDetail by remember { mutableStateOf<AuditLog?>(null) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
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
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AppStrings.auditLogs(currentLang),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = AppStrings.auditLogsSubtitle(currentLang),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("${logs.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text(AppStrings.search(currentLang), color = TextSecondary, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = DarkSurface,
                    focusedContainerColor = DarkSurface,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedBorderColor = CineStreamRed,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Categories horizontal row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LogCategory.values().forEach { cat ->
                    val isSelected = selectedCategory == cat
                    val chipShape = RoundedCornerShape(20.dp)
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) CineStreamRed else DarkSurface,
                                chipShape
                            )
                            .border(
                                1.dp,
                                if (isSelected) CineStreamRed else DarkCardBorder,
                                chipShape
                            )
                            .bounceClick(scaleDown = 0.92f, shape = chipShape) { viewModel.setCategory(cat) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = cat.name,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No audit events recorded", color = TextSecondary, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        val cardShape = RoundedCornerShape(12.dp)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = cardShape,
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkCardBorder, DarkCardBorder))),
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick(scaleDown = 0.98f, shape = cardShape) { selectedLogForDetail = log }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = log.action,
                                        color = CineStreamRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(log.createdAt)),
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = log.details,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "By: ${log.adminEmail.ifBlank { "System" }}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

    // Detail Dialog
    selectedLogForDetail?.let { log ->
        AlertDialog(
            onDismissRequest = { selectedLogForDetail = null },
            title = {
                Text(log.action, color = CineStreamRed, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Admin: ${log.adminEmail}", color = Color.White, fontSize = 13.sp)
                    Text("Target: ${log.targetType} (${log.targetId})", color = TextSecondary, fontSize = 12.sp)
                    Text("Time: ${Date(log.createdAt)}", color = TextSecondary, fontSize = 12.sp)
                    HorizontalDivider(color = DarkCardBorder)
                    Text(log.details, color = Color.White, fontSize = 13.sp)
                }
            },
            confirmButton = {
                CineStreamLoadingButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString("${log.action}: ${log.details}"))
                        Toast.makeText(context, AppStrings.copied(currentLang), Toast.LENGTH_SHORT).show()
                    },
                    containerColor = CineStreamRed,
                    shape = RoundedCornerShape(10.dp),
                    text = AppStrings.copy(currentLang)
                )
            },
            dismissButton = {
                TextButton(onClick = { selectedLogForDetail = null }) {
                    Text(AppStrings.close(currentLang), color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}
