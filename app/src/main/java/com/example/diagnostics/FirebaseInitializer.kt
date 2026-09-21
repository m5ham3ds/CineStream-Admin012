package com.example.diagnostics

import android.content.Context
import com.example.AppState
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Ensures Firebase is initialized reliably on any Android version/device,
 * handling cases where AGP namespace != applicationId or when default
 * string resource lookups return null.
 */
object FirebaseInitializer {
    private const val TAG = "FirebaseInitializer"

    // Reliable fallback credentials from google-services.json (cinestream-sulo)
    private const val FALLBACK_APP_ID = "1:979447256418:android:ea5b570266ae1aa6a9883b"
    private const val FALLBACK_API_KEY = "AIzaSyC9CvaM9Mw3NiD-KsOiYvHZHj6XZJQJnPs"
    private const val FALLBACK_PROJECT_ID = "cinestream-sulo"
    private const val FALLBACK_STORAGE_BUCKET = "cinestream-sulo.firebasestorage.app"
    private const val FALLBACK_GCM_SENDER_ID = "979447256418"

    @Synchronized
    fun init(context: Context): Boolean {
        try {
            // Check if already active
            val existingApps = FirebaseApp.getApps(context)
            if (existingApps.isNotEmpty()) {
                val app = existingApps.firstOrNull()
                AppLogger.i(TAG, "FirebaseApp is already active: ${app?.name}")
                AppState.firebaseInitialized = true
                AppState.firebaseInitError = null
                return true
            }

            AppLogger.i(TAG, "Initializing FirebaseApp for package: ${context.packageName}")

            // Step 1: Attempt standard auto-initialization via context resources
            var app: FirebaseApp? = null
            try {
                app = FirebaseApp.initializeApp(context)
            } catch (e: Exception) {
                AppLogger.w(TAG, "Standard FirebaseApp.initializeApp threw: ${e.message}")
            }

            if (app != null && FirebaseApp.getApps(context).isNotEmpty()) {
                AppLogger.i(TAG, "FirebaseApp initialized successfully via standard resources")
                AppState.firebaseInitialized = true
                AppState.firebaseInitError = null
                return true
            }

            AppLogger.w(TAG, "Standard initialization returned null or empty apps. Falling back to explicit FirebaseOptions...")

            // Step 2: Read resources with cross-package fallback (checking current package and namespace com.example)
            val appId = resolveResource(context, "google_app_id") ?: FALLBACK_APP_ID
            val apiKey = resolveResource(context, "google_api_key") ?: FALLBACK_API_KEY
            val projectId = resolveResource(context, "project_id") ?: FALLBACK_PROJECT_ID
            val storageBucket = resolveResource(context, "google_storage_bucket") ?: FALLBACK_STORAGE_BUCKET
            val gcmSenderId = resolveResource(context, "gcm_defaultSenderId") ?: FALLBACK_GCM_SENDER_ID

            AppLogger.i(TAG, "Configuring FirebaseOptions with AppID: $appId, ProjectID: $projectId")

            val options = FirebaseOptions.Builder()
                .setApplicationId(appId)
                .setApiKey(apiKey)
                .setProjectId(projectId)
                .setStorageBucket(storageBucket)
                .setGcmSenderId(gcmSenderId)
                .build()

            app = FirebaseApp.initializeApp(context, options)

            if (app != null && FirebaseApp.getApps(context).isNotEmpty()) {
                AppLogger.i(TAG, "FirebaseApp successfully initialized with explicit FirebaseOptions!")
                AppState.firebaseInitialized = true
                AppState.firebaseInitError = null
                return true
            } else {
                val errorMsg = "FirebaseApp.initializeApp with options returned null or no registered apps"
                AppLogger.e(TAG, errorMsg)
                AppState.firebaseInitialized = false
                AppState.firebaseInitError = errorMsg
                return false
            }
        } catch (e: Exception) {
            val errorMsg = "Fatal error during Firebase initialization: ${e.message}"
            AppLogger.e(TAG, errorMsg, e)
            AppState.firebaseInitialized = false
            AppState.firebaseInitError = errorMsg
            return false
        }
    }

    private fun resolveResource(context: Context, resName: String): String? {
        return try {
            // Check in application package name
            var resId = context.resources.getIdentifier(resName, "string", context.packageName)
            if (resId == 0) {
                // Check in library/namespace package
                resId = context.resources.getIdentifier(resName, "string", "com.example")
            }
            if (resId != 0) {
                context.getString(resId)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
