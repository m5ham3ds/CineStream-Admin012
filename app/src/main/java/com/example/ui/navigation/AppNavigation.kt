package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.state.AppLanguage
import com.example.state.AppSettings
import com.example.state.AppStrings
import com.example.ui.components.CineStreamOutlinedLoadingButton
import com.example.ui.components.bounceClick
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val icon: ImageVector) {
    object Login : Screen("login", Icons.Default.Lock)
    object Dashboard : Screen("dashboard", Icons.Default.Group)
    object AppUpdates : Screen("app_updates", Icons.Default.SystemUpdate)
    object GlobalConfig : Screen("global_config", Icons.Default.AdminPanelSettings)
    object Notifications : Screen("notifications", Icons.Default.Notifications)
    object ExtensionsUpdates : Screen("extensions_updates", Icons.Default.Extension)
    object Settings : Screen("settings", Icons.Default.Settings)
    object Profile : Screen("profile", Icons.Default.Person)
    object AuditLogs : Screen("audit_logs", Icons.Default.Security)
    object Reports : Screen("reports", Icons.Default.Report)
    object Diagnostics : Screen("diagnostics", Icons.Default.BugReport)
    object UserDetail : Screen("user_detail/{userId}", Icons.Default.Person) {
        fun createRoute(userId: String) = "user_detail/$userId"
    }

    fun getTitle(lang: AppLanguage): String = when (this) {
        Login -> AppStrings.loginTitle(lang)
        Dashboard -> AppStrings.users(lang)
        AppUpdates -> AppStrings.appUpdates(lang)
        GlobalConfig -> AppStrings.globalConfig(lang)
        Notifications -> AppStrings.notifications(lang)
        ExtensionsUpdates -> AppStrings.extensionsUpdates(lang)
        Settings -> AppStrings.settings(lang)
        Profile -> AppStrings.profile(lang)
        AuditLogs -> AppStrings.auditLogs(lang)
        Reports -> AppStrings.reports(lang)
        Diagnostics -> AppStrings.diagnostics(lang)
        UserDetail -> AppStrings.userDetailTitle(lang)
    }

    fun getSubtitle(lang: AppLanguage): String = when (this) {
        Login -> AppStrings.loginSubtitle(lang)
        Dashboard -> AppStrings.usersSubtitle(lang)
        AppUpdates -> AppStrings.appUpdatesSubtitle(lang)
        GlobalConfig -> AppStrings.globalConfigSubtitle(lang)
        Notifications -> AppStrings.notificationsSubtitle(lang)
        ExtensionsUpdates -> AppStrings.extensionsSubtitle(lang)
        Settings -> AppStrings.settingsSubtitle(lang)
        Profile -> AppStrings.profileSubtitle(lang)
        AuditLogs -> AppStrings.auditLogsSubtitle(lang)
        Reports -> AppStrings.reportsSubtitle(lang)
        Diagnostics -> AppStrings.diagnosticsSubtitle(lang)
        UserDetail -> AppStrings.userDetailSubtitle(lang)
    }
}

