package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
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
import com.example.models.User
import com.example.state.AppSettings
import com.example.state.AppStrings
import com.example.ui.components.AdaptiveScreenContainer
import com.example.ui.components.CineStreamLoadingButton
import com.example.ui.components.bounceClick
import com.example.ui.theme.*
import com.example.viewmodels.DashboardViewModel
import com.example.viewmodels.UserFilter
import com.example.viewmodels.UserSort
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    onUserClick: (String) -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val currentLang by AppSettings.language.collectAsState()
    val users by viewModel.users.collectAsState()
    val totalUsers by viewModel.totalUsers.collectAsState()
    val activeUsers by viewModel.activeUsers.collectAsState()
    val inactiveUsers by viewModel.inactiveUsers.collectAsState()
    val proUsersCount by viewModel.proUsersCount.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()
    val currentSort by viewModel.sort.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showAddUserDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<User?>(null) }
    var showFilterDropdown by remember { mutableStateOf(false) }

    AdaptiveScreenContainer(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Title and "Add User" Action Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = AppStrings.users(currentLang),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = AppStrings.usersSubtitle(currentLang),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                // Primary Add User Button (CineStream Red) with tactile bounce
                CineStreamLoadingButton(
                    onClick = { showAddUserDialog = true },
                    leadingIcon = Icons.Default.PersonAdd,
                    containerColor = CineStreamRed,
                    shape = RoundedCornerShape(12.dp),
                    text = AppStrings.addUser(currentLang)
                )
            }

        Spacer(modifier = Modifier.height(16.dp))

        // Metric Cards Row (4 cards matching the reference image)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Total Users (Blue Accent)
            MetricCard(
                value = totalUsers.toString(),
                label = AppStrings.totalUsers(currentLang),
                icon = Icons.Default.Group,
                accentColor = MetricBlue,
                iconBg = MetricBlueBg,
                modifier = Modifier.weight(1f)
            )

            // Card 2: Active Now (Green Accent)
            MetricCard(
                value = activeUsers.toString(),
                label = AppStrings.activeNow(currentLang),
                icon = Icons.Default.TrendingUp,
                accentColor = MetricGreen,
                iconBg = MetricGreenBg,
                modifier = Modifier.weight(1f)
            )

            // Card 3: PRO Users (Purple Accent)
            MetricCard(
                value = proUsersCount.toString(),
                label = AppStrings.proUsers(currentLang),
                icon = Icons.Default.Star,
                accentColor = MetricPurple,
                iconBg = MetricPurpleBg,
                modifier = Modifier.weight(1f)
            )

            // Card 4: Inactive (Red Accent)
            MetricCard(
                value = inactiveUsers.toString(),
                label = AppStrings.inactive(currentLang),
                icon = Icons.Default.PersonOff,
                accentColor = MetricRed,
                iconBg = MetricRedBg,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Input and Filter Tune Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        AppStrings.searchUsersPlaceholder(currentLang),
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = AppStrings.search(currentLang),
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = AppStrings.reset(currentLang), tint = TextSecondary)
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = DarkSurface,
                    focusedContainerColor = DarkSurface,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedBorderColor = CineStreamRed,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Filter / Tune Squircle Button
            Box {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(DarkSurface, RoundedCornerShape(14.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                        .clickable { showFilterDropdown = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Tune Filters",
                        tint = if (currentFilter != UserFilter.ALL || currentSort != UserSort.NEWEST) CineStreamRed else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                DropdownMenu(
                    expanded = showFilterDropdown,
                    onDismissRequest = { showFilterDropdown = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text(AppStrings.sortNewestFirst(currentLang), color = Color.White) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = CineStreamRed) },
                        onClick = {
                            viewModel.setSort(UserSort.NEWEST)
                            showFilterDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(AppStrings.sortRecent(currentLang), color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = TextSecondary) },
                        onClick = {
                            viewModel.setSort(UserSort.RECENT_LOGIN)
                            showFilterDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(AppStrings.sortName(currentLang), color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null, tint = TextSecondary) },
                        onClick = {
                            viewModel.setSort(UserSort.USERNAME)
                            showFilterDropdown = false
                        }
                    )
                    HorizontalDivider(color = DarkCardBorder)
                    DropdownMenuItem(
                        text = { Text(AppStrings.resetSearchAndFilters(currentLang), color = CineStreamRed) },
                        leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = CineStreamRed) },
                        onClick = {
                            viewModel.resetFilters()
                            showFilterDropdown = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row (Matching screenshot chips: All, Active, Inactive, PRO, Sort)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "All" Chip
            FilterChipItem(
                label = AppStrings.filterAll(currentLang),
                icon = Icons.Default.Group,
                isSelected = currentFilter == UserFilter.ALL,
                onClick = { viewModel.setFilter(UserFilter.ALL) }
            )

            // "Active" Chip (with Green dot indicator)
            StatusFilterChip(
                label = AppStrings.filterActive(currentLang),
                dotColor = MetricGreen,
                isSelected = currentFilter == UserFilter.ACTIVE,
                onClick = { viewModel.setFilter(UserFilter.ACTIVE) }
            )

            // "Inactive" Chip (with Grey dot indicator)
            StatusFilterChip(
                label = AppStrings.filterInactive(currentLang),
                dotColor = TextSecondary,
                isSelected = currentFilter == UserFilter.INACTIVE,
                onClick = { viewModel.setFilter(UserFilter.INACTIVE) }
            )

            // "PRO" Chip (with Crown/Star)
            FilterChipItem(
                label = AppStrings.filterPro(currentLang),
                icon = Icons.Default.Star,
                isSelected = currentFilter == UserFilter.PREMIUM,
                onClick = { viewModel.setFilter(UserFilter.PREMIUM) },
                iconTint = MetricPurple
            )

            // Sort Toggle Chip ("Newest First")
            Box(
                modifier = Modifier
                    .background(DarkSurface, RoundedCornerShape(20.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp))
                    .clickable {
                        val nextSort = when (currentSort) {
                            UserSort.NEWEST -> UserSort.RECENT_LOGIN
                            UserSort.RECENT_LOGIN -> UserSort.USERNAME
                            UserSort.USERNAME -> UserSort.NEWEST
                        }
                        viewModel.setSort(nextSort)
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (currentSort) {
                            UserSort.NEWEST -> AppStrings.sortNewestFirst(currentLang)
                            UserSort.RECENT_LOGIN -> AppStrings.sortRecent(currentLang)
                            UserSort.USERNAME -> AppStrings.sortName(currentLang)
                        },
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section Title: "User List" + Count Badge & Refresh Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = AppStrings.userListTitle(currentLang),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pill badge showing count
                Box(
                    modifier = Modifier
                        .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = AppStrings.userCountBadge(users.size, currentLang),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.refresh() },
                    enabled = !isRefreshing,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = CineStreamRed
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = AppStrings.refresh(currentLang),
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // User List Items
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CineStreamRed)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(AppStrings.syncingUsers(currentLang), color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else if (users.isEmpty()) {
            val isSearching = searchQuery.isNotBlank() || currentFilter != UserFilter.ALL
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        if (isSearching) Icons.Default.SearchOff else Icons.Default.PeopleOutline,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (isSearching) AppStrings.noUsersMatch(searchQuery, currentLang) else AppStrings.noUsersYet(currentLang),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        AppStrings.usersAutoSyncDesc(currentLang),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    if (isSearching) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { viewModel.resetFilters() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CineStreamRed),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(CineStreamRed, CineStreamRed)))
                        ) {
                            Text(AppStrings.resetSearchAndFilters(currentLang))
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(users, key = { it.id }) { user ->
                    UserCardItem(
                        user = user,
                        onClick = { onUserClick(user.id) },
                        onTogglePro = { viewModel.toggleUserPremium(user) },
                        onToggleBan = { viewModel.toggleUserBan(user) },
                        onDelete = { userToDelete = user }
                    )
                }
            }
        }
    }

    // Add User Dialog
    if (showAddUserDialog) {
        AddUserDialog(
            onDismiss = { showAddUserDialog = false },
            onConfirm = { username, email, role, isPro ->
                viewModel.createUser(
                    username = username,
                    email = email,
                    role = role,
                    isPremium = isPro,
                    onSuccess = { showAddUserDialog = false }
                )
            }
        )
    }

    // Confirm Delete User Dialog
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = {
                Text(
                    AppStrings.confirmDeleteUserTitle(currentLang),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "${AppStrings.confirmDeleteUserMsg(currentLang)}\n\n(${user.username} - ${user.email})",
                    color = TextSecondary
                )
            },
            confirmButton = {
                CineStreamLoadingButton(
                    onClick = {
                        viewModel.deleteUser(user.id)
                        userToDelete = null
                    },
                    containerColor = MetricRed,
                    shape = RoundedCornerShape(10.dp),
                    text = AppStrings.delete(currentLang)
                )
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text(AppStrings.cancel(currentLang), color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
    }
}

/**
 * Metric Card with icon, bold value, label, and bottom glowing accent line
 */
@Composable
private fun MetricCard(
    value: String,
    label: String,
    icon: ImageVector,
    accentColor: Color,
    iconBg: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(DarkSurface, RoundedCornerShape(14.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(iconBg.copy(alpha = 0.5f), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Value
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Label
            Text(
                text = label,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }

        // Bottom subtle glowing accent bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(accentColor.copy(alpha = 0.2f), accentColor, accentColor.copy(alpha = 0.2f))
                    ),
                    shape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
                )
        )
    }
}

