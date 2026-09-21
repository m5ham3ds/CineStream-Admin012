package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diagnostics.AppLogger
import com.example.diagnostics.LogEntry
import com.example.diagnostics.LogLevel
import com.example.state.AppSettings
import com.example.state.AppStrings
import com.example.ui.components.AdaptiveScreenContainer
import com.example.ui.components.CineStreamLoadingButton
import com.example.ui.components.bounceClick
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DiagnosticScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val logs by AppLogger.logsState.collectAsState()
    val listState = rememberLazyListState()
    val currentLang by AppSettings.language.collectAsState()

    var selectedLevel by remember { mutableStateOf<LogLevel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var autoScroll by remember { mutableStateOf(true) }
    var showCrashReportDialog by remember { mutableStateOf(false) }
    var lastCrashReportText by remember { mutableStateOf("") }

    val filteredLogs = remember(logs, selectedLevel, searchQuery) {
        logs.filter { entry ->
            val matchesLevel = selectedLevel == null || entry.level == selectedLevel
            val matchesQuery = searchQuery.isBlank() ||
                    entry.message.contains(searchQuery, ignoreCase = true) ||
                    entry.tag.contains(searchQuery, ignoreCase = true) ||
                    (entry.throwableStackTrace?.contains(searchQuery, ignoreCase = true) == true)
            matchesLevel && matchesQuery
        }
    }

    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    if (showCrashReportDialog) {
        AlertDialog(
            onDismissRequest = { showCrashReportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MetricRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crash Diagnostics", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = lastCrashReportText.ifEmpty { "No crash records found. All system components operating normally." },
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                CineStreamLoadingButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Crash Report", lastCrashReportText))
                        Toast.makeText(context, AppStrings.copied(currentLang), Toast.LENGTH_SHORT).show()
                    },
                    containerColor = CineStreamRed,
                    shape = RoundedCornerShape(10.dp),
                    text = AppStrings.copy(currentLang)
                )
            },
            dismissButton = {
                TextButton(onClick = { showCrashReportDialog = false }) {
                    Text(AppStrings.close(currentLang), color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

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
                        text = AppStrings.diagnostics(currentLang),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = AppStrings.diagnosticsSubtitle(currentLang),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(DarkSurface, RoundedCornerShape(10.dp))
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                            .bounceClick(scaleDown = 0.88f, shape = RoundedCornerShape(10.dp)) {
                                lastCrashReportText = AppLogger.getCrashFile()?.readText() ?: "No crash reports recorded."
                                showCrashReportDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = "Crash Report", tint = MetricRed, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(DarkSurface, RoundedCornerShape(10.dp))
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                            .bounceClick(scaleDown = 0.88f, shape = RoundedCornerShape(10.dp)) {
                                AppLogger.clearMemoryLogs()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(AppStrings.search(currentLang), color = TextSecondary, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
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

            // Level Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LogLevelChip(label = AppStrings.filterAll(currentLang), isSelected = selectedLevel == null, onClick = { selectedLevel = null })
                LogLevelChip(label = "INFO", isSelected = selectedLevel == LogLevel.INFO, onClick = { selectedLevel = LogLevel.INFO }, color = MetricBlue)
                LogLevelChip(label = "WARN", isSelected = selectedLevel == LogLevel.WARN, onClick = { selectedLevel = LogLevel.WARN }, color = WarningOrange)
                LogLevelChip(label = "ERROR", isSelected = selectedLevel == LogLevel.ERROR, onClick = { selectedLevel = LogLevel.ERROR }, color = MetricRed)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Logs Terminal Box
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkCardBorder, DarkCardBorder))),
                modifier = Modifier.fillMaxSize().weight(1f)
            ) {
                if (filteredLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No logs recorded", color = TextSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredLogs) { log ->
                            LogItemRow(log = log)
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun LogLevelChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color = CineStreamRed
) {
    val chipShape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .background(
                if (isSelected) color.copy(alpha = 0.2f) else DarkSurface,
                chipShape
            )
            .border(
                1.dp,
                if (isSelected) color else DarkCardBorder,
                chipShape
            )
            .bounceClick(scaleDown = 0.92f, shape = chipShape) { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) color else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun LogItemRow(log: LogEntry) {
    val levelColor = when (log.level) {
        LogLevel.DEBUG -> MetricBlue
        LogLevel.INFO -> MetricGreen
        LogLevel.WARN -> WarningOrange
        LogLevel.ERROR, LogLevel.CRASH -> MetricRed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "[${log.level.name.take(1)}]",
            color = levelColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(log.tag, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Text(log.timeFormatted, color = TextSecondary, fontSize = 10.sp)
            }
            Text(log.message, color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
    }
}
