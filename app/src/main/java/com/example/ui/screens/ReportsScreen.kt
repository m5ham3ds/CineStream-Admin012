package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.models.UserReport
import com.example.state.AppSettings
import com.example.state.AppStrings
import com.example.ui.components.AdaptiveScreenContainer
import com.example.ui.components.CineStreamLoadingButton
import com.example.ui.components.bounceClick
import com.example.ui.theme.*
import com.example.viewmodels.ReportsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsScreen(
    onBackClick: () -> Unit,
    viewModel: ReportsViewModel = viewModel()
) {
    val currentLang by AppSettings.language.collectAsState()
    val reports by viewModel.filteredReports.collectAsState()
    val allReports by viewModel.reports.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedReportForResolution by remember { mutableStateOf<UserReport?>(null) }
    var resolutionNotes by remember { mutableStateOf("") }
    var resolutionStatus by remember { mutableStateOf("RESOLVED") }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    if (selectedReportForResolution != null) {
        val rep = selectedReportForResolution!!
        AlertDialog(
            onDismissRequest = { selectedReportForResolution = null },
            title = {
                Text(
                    text = if (currentLang.name == "ARABIC") "معالجة البلاغ" else "Resolve Report",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${rep.title}: ${rep.description}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = resolutionStatus == "RESOLVED",
                            onClick = { resolutionStatus = "RESOLVED" },
                            label = { Text("RESOLVED") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MetricGreenBg,
                                selectedLabelColor = MetricGreen
                            )
                        )
                        FilterChip(
                            selected = resolutionStatus == "DISMISSED",
                            onClick = { resolutionStatus = "DISMISSED" },
                            label = { Text("DISMISSED") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DarkSurfaceVariant,
                                selectedLabelColor = TextSecondary
                            )
                        )
                    }

                    OutlinedTextField(
                        value = resolutionNotes,
                        onValueChange = { resolutionNotes = it },
                        label = { Text(if (currentLang.name == "ARABIC") "ملاحظات الحل" else "Resolution Notes") },
                        placeholder = { Text("e.g. Fixed stream source in v1.2.0") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CineStreamRed,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                CineStreamLoadingButton(
                    onClick = {
                        val rId = rep.id
                        selectedReportForResolution = null
                        viewModel.resolveReport(rId, resolutionStatus, resolutionNotes)
                        resolutionNotes = ""
                    },
                    containerColor = CineStreamRed,
                    shape = RoundedCornerShape(10.dp),
                    text = AppStrings.confirm(currentLang)
                )
            },
            dismissButton = {
                TextButton(onClick = { selectedReportForResolution = null }) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
            // Header stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (currentLang.name == "ARABIC") "بلاغات المستخدمين" else "User Reports & Issues",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (currentLang.name == "ARABIC") "متابعة المشاكل الفنية ومصادر البث" else "Manage issues submitted from CineStream client",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                val pendingCount = allReports.count { it.status.equals("PENDING", ignoreCase = true) }
                Box(
                    modifier = Modifier
                        .background(if (pendingCount > 0) WarningOrangeBg else MetricGreenBg, RoundedCornerShape(12.dp))
                        .border(1.dp, if (pendingCount > 0) WarningOrange else MetricGreen, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$pendingCount Pending",
                        color = if (pendingCount > 0) WarningOrange else MetricGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf("ALL", "PENDING", "RESOLVED", "DISMISSED")
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    val chipShape = RoundedCornerShape(20.dp)
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) CineStreamRed.copy(alpha = 0.2f) else DarkSurface, chipShape)
                            .border(1.dp, if (isSelected) CineStreamRed else DarkCardBorder, chipShape)
                            .bounceClick(scaleDown = 0.92f, shape = chipShape) { viewModel.setFilter(filter) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) CineStreamRed else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (reports.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MetricGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (currentLang.name == "ARABIC") "لا توجد بلاغات تطابق الفلتر" else "No reports match selected filter",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(reports, key = { it.id }) { report ->
                        ReportCard(
                            report = report,
                            onResolve = {
                                selectedReportForResolution = report
                                resolutionStatus = "RESOLVED"
                                resolutionNotes = report.resolutionNotes
                            },
                            onDelete = {
                                viewModel.deleteReport(report.id)
                            }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun ReportCard(
    report: UserReport,
    onResolve: () -> Unit,
    onDelete: () -> Unit
) {
    val isPending = report.status.equals("PENDING", ignoreCase = true)
    val isResolved = report.status.equals("RESOLVED", ignoreCase = true)

    val statusColor = when {
        isPending -> WarningOrange
        isResolved -> MetricGreen
        else -> TextSecondary
    }

    val statusBg = when {
        isPending -> WarningOrangeBg
        isResolved -> MetricGreenBg
        else -> DarkSurfaceVariant
    }

    val dateStr = if (report.createdAt > 0L) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(report.createdAt))
    } else "-"

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(DarkCardBorder, DarkCardBorder))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = report.type,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (report.targetType.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${report.targetType}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(statusBg, RoundedCornerShape(8.dp))
                        .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = report.status,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = report.title.ifBlank { "Untitled Issue" },
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )

            if (report.description.isNotBlank()) {
                Text(
                    text = report.description,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            if (report.resolutionNotes.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Resolution: ${report.resolutionNotes}",
                        color = MetricGreen,
                        fontSize = 12.sp
                    )
                }
            }

            HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = report.userEmail.ifBlank { report.userId.ifBlank { "Anonymous" } },
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = dateStr,
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .bounceClick(scaleDown = 0.85f) { onResolve() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Resolve",
                            tint = MetricGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .bounceClick(scaleDown = 0.85f) { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MetricRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