/**
 * Filter Chip Item (Capsule pill)
 */
@Composable
private fun FilterChipItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    iconTint: Color? = null
) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) CineStreamRed else DarkSurface,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) CineStreamRed else DarkCardBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .bounceClick(scaleDown = 0.93f, shape = RoundedCornerShape(20.dp)) { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else (iconTint ?: TextSecondary),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else TextSecondary
            )
        }
    }
}

/**
 * Status Filter Chip (Active / Inactive with dot)
 */
@Composable
private fun StatusFilterChip(
    label: String,
    dotColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) CineStreamRed else DarkSurface,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) CineStreamRed else DarkCardBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .bounceClick(scaleDown = 0.93f, shape = RoundedCornerShape(20.dp)) { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (isSelected) Color.White else dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else TextSecondary
            )
        }
    }
}

/**
 * User Item Card matching the reference design exactly
 */
@Composable
private fun UserCardItem(
    user: User,
    onClick: () -> Unit,
    onTogglePro: () -> Unit,
    onToggleBan: () -> Unit,
    onDelete: () -> Unit
) {
    val currentLang by AppSettings.language.collectAsState()
    val threshold = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
    val isActive = user.lastLoginTimestamp >= threshold
    val isBanned = user.chatBan || user.storyBan || user.downloadBan || user.p2pBan || user.watchBan
    var showMenu by remember { mutableStateOf(false) }

    val dateStr = if (user.lastLoginTimestamp > 0L) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(user.lastLoginTimestamp))
    } else {
        AppStrings.neverLoggedIn(currentLang)
    }

    val initialLetter = (user.username.firstOrNull() ?: user.email.firstOrNull() ?: 'U').uppercaseChar().toString()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(14.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
            .bounceClick(scaleDown = 0.97f, shape = RoundedCornerShape(14.dp)) { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Trailing status & arrow on the far edge (depending on RTL / LTR)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Date and Status (Active/Inactive dot)
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.widthIn(min = 90.dp)
        ) {
            Text(
                text = dateStr,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            color = if (isBanned) MetricRed else if (isActive) MetricGreen else TextSecondary,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isBanned) AppStrings.restrictedBadge(currentLang) else if (isActive) AppStrings.activeStatus(currentLang) else AppStrings.inactiveStatus(currentLang),
                    fontSize = 11.sp,
                    color = if (isBanned) MetricRed else if (isActive) MetricGreen else TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Username and Email
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (user.isPremium) {
                    Box(
                        modifier = Modifier
                            .background(MetricPurpleBg.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, MetricPurple, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(AppStrings.proBadge(currentLang), color = MetricPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (user.role == "admin") {
                    Box(
                        modifier = Modifier
                            .background(CineStreamRed.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(AppStrings.adminBadge(currentLang), color = CineStreamRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = user.username.ifBlank { "User" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = user.email.ifBlank { user.id },
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Circular Avatar with Letter
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Color(0xFF202532), CircleShape)
                .border(1.dp, if (user.isPremium) MetricPurple else DarkCardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initialLetter,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 3-dots Menu Button
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(DarkSurface)
            ) {
                DropdownMenuItem(
                    text = { Text(AppStrings.viewDetails(currentLang), color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, tint = CineStreamRed) },
                    onClick = {
                        showMenu = false
                        onClick()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (user.isPremium) AppStrings.revokePro(currentLang) else AppStrings.upgradeToPro(currentLang),
                            color = MetricPurple
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = MetricPurple) },
                    onClick = {
                        showMenu = false
                        onTogglePro()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (isBanned) AppStrings.unbanUser(currentLang) else AppStrings.restrictUser(currentLang),
                            color = if (isBanned) MetricGreen else MetricRed
                        )
                    },
                    leadingIcon = {
                        Icon(
                            if (isBanned) Icons.Default.CheckCircle else Icons.Default.Block,
                            contentDescription = null,
                            tint = if (isBanned) MetricGreen else MetricRed
                        )
                    },
                    onClick = {
                        showMenu = false
                        onToggleBan()
                    }
                )
                HorizontalDivider(color = DarkCardBorder)
                DropdownMenuItem(
                    text = { Text(AppStrings.deleteUser(currentLang), color = MetricRed) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MetricRed) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

/**
 * Add New User Dialog
 */
@Composable
private fun AddUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (username: String, email: String, role: String, isPro: Boolean) -> Unit
) {
    val currentLang by AppSettings.language.collectAsState()
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isPro by remember { mutableStateOf(false) }
    var isAdmin by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = CineStreamRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    AppStrings.addNewUserDialogTitle(currentLang),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(AppStrings.username(currentLang), color = TextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
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
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(AppStrings.email(currentLang), color = TextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
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

                // Toggle PRO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(AppStrings.proAccountCheckbox(currentLang), color = Color.White, fontSize = 13.sp)
                    Switch(
                        checked = isPro,
                        onCheckedChange = { isPro = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MetricPurple
                        )
                    )
                }

                // Toggle Admin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(AppStrings.adminRoleCheckbox(currentLang), color = Color.White, fontSize = 13.sp)
                    Switch(
                        checked = isAdmin,
                        onCheckedChange = { isAdmin = it },
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
                    if (username.isNotBlank() && email.isNotBlank()) {
                        onConfirm(username.trim(), email.trim(), if (isAdmin) "admin" else "user", isPro)
                    }
                },
                enabled = username.isNotBlank() && email.isNotBlank(),
                containerColor = CineStreamRed,
                shape = RoundedCornerShape(10.dp),
                text = AppStrings.add(currentLang)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.cancel(currentLang), color = TextSecondary)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
