package com.example

import com.example.models.AppConfig
import com.example.models.NotificationRequest
import com.example.models.User
import com.example.viewmodels.UserFilter
import com.example.viewmodels.UserSort
import org.junit.Assert.*
import org.junit.Test

class CineStreamAdminLogicTest {

    @Test
    fun testUserFilterLogic() {
        val users = listOf(
            User(id = "1", username = "alice", email = "alice@example.com", isPremium = true),
            User(id = "2", username = "bob", email = "bob@example.com", isPremium = false, chatBan = true),
            User(id = "3", username = "charlie", email = "charlie@example.com", isPremium = false)
        )

        val proOnly = users.filter { it.isPremium }
        assertEquals(1, proOnly.size)
        assertEquals("alice", proOnly[0].username)

        val bannedOnly = users.filter { it.chatBan || it.storyBan || it.downloadBan || it.p2pBan || it.watchBan }
        assertEquals(1, bannedOnly.size)
        assertEquals("bob", bannedOnly[0].username)
    }

    @Test
    fun testUserSortLogic() {
        val users = listOf(
            User(id = "1", username = "Charlie", lastLoginTimestamp = 100L, createdAt = 500L),
            User(id = "2", username = "Alice", lastLoginTimestamp = 300L, createdAt = 200L),
            User(id = "3", username = "Bob", lastLoginTimestamp = 200L, createdAt = 700L)
        )

        val byRecentLogin = users.sortedByDescending { it.lastLoginTimestamp }
        assertEquals("Alice", byRecentLogin.first().username)

        val byUsername = users.sortedBy { it.username.lowercase() }
        assertEquals("Alice", byUsername[0].username)
        assertEquals("Bob", byUsername[1].username)
        assertEquals("Charlie", byUsername[2].username)

        val byNewest = users.sortedByDescending { it.createdAt }
        assertEquals("Bob", byNewest.first().username)
    }

    @Test
    fun testNotificationValidation() {
        // UID Target requires valid UID
        val uidRequest = NotificationRequest(
            targetType = NotificationRequest.TargetType.UID,
            targetUid = "user_12345",
            title = "Special Offer",
            message = "Your VIP pass is ready"
        )
        assertTrue(uidRequest.targetUid.isNotBlank())
        assertTrue(uidRequest.title.isNotBlank())
        assertTrue(uidRequest.message.isNotBlank())

        // Global broadcast
        val globalRequest = NotificationRequest(
            targetType = NotificationRequest.TargetType.ALL,
            title = "System Update",
            message = "Maintenance tonight at 2 AM UTC"
        )
        assertEquals(NotificationRequest.TargetType.ALL, globalRequest.targetType)
    }

    @Test
    fun testStepperLowerBound() {
        val currentVal = 0
        val decremented = if (currentVal > 0) currentVal - 1 else 0
        assertEquals(0, decremented)

        val positiveVal = 5
        val decrementedPositive = if (positiveVal > 0) positiveVal - 1 else 0
        assertEquals(4, decrementedPositive)
    }

    @Test
    fun testAppConfigOtaUrlValidation() {
        fun isValidApkUrl(url: String): Boolean {
            return url.isBlank() || url.startsWith("https://") || url.startsWith("http://")
        }

        assertTrue(isValidApkUrl(""))
        assertTrue(isValidApkUrl("https://cinestream.app/downloads/latest.apk"))
        assertTrue(isValidApkUrl("http://mirror.cinestream.app/v2.apk"))
        assertFalse(isValidApkUrl("ftp://cinestream.app/bad.apk"))
        assertFalse(isValidApkUrl("invalid-path-file"))
    }

    @Test
    fun testUnifiedContractModels() {
        val user = User(
            uid = "user_abc123",
            username = "streamer",
            email = "streamer@example.com",
            isPremium = true,
            subscriptionTier = "pro",
            canWatch = true,
            watchBan = false,
            canDownload = false,
            downloadBan = true
        )
        assertEquals("user_abc123", user.uid)
        assertEquals("user_abc123", user.id)
        assertTrue(user.isWatchAllowed)
        assertFalse(user.isDownloadAllowed)
        assertEquals("pro", user.subscriptionTier)

        val notif = NotificationRequest(
            title = "Promo",
            body = "Check out new movies!",
            type = "PROMO",
            target = "PRO"
        )
        assertEquals("Check out new movies!", notif.body)
        assertEquals("Check out new movies!", notif.message)
        assertEquals("PROMO", notif.type)
        assertEquals("PRO", notif.target)

        val config = AppConfig(
            maintenanceEnabled = true,
            latestVersionCode = 12,
            latestVersionName = "2.1.0",
            apkSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        )
        assertTrue(config.maintenanceEnabled)
        assertEquals("2.1.0", config.latestVersionName)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", config.apkSha256)
    }

    @Test
    fun testCloudinaryManagerPolicies() {
        val manager = com.example.media.CloudinaryManager
        
        // Cloudinary URL recognition
        val cloudUrl = "https://res.cloudinary.com/cinestream/image/upload/v1/avatar.jpg"
        assertTrue(manager.isCloudinaryUrl(cloudUrl))
        assertTrue(manager.validateMediaUrl(cloudUrl))

        // Firebase storage detection & restriction
        val fbStorageUrl = "https://firebasestorage.googleapis.com/v0/b/cinestream.appspot.com/o/avatar.jpg"
        assertTrue(manager.isFirebaseStorageUrl(fbStorageUrl))
        assertFalse(manager.validateMediaUrl(fbStorageUrl))

        // Transformations
        val transformed = manager.getOptimizedImageUrl(cloudUrl, width = 200, height = 200, crop = "thumb")
        assertTrue(transformed.contains("w_200"))
        assertTrue(transformed.contains("h_200"))
        assertTrue(transformed.contains("c_thumb"))
        assertTrue(transformed.contains("f_auto"))
        assertTrue(transformed.contains("q_auto"))
    }

    @Test
    fun testUserReportContract() {
        val report = com.example.models.UserReport(
            id = "rep_123",
            userId = "usr_456",
            userEmail = "user@cinestream.app",
            type = "ISSUE",
            targetType = "STREAM",
            targetId = "movie_999",
            title = "Broken Audio Track",
            description = "Audio is out of sync at minute 14",
            status = "PENDING"
        )
        assertEquals("rep_123", report.id)
        assertEquals("usr_456", report.userId)
        assertEquals("ISSUE", report.type)
        assertEquals("PENDING", report.status)
        assertEquals("STREAM", report.targetType)
    }
}

