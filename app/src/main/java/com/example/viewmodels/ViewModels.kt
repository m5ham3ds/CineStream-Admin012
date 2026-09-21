package com.example.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diagnostics.AppLogger
import com.example.models.*
import com.example.repository.AdminRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class UserFilter { ALL, ACTIVE, INACTIVE, PREMIUM, BANNED }
enum class UserSort { NEWEST, RECENT_LOGIN, USERNAME }

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(private val repository: AdminRepository = AdminRepository()) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(UserFilter.ALL)
    val filter = _filter.asStateFlow()

    private val _sort = MutableStateFlow(UserSort.NEWEST)
    val sort = _sort.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val refreshTrigger = MutableStateFlow(0L)

    private val allUsers: StateFlow<List<User>> = refreshTrigger
        .flatMapLatest { repository.getAllUsers() }
        .onEach {
            _isLoading.value = false
            _isRefreshing.value = false
        }
        .catch {
            _isLoading.value = false
            _isRefreshing.value = false
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<User>> = combine(allUsers, _searchQuery, _filter, _sort) { users, query, filter, sort ->
        var list = users

        // 1. Search by username, email, or UID
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.username.lowercase().contains(q) ||
                it.email.lowercase().contains(q) ||
                it.id.lowercase().contains(q)
            }
        }

        // 2. Filter
        val threshold = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        list = when (filter) {
            UserFilter.ALL -> list
            UserFilter.ACTIVE -> list.filter { it.lastLoginTimestamp >= threshold }
            UserFilter.INACTIVE -> list.filter { it.lastLoginTimestamp < threshold }
            UserFilter.PREMIUM -> list.filter { it.isPremium }
            UserFilter.BANNED -> list.filter {
                it.chatBan || it.storyBan || it.downloadBan || it.p2pBan || it.watchBan
            }
        }

        // 3. Sort
        when (sort) {
            UserSort.NEWEST -> list.sortedByDescending { it.createdAt }
            UserSort.RECENT_LOGIN -> list.sortedByDescending { it.lastLoginTimestamp }
            UserSort.USERNAME -> list.sortedBy { it.username.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalUsers = allUsers.map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    // Active if logged in within the last 30 days
    val activeUsers = allUsers.map { userList -> 
        val threshold = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        userList.count { it.lastLoginTimestamp >= threshold }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val inactiveUsers = combine(totalUsers, activeUsers) { total, active -> 
        (total - active).coerceAtLeast(0) 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val proUsersCount = allUsers.map { userList -> userList.count { it.isPremium } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(newFilter: UserFilter) {
        _filter.value = newFilter
    }

    fun setSort(newSort: UserSort) {
        _sort.value = newSort
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _filter.value = UserFilter.ALL
        _sort.value = UserSort.NEWEST
    }

    fun refresh() {
        _isRefreshing.value = true
        refreshTrigger.value = System.currentTimeMillis()
    }

    fun createUser(
        username: String,
        email: String,
        role: String = "user",
        isPremium: Boolean = false,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val newUser = User(
                    username = username,
                    email = email,
                    role = role,
                    isPremium = isPremium,
                    canWatch = true,
                    canDownload = true,
                    canChat = true,
                    canStory = true,
                    canP2P = true,
                    createdAt = System.currentTimeMillis(),
                    lastLoginTimestamp = 0L
                )
                repository.createUser(newUser)
                refresh()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to create user")
            }
        }
    }

    fun deleteUser(userId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteUser(userId)
                refresh()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to delete user")
            }
        }
    }

    fun toggleUserPremium(user: User) {
        viewModelScope.launch {
            try {
                val newIsPremium = !user.isPremium
                val newTier = if (newIsPremium) "pro" else "free"
                repository.updateUser(
                    user.id,
                    mapOf(
                        "isPremium" to newIsPremium,
                        "subscriptionTier" to newTier
                    )
                )
                refresh()
            } catch (e: Exception) {
                AppLogger.e("DashboardViewModel", "Error toggling premium: ${e.message}")
            }
        }
    }

    fun toggleUserBan(user: User) {
        viewModelScope.launch {
            try {
                val isCurrentlyBanned = user.chatBan || user.storyBan || user.downloadBan || user.p2pBan || user.watchBan
                val newBanState = !isCurrentlyBanned
                repository.updateUser(
                    user.id,
                    mapOf(
                        "chatBan" to newBanState,
                        "storyBan" to newBanState,
                        "downloadBan" to newBanState,
                        "p2pBan" to newBanState,
                        "watchBan" to newBanState
                    )
                )
                refresh()
            } catch (e: Exception) {
                AppLogger.e("DashboardViewModel", "Error toggling ban: ${e.message}")
            }
        }
    }
}

