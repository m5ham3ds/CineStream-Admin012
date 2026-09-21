package com.example.repository

import com.example.diagnostics.AppLogger
import com.example.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AdminRepository {
    private fun ensureFirebase() {
        try {
            val apps = com.google.firebase.FirebaseApp.getApps(com.example.MyApplication.instance)
            if (apps.isEmpty()) {
                com.example.diagnostics.FirebaseInitializer.init(com.example.MyApplication.instance)
            }
        } catch (e: Exception) {
            AppLogger.w("AdminRepository", "ensureFirebase check exception: ${e.message}")
        }
    }

    private val auth: FirebaseAuth by lazy {
        ensureFirebase()
        FirebaseAuth.getInstance()
    }

    private val firestore: FirebaseFirestore by lazy {
        ensureFirebase()
        FirebaseFirestore.getInstance()
    }

    private val usersCollection get() = firestore.collection("users")
    private val adminsCollection get() = firestore.collection("admins")
    private val configCollection get() = firestore.collection("config")
    private val notificationsCollection get() = firestore.collection("notifications")
    private val auditLogsCollection get() = firestore.collection("auditLogs")
    private val extensionsCollection get() = firestore.collection("extensions")
    private val reportsCollection get() = firestore.collection("reports")

    private fun extractTimestampMillis(doc: com.google.firebase.firestore.DocumentSnapshot, field: String, fallback: Long = 0L): Long {
        val value = doc.get(field) ?: return fallback
        return when (value) {
            is Number -> value.toLong()
            is com.google.firebase.Timestamp -> value.toDate().time
            is java.util.Date -> value.time
            else -> fallback
        }
    }

    private fun docToUser(doc: com.google.firebase.firestore.DocumentSnapshot): User? {
        return try {
            val user = doc.toObject(User::class.java)
            val resolvedUid = if (!user?.uid.isNullOrBlank()) user!!.uid else doc.id
            val lastLogin = extractTimestampMillis(doc, "lastLoginAt",
                extractTimestampMillis(doc, "lastLoginTimestamp", user?.lastLoginAt ?: 0L))
            val updated = extractTimestampMillis(doc, "updatedAt",
                extractTimestampMillis(doc, "lastActiveAt", user?.updatedAt ?: 0L))
            val created = extractTimestampMillis(doc, "createdAt", user?.createdAt ?: 0L)
            val subExpires = if (doc.contains("subscriptionExpiresAt")) {
                extractTimestampMillis(doc, "subscriptionExpiresAt", 0L).takeIf { it > 0L }
            } else user?.subscriptionExpiresAt

            (user ?: User()).copy(
                uid = resolvedUid,
                id = resolvedUid,
                createdAt = created,
                lastLoginAt = lastLogin,
                lastLoginTimestamp = lastLogin,
                updatedAt = updated,
                lastActiveAt = updated,
                subscriptionExpiresAt = subExpires
            )
        } catch (e: Exception) {
            try {
                val uid = doc.getString("uid") ?: doc.id
                User(
                    uid = uid,
                    id = uid,
                    username = doc.getString("username") ?: "",
                    email = doc.getString("email") ?: "",
                    displayName = doc.getString("displayName") ?: "",
                    photoUrl = doc.getString("photoUrl") ?: "",
                    createdAt = extractTimestampMillis(doc, "createdAt", 0L),
                    updatedAt = extractTimestampMillis(doc, "updatedAt", 0L),
                    lastLoginAt = extractTimestampMillis(doc, "lastLoginAt", 0L),
                    lastLoginTimestamp = extractTimestampMillis(doc, "lastLoginAt", 0L),
                    isActive = doc.getBoolean("isActive") ?: true,
                    isPremium = doc.getBoolean("isPremium") ?: false,
                    subscriptionTier = doc.getString("subscriptionTier") ?: "free",
                    subscriptionExpiresAt = if (doc.contains("subscriptionExpiresAt")) extractTimestampMillis(doc, "subscriptionExpiresAt", 0L).takeIf { it > 0L } else null,
                    role = doc.getString("role") ?: "user",
                    canWatch = doc.getBoolean("canWatch") ?: true,
                    canDownload = doc.getBoolean("canDownload") ?: true,
                    canChat = doc.getBoolean("canChat") ?: true,
                    canStory = doc.getBoolean("canStory") ?: true,
                    canP2P = doc.getBoolean("canP2P") ?: true,
                    watchBan = doc.getBoolean("watchBan") ?: false,
                    downloadBan = doc.getBoolean("downloadBan") ?: false,
                    chatBan = doc.getBoolean("chatBan") ?: false,
                    storyBan = doc.getBoolean("storyBan") ?: false,
                    p2pBan = doc.getBoolean("p2pBan") ?: false,
                    deviceLimit = (doc.getLong("deviceLimit") ?: 2L).toInt(),
                    offlineDaysOverride = doc.getLong("offlineDaysOverride")?.toInt(),
                    forcedAdsOverride = doc.getLong("forcedAdsOverride")?.toInt(),
                    appVersion = doc.getString("appVersion") ?: ""
                )
            } catch (inner: Exception) {
                AppLogger.w("AdminRepository", "Failed to deserialize user document ${doc.id}: ${inner.message}")
                null
            }
        }
    }

    /**
     * Verifies if the authenticated user is an authorized administrator.
     * Checks both 'admins' and 'users' collections in Firestore,
     * and provisions bootstrap credentials for the project owner.
     */
    suspend fun checkIsAdmin(user: FirebaseUser): Boolean {
        // Instant check for project owner: always granted superadmin
        val isOwner = user.email?.equals("sulopros01@gmail.com", ignoreCase = true) == true
        if (isOwner) {
            AppLogger.i("AdminRepository", "Project owner authenticated: ${user.email}")
            try {
                adminsCollection.document(user.uid).set(
                    AdminUser(
                        uid = user.uid,
                        email = user.email ?: "",
                        role = "superadmin",
                        enabled = true,
                        createdAt = System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
            } catch (e: Exception) {
                AppLogger.w("AdminRepository", "Owner bootstrap doc write non-fatal notice: ${e.message}")
            }
            return true
        }

        try {
            // 1. Check direct document in admins collection
            val adminDoc = adminsCollection.document(user.uid).get().await()
            if (adminDoc.exists()) {
                val enabled = adminDoc.getBoolean("enabled") ?: true
                if (enabled) return true
            }
        } catch (e: Exception) {
            AppLogger.w("AdminRepository", "Admin collection check notice: ${e.message}")
        }

        try {
            // 2. Check role in users collection
            val userDoc = usersCollection.document(user.uid).get().await()
            if (userDoc.exists()) {
                val role = userDoc.getString("role")
                if (role.equals("admin", ignoreCase = true) || role.equals("superadmin", ignoreCase = true)) {
                    return true
                }
            }
        } catch (e: Exception) {
            AppLogger.w("AdminRepository", "Users collection check notice: ${e.message}")
        }

        return false
    }

    /**
     * Records administrative actions in Firestore for full transparency and accountability.
     */
    suspend fun logAudit(action: String, targetType: String, targetId: String, details: String) {
        try {
            val currentAdmin = auth.currentUser
            val docRef = auditLogsCollection.document()
            val log = AuditLog(
                id = docRef.id,
                actorUid = currentAdmin?.uid ?: "system",
                adminUid = currentAdmin?.uid ?: "system",
                adminEmail = currentAdmin?.email ?: "system",
                action = action,
                targetType = targetType,
                targetId = targetId,
                details = details,
                createdAt = System.currentTimeMillis()
            )
            docRef.set(log).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- USERS MANAGEMENT ---

    fun getAllUsers(): Flow<List<User>> = callbackFlow {
        AppLogger.d("AdminRepository", "Listening for Firestore users updates")
        val listener = usersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                AppLogger.e("AdminRepository", "Firestore users listener error: ${error.message}", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val users = snapshot?.documents?.mapNotNull { doc ->
                docToUser(doc)
            } ?: emptyList()
            AppLogger.d("AdminRepository", "Received ${users.size} users from Firestore")
            trySend(users.sortedByDescending { it.lastLoginTimestamp })
        }
        awaitClose { listener.remove() }
    }

    fun getUser(userId: String): Flow<User?> = callbackFlow {
        AppLogger.d("AdminRepository", "Listening for Firestore user $userId")
        val listener = usersCollection.document(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                AppLogger.e("AdminRepository", "Firestore user($userId) error: ${error.message}", error)
                trySend(null)
                return@addSnapshotListener
            }
            val user = if (snapshot != null && snapshot.exists()) docToUser(snapshot) else null
            trySend(user)
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateUser(userId: String, updates: Map<String, Any?>) {
        val updatesWithTimestamp = updates.toMutableMap()
        updatesWithTimestamp["updatedAt"] = System.currentTimeMillis()
        usersCollection.document(userId).update(updatesWithTimestamp).await()
        val formattedDetails = updates.entries.joinToString(", ") { "${it.key}=${it.value}" }
        logAudit(
            action = "UPDATE_USER",
            targetType = "USER",
            targetId = userId,
            details = "Updated fields: $formattedDetails"
        )
    }

    suspend fun setUserAdminRole(userId: String, email: String, isAdmin: Boolean) {
        val role = if (isAdmin) "admin" else "user"
        val updates = mapOf(
            "role" to role,
            "updatedAt" to System.currentTimeMillis()
        )
        usersCollection.document(userId).update(updates).await()
        if (isAdmin) {
            val adminDoc = AdminUser(
                uid = userId,
                email = email,
                role = "admin",
                enabled = true,
                createdAt = System.currentTimeMillis()
            )
            adminsCollection.document(userId).set(adminDoc, SetOptions.merge()).await()
        } else {
            adminsCollection.document(userId).set(mapOf("enabled" to false), SetOptions.merge()).await()
        }
        logAudit(
            action = if (isAdmin) "GRANT_ADMIN" else "REVOKE_ADMIN",
            targetType = "USER",
            targetId = userId,
            details = "Set admin role to $role for $email"
        )
    }

    suspend fun createUser(user: User): String {
        val resolvedId = if (user.uid.isNotBlank()) user.uid else if (user.id.isNotBlank()) user.id else usersCollection.document().id
        val docRef = usersCollection.document(resolvedId)
        val now = System.currentTimeMillis()
        val finalUser = user.copy(
            uid = docRef.id,
            id = docRef.id,
            createdAt = if (user.createdAt == 0L) now else user.createdAt,
            updatedAt = now,
            lastLoginAt = if (user.lastLoginAt != 0L) user.lastLoginAt else user.lastLoginTimestamp,
            lastLoginTimestamp = if (user.lastLoginAt != 0L) user.lastLoginAt else user.lastLoginTimestamp
        )
        // Set with merge to avoid wiping user-managed subcollections or fields if existing
        docRef.set(finalUser, SetOptions.merge()).await()
        logAudit(
            action = "CREATE_USER",
            targetType = "USER",
            targetId = docRef.id,
            details = "Created user: ${finalUser.username} (${finalUser.email})"
        )
        return docRef.id
    }

    suspend fun deleteUser(userId: String) {
        usersCollection.document(userId).delete().await()
        logAudit(
            action = "DELETE_USER",
            targetType = "USER",
            targetId = userId,
            details = "Permanently deleted user: $userId"
        )
    }

    // --- APP & GLOBAL CONFIG (MAINTENANCE, OTA, DRM) ---

    fun getAppConfig(): Flow<AppConfig> = callbackFlow {
        AppLogger.d("AdminRepository", "Listening for Firestore app config")
        val listener = configCollection.document("app").addSnapshotListener { snapshot, error ->
            if (error != null) {
                AppLogger.e("AdminRepository", "Firestore config error: ${error.message}", error)
                trySend(AppConfig())
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val config = try {
                    snapshot.toObject(AppConfig::class.java) ?: AppConfig()
                } catch (e: Exception) {
                    AppLogger.w("AdminRepository", "Error parsing AppConfig: ${e.message}")
                    AppConfig()
                }
                trySend(config)
            } else {
                // Try reading legacy 'global' document
                configCollection.document("global").get().addOnSuccessListener { globalSnap ->
                    val globalConfig = try {
                        globalSnap.toObject(AppConfig::class.java) ?: AppConfig()
                    } catch (e: Exception) {
                        AppConfig()
                    }
                    trySend(globalConfig)
                }.addOnFailureListener {
                    trySend(AppConfig())
                }
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateAppConfig(updates: Map<String, Any?>) {
        val updatesWithTimestamp = updates.toMutableMap()
        updatesWithTimestamp["updatedAt"] = System.currentTimeMillis()

        // Canonical unified path: /config/app
        configCollection.document("app").set(updatesWithTimestamp, SetOptions.merge()).await()
        // Dual write to legacy /config/global for backward compatibility
        configCollection.document("global").set(updatesWithTimestamp, SetOptions.merge()).await()

        logAudit(
            action = "UPDATE_APP_CONFIG",
            targetType = "CONFIG",
            targetId = "app",
            details = "Updated configs: ${updates.keys.joinToString(", ")}"
        )
    }

    // Backward-compatibility alias
    fun getGlobalConfig(): Flow<GlobalConfig> = getAppConfig()
    suspend fun updateGlobalConfig(updates: Map<String, Any?>) = updateAppConfig(updates)

    // --- NOTIFICATIONS ---

    suspend fun sendNotification(request: NotificationRequest) {
        val currentAdmin = auth.currentUser
        val docRef = notificationsCollection.document()
        val bodyText = request.body.ifBlank { request.message }
        val targetVal = if (request.target.isNotBlank()) request.target else request.targetType.name
        val payload = hashMapOf<String, Any?>(
            "id" to docRef.id,
            "title" to request.title,
            "body" to bodyText,
            "message" to bodyText,
            "type" to request.type,
            "target" to targetVal,
            "targetType" to targetVal,
            "targetUid" to request.targetUid,
            "createdBy" to (currentAdmin?.uid ?: "admin"),
            "createdAt" to System.currentTimeMillis(),
            "expiresAt" to request.expiresAt,
            "isActive" to request.isActive,
            "status" to "PENDING"
        )
        docRef.set(payload).await()
        logAudit(
            action = "DISPATCH_NOTIFICATION",
            targetType = "NOTIFICATION",
            targetId = targetVal,
            details = "Title: ${request.title}, Audience: $targetVal, Type: ${request.type}"
        )
    }

    fun getRecentNotifications(): Flow<List<NotificationRequest>> = callbackFlow {
        val listener = notificationsCollection.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(25)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLogger.e("AdminRepository", "Firestore notifications error: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val notif = doc.toObject(NotificationRequest::class.java)?.copy(id = doc.id)
                        if (notif != null) {
                            val bodyText = if (notif.body.isNotBlank()) notif.body else notif.message
                            val targetVal = if (notif.target.isNotBlank()) notif.target else notif.targetType.name
                            notif.copy(body = bodyText, message = bodyText, target = targetVal)
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // --- AUDIT LOGS ---

    fun getAuditLogs(): Flow<List<AuditLog>> = callbackFlow {
        val listener = auditLogsCollection.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLogger.e("AdminRepository", "Firestore audit logs error: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(AuditLog::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // --- EXTENSIONS MANAGEMENT ---

    fun getExtensions(): Flow<List<ExtensionItem>> = callbackFlow {
        val listener = extensionsCollection.orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLogger.e("AdminRepository", "Firestore extensions error: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val ext = doc.toObject(ExtensionItem::class.java)?.copy(id = doc.id)
                        if (ext != null) {
                            val resolvedSha = ext.apkSha256.ifBlank { ext.sha256 }
                            ext.copy(apkSha256 = resolvedSha, sha256 = resolvedSha)
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveExtension(extension: ExtensionItem) {
        val docRef = if (extension.id.isNotBlank()) extensionsCollection.document(extension.id) else extensionsCollection.document()
        val now = System.currentTimeMillis()
        val resolvedSha = extension.apkSha256.ifBlank { extension.sha256 }
        val payload = extension.copy(
            id = docRef.id,
            apkSha256 = resolvedSha,
            sha256 = resolvedSha,
            createdAt = if (extension.createdAt == 0L) now else extension.createdAt,
            updatedAt = now
        )
        docRef.set(payload, SetOptions.merge()).await()
        logAudit(
            action = "SAVE_EXTENSION",
            targetType = "EXTENSION",
            targetId = docRef.id,
            details = "Saved extension: ${extension.name} v${extension.versionName}"
        )
    }

    suspend fun deleteExtension(extensionId: String) {
        extensionsCollection.document(extensionId).delete().await()
        logAudit(
            action = "DELETE_EXTENSION",
            targetType = "EXTENSION",
            targetId = extensionId,
            details = "Deleted extension id: $extensionId"
        )
    }

    // --- USER REPORTS MANAGEMENT ---

    fun getReports(): Flow<List<UserReport>> = callbackFlow {
        val listener = reportsCollection.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLogger.e("AdminRepository", "Firestore reports error: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val report = doc.toObject(UserReport::class.java)?.copy(id = doc.id)
                        val created = extractTimestampMillis(doc, "createdAt", report?.createdAt ?: 0L)
                        val resolved = if (doc.contains("resolvedAt")) extractTimestampMillis(doc, "resolvedAt", 0L) else null
                        (report ?: UserReport()).copy(
                            id = doc.id,
                            createdAt = created,
                            resolvedAt = resolved
                        )
                    } catch (e: Exception) {
                        try {
                            UserReport(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                userEmail = doc.getString("userEmail") ?: "",
                                type = doc.getString("type") ?: "ISSUE",
                                targetType = doc.getString("targetType") ?: "",
                                targetId = doc.getString("targetId") ?: "",
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                status = doc.getString("status") ?: "PENDING",
                                resolutionNotes = doc.getString("resolutionNotes") ?: "",
                                createdAt = extractTimestampMillis(doc, "createdAt", 0L),
                                resolvedAt = if (doc.contains("resolvedAt")) extractTimestampMillis(doc, "resolvedAt", 0L) else null,
                                resolvedBy = doc.getString("resolvedBy") ?: ""
                            )
                        } catch (inner: Exception) {
                            null
                        }
                    }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateReportStatus(reportId: String, status: String, notes: String) {
        val currentAdmin = auth.currentUser
        val updates = mapOf(
            "status" to status,
            "resolutionNotes" to notes,
            "resolvedAt" to System.currentTimeMillis(),
            "resolvedBy" to (currentAdmin?.uid ?: "admin")
        )
        reportsCollection.document(reportId).update(updates).await()
        logAudit(
            action = "RESOLVE_REPORT",
            targetType = "REPORT",
            targetId = reportId,
            details = "Status changed to $status with notes: $notes"
        )
    }

    suspend fun deleteReport(reportId: String) {
        reportsCollection.document(reportId).delete().await()
        logAudit(
            action = "DELETE_REPORT",
            targetType = "REPORT",
            targetId = reportId,
            details = "Deleted report id: $reportId"
        )
    }
}

