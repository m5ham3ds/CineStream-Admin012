import os

def write_file(path, content):
    with open(path, 'w') as f:
        f.write(content)

app_nav = """package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val subtitle: String, val icon: ImageVector) {
    object Login : Screen("login", "Login", "Authenticate", Icons.Default.Lock)
    object Dashboard : Screen("dashboard", "Dashboard", "Overview", Icons.Default.Dashboard)
    object Config : Screen("config", "Config", "Settings & OTA", Icons.Default.Settings)
    object Notifications : Screen("notifications", "Notifications", "Push alerts", Icons.Default.Notifications)
    object Profile : Screen("profile", "Profile", "Admin Account", Icons.Default.Person)
    object UserDetail : Screen("user_detail/{userId}", "User Detail", "Manage user", Icons.Default.Person) {
        fun createRoute(userId: String) = "user_detail/$userId"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val isMainScreen = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.Config.route,
        Screen.Notifications.route
    )

    val currentScreen = listOf(Screen.Dashboard, Screen.Config, Screen.Notifications).find { it.route == currentRoute }
    var isDarkTheme by remember { mutableStateOf(true) }

    // Wrap the drawer in RTL so it opens from the right
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = isMainScreen,
            drawerContent = {
                if (isMainScreen) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        ModalDrawerSheet(
                            drawerContainerColor = DarkSurface,
                            modifier = Modifier.width(320.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                brush = Brush.linearGradient(listOf(Color(0xFF6C63FF), Color(0xFF3F3D56))),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Logo", tint = Color.White, modifier = Modifier.size(32.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("CineStream", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                        Text("Admin Panel", color = PrimaryBlue, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                DrawerItem(
                                    icon = Icons.Default.Person,
                                    title = "My Profile",
                                    subtitle = "Account settings",
                                    selected = currentRoute == Screen.Profile.route,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        navController.navigate(Screen.Profile.route)
                                    }
                                )

                                Spacer(modifier = Modifier.height(24.dp))
                                Text("SYSTEM", color = TextSecondary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(16.dp))

                                DrawerItem(icon = Icons.Default.Security, title = "Security", subtitle = "Logs & audits", selected = false) { }
                                Spacer(modifier = Modifier.height(8.dp))
                                DrawerItem(icon = Icons.Default.History, title = "Activity Logs", subtitle = "Recent actions", selected = false) { }

                                Spacer(modifier = Modifier.weight(1f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Dark Theme", color = Color.White)
                                    Switch(
                                        checked = isDarkTheme,
                                        onCheckedChange = { isDarkTheme = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryBlue)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            coroutineScope.launch { drawerState.close() }
                                            FirebaseAuth.getInstance().signOut()
                                            navController.navigate(Screen.Login.route) { popUpTo(0) }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color(0xFFFF5252))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Log Out", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("v1.0.0", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Scaffold(
                    containerColor = DarkBackground,
                    topBar = {
                        if (isMainScreen && currentScreen != null) {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(currentScreen.title, color = Color.White, fontWeight = FontWeight.Bold)
                                        if (currentScreen == Screen.Dashboard) {
                                            Text("Welcome back, Admin", color = PrimaryBlue, style = MaterialTheme.typography.bodySmall)
                                        } else {
                                            Text(currentScreen.subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                },
                                actions = {
                                    // Move the hamburger menu to the right side (actions)
                                    IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = DarkBackground,
                                    titleContentColor = Color.White
                                )
                            )
                        }
                    },
                    bottomBar = {
                        if (isMainScreen) {
                            NavigationBar(
                                containerColor = DarkSurface,
                                contentColor = TextSecondary,
                                tonalElevation = 0.dp
                            ) {
                                listOf(Screen.Dashboard, Screen.Config, Screen.Notifications).forEach { screen ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                    NavigationBarItem(
                                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                                        label = { Text(screen.title) },
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = PrimaryBlue,
                                            selectedTextColor = PrimaryBlue,
                                            unselectedIconColor = TextSecondary,
                                            unselectedTextColor = TextSecondary,
                                            indicatorColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    val startDest = try {
                        if (FirebaseAuth.getInstance().currentUser != null) Screen.Dashboard.route else Screen.Login.route
                    } catch (e: Exception) {
                        Screen.Login.route
                    }
                    
                    NavHost(
                        navController = navController,
                        startDestination = startDest,
                        modifier = Modifier.padding(innerPadding).background(DarkBackground)
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
                        composable(Screen.Config.route) {
                            ConfigScreen()
                        }
                        composable(Screen.Notifications.route) {
                            NotificationScreen()
                        }
                        composable(Screen.Profile.route) {
                            AdminProfileScreen(
                                onLogoutClick = {
                                    FirebaseAuth.getInstance().signOut()
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
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
}

@Composable
fun DrawerItem(icon: ImageVector, title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val modifier = if (selected) {
        Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(PrimaryBlue.copy(alpha = 0.15f), Color.Transparent)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(1.dp, PrimaryBlue.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    } else {
        Modifier.fillMaxWidth()
    }
    
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) PrimaryBlue else TextSecondary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(PrimaryBlue.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            }
        } else {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}
"""

