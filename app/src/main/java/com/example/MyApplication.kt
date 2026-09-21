package com.example

import android.app.Application
import com.example.diagnostics.AppLogger
import com.example.diagnostics.CrashHandler
import com.google.firebase.FirebaseApp

object AppState {
    var firebaseInitialized: Boolean = false
    var firebaseInitError: String? = null
}

class MyApplication : Application() {

    companion object {
        lateinit var instance: MyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 1. Initialize instant real-time logger & disk writer immediately
        AppLogger.init(this)

        // 2. Initialize AppSettings (theme, language, notifications)
        com.example.state.AppSettings.init(this)
        
        // 3. Install comprehensive crash interceptor
        CrashHandler.install(this)
        
        AppLogger.i("MyApplication", "Application onCreate started")

        // 3. Initialize Firebase safely with guaranteed fallbacks
        val success = com.example.diagnostics.FirebaseInitializer.init(this)
        if (success) {
            AppLogger.i("MyApplication", "Firebase is ready for use")
        } else {
            AppLogger.e("MyApplication", "Firebase initialization could not be completed: ${AppState.firebaseInitError}")
        }
    }
}