@Composable
fun AppNavigation() {
    val currentLang by AppSettings.language.collectAsState()
    val isDarkTheme by AppSettings.isDarkTheme.collectAsState()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val isAuthScreen = currentRoute == Screen.Login.route
    val isMainScreen = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.AppUpdates.route,
        Screen.GlobalConfig.route,
        Screen.Notifications.route
    )

    val currentScreen = when (currentRoute) {
        Screen.Login.route -> Screen.Login
        Screen.Dashboard.route -> Screen.Dashboard
        Screen.AppUpdates.route -> Screen.AppUpdates
        Screen.GlobalConfig.route -> Screen.GlobalConfig
        Screen.Notifications.route -> Screen.Notifications
        Screen.ExtensionsUpdates.route -> Screen.ExtensionsUpdates
        Screen.Settings.route -> Screen.Settings
        Screen.Profile.route -> Screen.Profile
        Screen.AuditLogs.route -> Screen.AuditLogs
        Screen.Reports.route -> Screen.Reports
        Screen.Diagnostics.route -> Screen.Diagnostics
        Screen.UserDetail.route -> Screen.UserDetail
        else -> null
    }

    val currentUser = remember {
        try {
            FirebaseAuth.getInstance().currentUser
        } catch (e: Exception) {
            null
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides currentLang.layoutDirection) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !isAuthScreen,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = DarkSurface,
                    drawerContentColor = Color.White,
                    modifier = Modifier
                        .width(310.dp)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(16.dp)
                    ) {
                        // CineStream Drawer Brand Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(listOf(DarkSurfaceVariant, DarkSurface)),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(46.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color(0xFF202532), CircleShape)
                                            .border(1.5.dp, CineStreamRed, CircleShape)
                                            .align(Alignment.Center),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = AppStrings.adminLetter(currentLang),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(11.dp)
                                            .background(MetricGreen, CircleShape)
                                            .border(2.dp, DarkSurface, CircleShape)
                                            .align(Alignment.BottomEnd)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = buildAnnotatedString {
                                            append(AppStrings.cine(currentLang))
                                            withStyle(style = SpanStyle(color = CineStreamRed)) { append(AppStrings.stream(currentLang)) }
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = currentUser?.email ?: AppStrings.adminPanel(currentLang),
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Navigation Items
                        DrawerItem(
                            icon = Icons.Default.Group,
                            title = AppStrings.users(currentLang),
                            selected = currentRoute == Screen.Dashboard.route,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        DrawerItem(
                            icon = Icons.Default.SystemUpdate,
                            title = AppStrings.appUpdates(currentLang),
                            selected = currentRoute == Screen.AppUpdates.route,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                navController.navigate(Screen.AppUpdates.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        DrawerItem(
                            icon = Icons.Default.Extension,
                            title = AppStrings.extensionsUpdates(currentLang),
                            selected = currentRoute == Screen.ExtensionsUpdates.route,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                navController.navigate(Screen.ExtensionsUpdates.route)
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        DrawerItem(
                            icon = Icons.Default.AdminPanelSettings,
                            title = AppStrings.globalConfig(currentLang),
                            selected = currentRoute == Screen.GlobalConfig.route,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                navController.navigate(Screen.GlobalConfig.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        DrawerItem(
                            icon = Icons.Default.Notifications,
                            title = AppStrings.notifications(currentLang),
                            selected = currentRoute == Screen.Notifications.route,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                navController.navigate(Screen.Notifications.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = DarkCardBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        DrawerItem(
                            icon = Icons.Default.Security,
                            title = AppStrings.auditLogs(currentLang),
                            selected = currentRoute == Screen.AuditLogs.route,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                navController.navigate(Screen.AuditLogs.route)
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        DrawerItem(
                            icon = Icons.Default.Report,
                            title = AppStrings.reports(currentLang),
                            selected = currentRoute == Screen.Reports.route,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                navController.navigate(Screen.Reports.route)
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        DrawerItem(
                            icon = Icons.Default.BugReport,
                            title = AppStrings.diagnostics(currentLang),
                            selected = currentRoute == Screen.Diagnostics.route,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                navController.navigate(Screen.Diagnostics.route)
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        DrawerItem(
                            icon = Icons.Default.Settings,
                            title = AppStrings.settings(currentLang),
                            selected = currentRoute == Screen.Settings.route,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                navController.navigate(Screen.Settings.route)
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        DrawerItem(
                            icon = Icons.Default.Person,
                            title = AppStrings.profile(currentLang),
                            selected = currentRoute == Screen.Profile.route,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                navController.navigate(Screen.Profile.route)
                            }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Logout button
                        CineStreamOutlinedLoadingButton(
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                try {
                                    FirebaseAuth.getInstance().signOut()
                                } catch (e: Exception) {
                                    // ignore
                                }
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            containerColor = MetricRedBg,
                            contentColor = MetricRed,
                            borderColor = MetricRed,
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = Icons.Default.Logout,
                            modifier = Modifier.fillMaxWidth(),
                            text = AppStrings.logout(currentLang)
                        )
                    }
                }
            }
        ) {
            Scaffold(
                containerColor = DarkBackground,
                topBar = {
                    if (!isAuthScreen) {
                        // Unified CineStream Header on ALL Pages
                        CineStreamGlobalHeader(
                            showBackButton = !isMainScreen,
                            onBackClick = { navController.popBackStack() },
                            onMenuClick = { coroutineScope.launch { drawerState.open() } },
                            currentLang = currentLang
                        )
                    }
                },
                bottomBar = {
                    if (isMainScreen) {
                        // CineStream Branded Bottom Navigation Bar Matching Image
                        CineStreamBottomBar(
                            currentRoute = currentRoute,
                            currentLang = currentLang,
                            onNavigate = { screen ->
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            ) { innerPadding ->
                // Always start at Login to strictly verify admin authorization on cold start
                val startDest = Screen.Login.route

                NavHost(
                    navController = navController,
                    startDestination = startDest,
                    modifier = Modifier
                        .padding(innerPadding)
                        .background(DarkBackground)
                ) {
                    composable(Screen.Login.route) {
                        LoginScreen(onLoginSuccess = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        })
                    }
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(onUserClick = { userId ->
                            navController.navigate(Screen.UserDetail.createRoute(userId))
                        })
                    }
                    composable(Screen.AppUpdates.route) {
                        AppUpdatesScreen()
                    }
                    composable(Screen.GlobalConfig.route) {
                        GlobalConfigScreen()
                    }
                    composable(Screen.Notifications.route) {
                        NotificationScreen()
                    }
                    composable(Screen.ExtensionsUpdates.route) {
                        ExtensionsUpdatesScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onBackClick = { navController.popBackStack() },
                            onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) },
                            onLogoutClick = {
                                try {
                                    FirebaseAuth.getInstance().signOut()
                                } catch (e: Exception) {
                                    // ignore
                                }
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Screen.Profile.route) {
                        AdminProfileScreen(
                            onLogoutClick = {
                                try {
                                    FirebaseAuth.getInstance().signOut()
                                } catch (e: Exception) {
                                    // ignore
                                }
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Screen.AuditLogs.route) {
                        AuditLogsScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Reports.route) {
                        ReportsScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Diagnostics.route) {
                        DiagnosticScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.UserDetail.route) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                        UserDetailScreen(
                            userId = userId,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Unified CineStream Header displayed on every screen across the app
 */
@Composable
private fun CineStreamGlobalHeader(
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    currentLang: AppLanguage
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .border(1.dp, DarkCardBorder)
            .statusBarsPadding()
            .displayCutoutPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showBackButton) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                            .bounceClick(scaleDown = 0.88f, shape = RoundedCornerShape(10.dp)) { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = AppStrings.back(currentLang),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                // Avatar circle with green indicator
                Box(modifier = Modifier.size(36.dp)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFF202532), CircleShape)
                            .border(1.5.dp, CineStreamRed, CircleShape)
                            .align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = AppStrings.adminLetter(currentLang),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(MetricGreen, CircleShape)
                            .border(1.5.dp, DarkSurface, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = buildAnnotatedString {
                            append(AppStrings.cine(currentLang))
                            withStyle(style = SpanStyle(color = CineStreamRed)) { append(AppStrings.stream(currentLang)) }
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    Text(
                        text = AppStrings.adminPanel(currentLang),
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            // Hamburger Menu Icon Button with tactile bounce
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                    .bounceClick(scaleDown = 0.88f, shape = RoundedCornerShape(10.dp)) { onMenuClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = AppStrings.navigationMenu(currentLang),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * CineStream Bottom Navigation Bar with Red Active Capsule Pill
 */
@Composable
private fun CineStreamBottomBar(
    currentRoute: String?,
    currentLang: AppLanguage,
    onNavigate: (Screen) -> Unit
) {
    val bottomScreens = listOf(
        Screen.Dashboard,
        Screen.AppUpdates,
        Screen.GlobalConfig,
        Screen.Notifications
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .border(1.dp, DarkCardBorder)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        bottomScreens.forEach { screen ->
            val isSelected = currentRoute == screen.route
            val pillShape = RoundedCornerShape(12.dp)

            Box(
                modifier = Modifier
                    .background(
                        if (isSelected) CineStreamRed.copy(alpha = 0.15f) else Color.Transparent,
                        pillShape
                    )
                    .border(
                        1.dp,
                        if (isSelected) CineStreamRed.copy(alpha = 0.6f) else Color.Transparent,
                        pillShape
                    )
                    .bounceClick(scaleDown = 0.88f, shape = pillShape) { onNavigate(screen) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        screen.icon,
                        contentDescription = screen.getTitle(currentLang),
                        tint = if (isSelected) CineStreamRed else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = screen.getTitle(currentLang),
                        color = if (isSelected) CineStreamRed else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerItem(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val backgroundModifier = if (selected) {
        Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(CineStreamRed.copy(alpha = 0.18f), Color.Transparent)
                ),
                shape = shape
            )
            .border(1.dp, CineStreamRed.copy(alpha = 0.4f), shape)
    } else {
        Modifier.fillMaxWidth()
    }

    Row(
        modifier = backgroundModifier
            .bounceClick(scaleDown = 0.96f, shape = shape) { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) CineStreamRed else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            title,
            color = if (selected) Color.White else TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(CineStreamRed, CircleShape)
            )
        }
    }
}