login_screen = """package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.theme.*
import com.example.viewmodels.LoginViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val webClientId = context.getString(R.string.default_web_client_id)

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    // Outer background layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Header / Logo
            if (isSignUpMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isSignUpMode = false }) {
                        Icon(painterResource(android.R.drawable.ic_menu_revert), contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = buildAnnotatedString {
                            append("Already have an account? ")
                            withStyle(style = SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.Bold)) {
                                append("Sign In")
                            }
                        },
                        color = TextSecondary,
                        modifier = Modifier.clickable { isSignUpMode = false }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(Color(0xFF6C63FF), Color(0xFF3F3D56))),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Logo", tint = Color.White, modifier = Modifier.size(50.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = buildAnnotatedString {
                    append("CineStream ")
                    withStyle(style = SpanStyle(color = PrimaryBlue)) { append("Admin") }
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Administrator Panel", color = TextSecondary)
            
            Spacer(modifier = Modifier.height(32.dp))

            // Main Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    if (isSignUpMode) {
                        Text("Create Admin Account", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Fill in the details to create your admin account", color = TextSecondary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        CustomTextField(
                            value = "", 
                            onValueChange = { },
                            label = "Full Name",
                            icon = Icons.Default.Person
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Text("Welcome Back", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Sign in to access your admin dashboard", color = TextSecondary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    CustomTextField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChanged,
                        label = "Admin Email",
                        icon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    CustomTextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChanged,
                        label = "Password",
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityToggle = { passwordVisible = !passwordVisible }
                    )

                    if (isSignUpMode) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CustomTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = "Confirm Password",
                            icon = Icons.Default.Lock,
                            isPassword = true,
                            passwordVisible = confirmPasswordVisible,
                            onPasswordVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = buildAnnotatedString {
                                append("Password strength: ")
                                withStyle(style = SpanStyle(color = Color(0xFFFF5252))) { append("Weak") }
                            },
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.height(4.dp).weight(1f).background(Color(0xFFFF5252), RoundedCornerShape(2.dp)))
                            Box(modifier = Modifier.height(4.dp).weight(1f).background(DarkBackground, RoundedCornerShape(2.dp)))
                            Box(modifier = Modifier.height(4.dp).weight(1f).background(DarkBackground, RoundedCornerShape(2.dp)))
                            Box(modifier = Modifier.height(4.dp).weight(1f).background(DarkBackground, RoundedCornerShape(2.dp)))
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = { rememberMe = it },
                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue, uncheckedColor = TextSecondary)
                                )
                                Text("Remember me", color = TextSecondary, fontSize = 14.sp)
                            }
                            Text("Forgot Password?", color = PrimaryBlue, fontSize = 14.sp, modifier = Modifier.clickable { /* Handle forgot password */ })
                        }
                    }
                    
                    if (uiState.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uiState.error!!, color = Color(0xFFFF5252), fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Gradient Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(
                                brush = Brush.horizontalGradient(listOf(Color(0xFF6C63FF), Color(0xFF3F3D56))),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = !uiState.isLoading) {
                                if (isSignUpMode) {
                                    if (uiState.password == confirmPassword) viewModel.signUp() else viewModel.setError("Passwords don't match")
                                } else {
                                    viewModel.login()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isSignUpMode) "Create Account" else "Sign In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(painterResource(android.R.drawable.ic_media_next), contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = DarkBackground)
                        Text("OR", color = TextSecondary, modifier = Modifier.padding(horizontal = 16.dp))
                        HorizontalDivider(modifier = Modifier.weight(1f), color = DarkBackground)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Google Sign In
                    OutlinedButton(
                        onClick = {
                            viewModel.setGoogleSignInLoading(true)
                            coroutineScope.launch {
                                try {
                                    val credentialManager = CredentialManager.create(context)
                                    val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                                        .setFilterByAuthorizedAccounts(false)
                                        .setServerClientId(webClientId)
                                        .setAutoSelectEnabled(true)
                                        .build()
                                    val request: GetCredentialRequest = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()
                                    val result = credentialManager.getCredential(
                                        request = request,
                                        context = context
                                    )
                                    val credential = result.credential
                                    if (credential is CustomCredential &&
                                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                        viewModel.signInWithGoogleToken(googleIdTokenCredential.idToken)
                                    } else {
                                        viewModel.setError("Unexpected credential type.")
                                    }
                                } catch (e: Exception) {
                                    viewModel.setError(e.message ?: "Google Sign In failed")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkBackground, contentColor = Color.White),
                        border = null
                    ) {
                        Text(if (isSignUpMode) "Sign up with Google" else "Sign in with Google", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (isSignUpMode) {
                        Text(
                            text = buildAnnotatedString {
                                append("By creating an account, you agree to our\n")
                                withStyle(style = SpanStyle(color = PrimaryBlue)) { append("Terms of Service") }
                                append(" and ")
                                withStyle(style = SpanStyle(color = PrimaryBlue)) { append("Privacy Policy") }
                            },
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = buildAnnotatedString {
                                append("Don't have an account? ")
                                withStyle(style = SpanStyle(color = PrimaryBlue)) { append("Sign Up") }
                            },
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().clickable { isSignUpMode = true }
                        )
                    }
                }
            }
            
            if (!isSignUpMode) {
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Secure admin access", color = TextSecondary, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Your data is protected with industry-standard security.", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityToggle: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = TextSecondary) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = TextSecondary) },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = "Toggle Password Visibility",
                        tint = TextSecondary
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkBackground,
            unfocusedContainerColor = DarkBackground,
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = Color(0xFF2A2D3E),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        singleLine = true
    )
}
"""

write_file('app/src/main/java/com/example/ui/navigation/AppNavigation.kt', app_nav)
write_file('app/src/main/java/com/example/ui/screens/LoginScreen.kt', login_screen)
print("Files updated successfully")
