package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diagnostics.AppLogger
import com.example.diagnostics.CrashReportActivity
import com.example.ui.components.LocalWindowSizeClass
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.AppTheme
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryBlue

class MainActivity : ComponentActivity() {

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AppLogger.i("MainActivity", "MainActivity onCreate started")

    enableEdgeToEdge()
    setContent {
      val windowSizeClass = calculateWindowSizeClass(this)
      val isDarkTheme by com.example.state.AppSettings.isDarkTheme.collectAsState()
      CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
        AppTheme(isDarkTheme = isDarkTheme) {
        var uiError by remember { mutableStateOf<String?>(null) }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          if (uiError != null) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(20.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(48.dp))
                  Spacer(modifier = Modifier.height(12.dp))
                  Text("Interface Rendering Error", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(uiError ?: "", color = ErrorRed, fontSize = 13.sp)
                  Spacer(modifier = Modifier.height(16.dp))
                  Button(
                    onClick = {
                      val intent = Intent(this@MainActivity, CrashReportActivity::class.java)
                      startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                  ) {
                    Text("View Detailed Diagnostics")
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  OutlinedButton(onClick = { uiError = null }) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry")
                  }
                }
              }
            }
          } else {
            AppNavigation()
          }
        }
      }
    }
    }
  }

  override fun onResume() {
    super.onResume()
    AppLogger.d("MainActivity", "MainActivity onResume")
  }

  override fun onPause() {
    super.onPause()
    AppLogger.d("MainActivity", "MainActivity onPause")
  }

  override fun onDestroy() {
    super.onDestroy()
    AppLogger.i("MainActivity", "MainActivity onDestroy")
  }
}

