package com.example.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val id: String = uid,
    val username: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val lastLoginAt: Long = 0L,
    val lastLoginTimestamp: Long = lastLoginAt,
    val lastActiveAt: Long = updatedAt,
    val isActive: Boolean = true,
    val isPremium: Boolean = false,
    val subscriptionTier: String = "free",
    val subscriptionExpiresAt: Long? = null,
    val role: String = "user",
    // Positive feature permissions
    val canWatch: Boolean = true,
    val canDownload: Boolean = true,
    val canChat: Boolean = true,
    val canStory: Boolean = true,
    val canP2P: Boolean = true,
    // Ban flags maintained for compatibility
    val watchBan: Boolean = false,
    val downloadBan: Boolean = false,
    val chatBan: Boolean = false,
    val storyBan: Boolean = false,
    val p2pBan: Boolean = false,
    val deviceLimit: Int = 2,
    val offlineDaysOverride: Int? = null,
    val forcedAdsOverride: Int? = null,
    val appVersion: String = ""
) {
    val isChatAllowed: Boolean get() = canChat && !chatBan
    val isStoryAllowed: Boolean get() = canStory && !storyBan
    val isDownloadAllowed: Boolean get() = canDownload && !downloadBan
    val isP2PAllowed: Boolean get() = canP2P && !p2pBan
    val isWatchAllowed: Boolean get() = canWatch && !watchBan
}

@IgnoreExtraProperties
data class AdminUser(
    val uid: String = "",
    val email: String = "",
    val role: String = "admin",
    val enabled: Boolean = true,
    val createdAt: Long = 0L
)

@IgnoreExtraProperties
data class AppConfig(
    // Maintenance Mode
    val maintenanceEnabled: Boolean = false,
    val maintenanceTitle: String = "Under Scheduled Maintenance",
    val maintenanceMessage: String = "CineStream is temporarily offline for upgrades. Please check back shortly.",
    val minimumVersionCode: Int = 1,
    // App OTA Updates
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0.0",
    val apkUrl: String = "",
    val apkSha256: String = "",
    val mandatoryUpdate: Boolean = false,
    val releaseNotes: String = "",
    // Media Providers and Scrapers Dynamic Catalog
    val providersJson: String = "{\n  \"providers\": [],\n  \"scrapers\": [],\n  \"endpoints\": []\n}",
    // DRM & Playback Defaults
    val defaultOfflineDays: Int = 2,
    val defaultForcedAds: Int = 5,
    // Cloudinary Media Configuration (Safe client settings - NO API Secret)
    val cloudinaryCloudName: String = "cinestream",
    val cloudinaryUploadPreset: String = "cinestream_unsigned",
    val updatedAt: Long = 0L
)

typealias GlobalConfig = AppConfig

@IgnoreExtraProperties
data class ExtensionItem(
    val id: String = "",
    val name: String = "",
    val packageName: String = "",
    val versionCode: Int = 1,
    val versionName: String = "1.0.0",
    val apkUrl: String = "",
    val apkSha256: String = "",
    val sha256: String = apkSha256,
    val minAppVersionCode: Int = 1,
    val enabled: Boolean = true,
    val mandatory: Boolean = false,
    val releaseNotes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@IgnoreExtraProperties
data class NotificationRequest(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val message: String = body,
    val type: String = "SYSTEM",
    val target: String = "ALL",
    val targetType: TargetType = TargetType.ALL,
    val targetUid: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val isActive: Boolean = true,
    val status: String = "PENDING"
) {
    enum class TargetType {
        ALL, UID, PRO
    }
}

@IgnoreExtraProperties
data class AuditLog(
    val id: String = "",
    val actorUid: String = "",
    val adminUid: String = actorUid,
    val adminEmail: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val details: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class UserReport(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val type: String = "ISSUE",
    val targetType: String = "",
    val targetId: String = "",
    val title: String = "",
    val description: String = "",
    val status: String = "PENDING",
    val resolutionNotes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val resolvedBy: String = ""
)

@IgnoreExtraProperties
data class CloudinaryMedia(
    val url: String = "",
    val publicId: String = "",
    val resourceType: String = "image",
    val format: String = "",
    val width: Int? = null,
    val height: Int? = null,
    val duration: Double? = null,
    val bytes: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