class UserDetailViewModel(
    private val userId: String,
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {
    val user: StateFlow<User?> = repository.getUser(userId)
        .catch { e ->
            AppLogger.e("UserDetailViewModel", "Error in user flow: ${e.message}", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun togglePremium() {
        val currentUser = user.value ?: return
        val newIsPremium = !currentUser.isPremium
        val newTier = if (newIsPremium) "pro" else "free"
        viewModelScope.launch {
            try {
                repository.updateUser(
                    userId,
                    mapOf(
                        "isPremium" to newIsPremium,
                        "subscriptionTier" to newTier
                    )
                )
                _statusMessage.value = "Premium status updated to ${newTier.uppercase()}"
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun toggleRole() {
        val currentUser = user.value ?: return
        val willBeAdmin = currentUser.role != "admin"
        viewModelScope.launch {
            try {
                repository.setUserAdminRole(userId, currentUser.email, willBeAdmin)
                _statusMessage.value = if (willBeAdmin) "Granted administrator access" else "Revoked administrator access"
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun togglePermission(permissionKey: String, isCurrentlyAllowed: Boolean) {
        val newAllowed = !isCurrentlyAllowed
        val updates = when (permissionKey) {
            "chat" -> mapOf("canChat" to newAllowed, "chatBan" to !newAllowed)
            "story" -> mapOf("canStory" to newAllowed, "storyBan" to !newAllowed)
            "download" -> mapOf("canDownload" to newAllowed, "downloadBan" to !newAllowed)
            "p2p" -> mapOf("canP2P" to newAllowed, "p2pBan" to !newAllowed)
            "watch" -> mapOf("canWatch" to newAllowed, "watchBan" to !newAllowed)
            else -> mapOf(permissionKey to newAllowed)
        }
        viewModelScope.launch {
            try {
                repository.updateUser(userId, updates)
                _statusMessage.value = "Permission updated"
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun toggleBan(banType: String, currentValue: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateUser(userId, mapOf(banType to !currentValue))
                _statusMessage.value = "$banType set to ${!currentValue}"
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun updateOfflineOverrides(days: Int?, ads: Int?) {
        viewModelScope.launch {
            try {
                repository.updateUser(userId, mapOf(
                    "offlineDaysOverride" to days,
                    "forcedAdsOverride" to ads
                ))
                _statusMessage.value = "Offline DRM overrides saved"
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }
}

class ConfigViewModel(private val repository: AdminRepository = AdminRepository()) : ViewModel() {
    val config: StateFlow<AppConfig> = repository.getAppConfig()
        .catch { e ->
            AppLogger.e("ConfigViewModel", "Error in config flow: ${e.message}", e)
            emit(AppConfig())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfig())

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    private val _apkUploadState = MutableStateFlow<com.example.media.ApkUploadState>(com.example.media.ApkUploadState.Idle)
    val apkUploadState = _apkUploadState.asStateFlow()

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun resetApkUploadState() {
        _apkUploadState.value = com.example.media.ApkUploadState.Idle
    }

    fun handleSelectedApk(
        context: android.content.Context,
        uri: android.net.Uri,
        onMetadataExtracted: (sha256: String, versionName: String?, versionCode: Int?, url: String?) -> Unit
    ) {
        viewModelScope.launch {
            _apkUploadState.value = com.example.media.ApkUploadState.Processing("Analyzing APK file & computing SHA-256...")
            val result = com.example.media.ApkUploadHelper.analyzeApkUri(context, uri)
            result.onSuccess { info ->
                val cachedFile = info.localCachedFile
                val hasCloudinary = com.example.media.CloudinaryManager.getCloudName().isNotBlank() &&
                                    com.example.media.CloudinaryManager.getUploadPreset().isNotBlank()

                if (hasCloudinary && cachedFile != null && cachedFile.exists()) {
                    _apkUploadState.value = com.example.media.ApkUploadState.Uploading("Uploading ${info.fileName} (${info.sizeFormatted}) to server...")
                    val uploadResult = com.example.media.ApkUploadHelper.uploadToCloudinary(cachedFile)
                    uploadResult.onSuccess { uploadedUrl ->
                        _apkUploadState.value = com.example.media.ApkUploadState.Success(info, uploadedUrl)
                        onMetadataExtracted(info.sha256Hex, info.extractedVersionName, info.extractedVersionCode, uploadedUrl)
                        _statusMessage.value = "APK uploaded successfully! SHA-256 and URL updated."
                    }.onFailure { uploadErr ->
                        _apkUploadState.value = com.example.media.ApkUploadState.HashCalculated(info, "SHA-256 calculated. Upload notice: ${uploadErr.message}")
                        onMetadataExtracted(info.sha256Hex, info.extractedVersionName, info.extractedVersionCode, null)
                        _statusMessage.value = "SHA-256 calculated! You can enter APK URL manually."
                    }
                } else {
                    _apkUploadState.value = com.example.media.ApkUploadState.HashCalculated(info, "SHA-256 computed: ${info.sizeFormatted}")
                    onMetadataExtracted(info.sha256Hex, info.extractedVersionName, info.extractedVersionCode, null)
                    _statusMessage.value = "APK SHA-256 computed successfully (${info.sizeFormatted})"
                }
            }.onFailure { error ->
                _apkUploadState.value = com.example.media.ApkUploadState.Error(error.message ?: "Failed to process APK")
                _statusMessage.value = "Error analyzing APK: ${error.message}"
            }
        }
    }

    fun updateMaintenance(enabled: Boolean, title: String, message: String, minVersion: Int) {
        if (minVersion < 0) {
            _statusMessage.value = "Minimum version code cannot be negative"
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.updateAppConfig(
                    mapOf(
                        "maintenanceEnabled" to enabled,
                        "maintenanceTitle" to title,
                        "maintenanceMessage" to message,
                        "minimumVersionCode" to minVersion
                    )
                )
                _statusMessage.value = "Maintenance settings updated successfully"
            } catch (e: Exception) {
                _statusMessage.value = "Failed to update maintenance: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateOta(
        versionCode: Int,
        versionName: String,
        apkUrl: String,
        apkSha256: String,
        mandatory: Boolean,
        releaseNotes: String,
        minimumVersionCode: Int? = null,
        broadcastNotification: Boolean = false
    ) {
        if (versionCode <= 0) {
            _statusMessage.value = "Version code must be greater than 0"
            return
        }
        if (apkUrl.isNotBlank() && !apkUrl.startsWith("http://") && !apkUrl.startsWith("https://")) {
            _statusMessage.value = "APK URL must start with https:// or http://"
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val configUpdates = mutableMapOf<String, Any?>(
                    "latestVersionCode" to versionCode,
                    "latestVersionName" to versionName,
                    "apkUrl" to apkUrl,
                    "apkSha256" to apkSha256,
                    "mandatoryUpdate" to mandatory,
                    "releaseNotes" to releaseNotes
                )
                if (minimumVersionCode != null && minimumVersionCode > 0) {
                    configUpdates["minimumVersionCode"] = minimumVersionCode
                } else if (mandatory) {
                    configUpdates["minimumVersionCode"] = versionCode
                }
                repository.updateAppConfig(configUpdates)

                if (broadcastNotification) {
                    val notifBody = if (releaseNotes.isNotBlank()) releaseNotes else "يتوفر إصدار جديد من تطبيق CineStream ($versionName). يرجى التحديث لتجربة أفضل وميزات جديدة."
                    repository.sendNotification(
                        NotificationRequest(
                            title = "تحديث جديد متوفر: CineStream v$versionName",
                            body = notifBody,
                            message = notifBody,
                            type = "UPDATE",
                            target = "ALL",
                            targetType = NotificationRequest.TargetType.ALL
                        )
                    )
                }

                _statusMessage.value = if (broadcastNotification) 
                    "CineStream OTA Update published & broadcast sent to users" 
                else 
                    "CineStream OTA Update published successfully"
            } catch (e: Exception) {
                _statusMessage.value = "Failed to publish OTA: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateDrmSettings(days: Int, ads: Int) {
        if (days < 0 || ads < 0) {
            _statusMessage.value = "Offline days and forced ads cannot be negative"
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.updateAppConfig(
                    mapOf(
                        "defaultOfflineDays" to days,
                        "defaultForcedAds" to ads
                    )
                )
                _statusMessage.value = "Default DRM rules saved"
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateProvidersJson(json: String) {
        val trimmed = json.trim()
        if (trimmed.isNotBlank()) {
            try {
                if (trimmed.startsWith("[")) {
                    org.json.JSONArray(trimmed)
                } else {
                    org.json.JSONObject(trimmed)
                }
            } catch (e: Exception) {
                _statusMessage.value = "Invalid JSON syntax: ${e.message}"
                return
            }
        }
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.updateAppConfig(mapOf("providersJson" to trimmed))
                _statusMessage.value = "Dynamic providers JSON deployed"
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateCloudinaryConfig(cloudName: String, uploadPreset: String) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.updateAppConfig(
                    mapOf(
                        "cloudinaryCloudName" to cloudName.trim(),
                        "cloudinaryUploadPreset" to uploadPreset.trim()
                    )
                )
                com.example.media.CloudinaryManager.updateConfig(cloudName.trim(), uploadPreset.trim())
                _statusMessage.value = "Cloudinary media storage config saved"
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }
}

class NotificationViewModel(private val repository: AdminRepository = AdminRepository()) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationState())
    val uiState = _uiState.asStateFlow()

    val recentNotifications: StateFlow<List<NotificationRequest>> = repository.getRecentNotifications()
        .catch { e ->
            AppLogger.e("NotificationViewModel", "Error fetching notifications: ${e.message}", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSending = MutableStateFlow(false)
    val isSending = _isSending.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun updateTitle(title: String) { _uiState.value = _uiState.value.copy(title = title) }
    fun updateMessage(msg: String) { _uiState.value = _uiState.value.copy(message = msg, body = msg) }
    fun updateBody(body: String) { _uiState.value = _uiState.value.copy(body = body, message = body) }
    fun updateType(type: String) { _uiState.value = _uiState.value.copy(type = type) }
    fun updateTargetType(type: NotificationRequest.TargetType) { _uiState.value = _uiState.value.copy(targetType = type) }
    fun updateTargetUid(uid: String) { _uiState.value = _uiState.value.copy(targetUid = uid) }

    fun sendNotification() {
        val state = _uiState.value
        val bodyContent = state.body.ifBlank { state.message }.trim()
        if (state.title.isBlank() || bodyContent.isBlank()) {
            _statusMessage.value = "Please fill in title and message"
            return
        }
        if (state.targetType == NotificationRequest.TargetType.UID && state.targetUid.isBlank()) {
            _statusMessage.value = "Please enter target user UID"
            return
        }

        val targetStr = when (state.targetType) {
            NotificationRequest.TargetType.ALL -> "ALL"
            NotificationRequest.TargetType.PRO -> "PRO"
            NotificationRequest.TargetType.UID -> "UID"
        }

        viewModelScope.launch {
            _isSending.value = true
            try {
                repository.sendNotification(
                    NotificationRequest(
                        title = state.title.trim(),
                        body = bodyContent,
                        message = bodyContent,
                        type = state.type,
                        target = targetStr,
                        targetType = state.targetType,
                        targetUid = if (state.targetType == NotificationRequest.TargetType.UID) state.targetUid.trim() else ""
                    )
                )
                _statusMessage.value = "Notification dispatched to /notifications"
                _uiState.value = NotificationState()
            } catch (e: Exception) {
                _statusMessage.value = "Failed to send: ${e.message}"
            } finally {
                _isSending.value = false
            }
        }
    }
}

data class NotificationState(
    val title: String = "",
    val message: String = "",
    val body: String = "",
    val type: String = "SYSTEM",
    val targetType: NotificationRequest.TargetType = NotificationRequest.TargetType.ALL,
    val targetUid: String = ""
)

enum class LogCategory { ALL, USER, CONFIG, NOTIFICATION, EXTENSION }

class AuditLogsViewModel(private val repository: AdminRepository = AdminRepository()) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _category = MutableStateFlow(LogCategory.ALL)
    val category = _category.asStateFlow()

    private val allLogs = repository.getAuditLogs()
        .catch { e ->
            AppLogger.e("AuditLogsViewModel", "Error in audit logs: ${e.message}", e)
            emit(emptyList())
        }
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

    val logs: StateFlow<List<AuditLog>> = combine(allLogs, _searchQuery, _category) { list, query, cat ->
        var filtered = list

        if (cat != LogCategory.ALL) {
            val targetKeyword = cat.name
            filtered = filtered.filter {
                it.targetType.equals(targetKeyword, ignoreCase = true) ||
                it.action.contains(targetKeyword, ignoreCase = true)
            }
        }

        if (query.isNotBlank()) {
            val q = query.lowercase().trim()
            filtered = filtered.filter {
                it.action.lowercase().contains(q) ||
                it.adminEmail.lowercase().contains(q) ||
                it.targetId.lowercase().contains(q) ||
                it.details.lowercase().contains(q)
            }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(q: String) {
        _searchQuery.value = q
    }

    fun setCategory(cat: LogCategory) {
        _category.value = cat
    }
}

class ExtensionsViewModel(private val repository: AdminRepository = AdminRepository()) : ViewModel() {
    val extensions: StateFlow<List<ExtensionItem>> = repository.getExtensions()
        .catch { e ->
            AppLogger.e("ExtensionsViewModel", "Error fetching extensions: ${e.message}", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun saveExtension(extension: ExtensionItem) {
        viewModelScope.launch {
            try {
                repository.saveExtension(extension)
                _statusMessage.value = "Extension saved"
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun toggleExtension(item: ExtensionItem) {
        viewModelScope.launch {
            try {
                repository.saveExtension(item.copy(enabled = !item.enabled))
                _statusMessage.value = "Extension ${if (!item.enabled) "enabled" else "disabled"}"
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun deleteExtension(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteExtension(id)
                _statusMessage.value = "Extension deleted"
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }
}
