package com.example.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.diagnostics.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale

data class ApkFileInfo(
    val fileName: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val sha256Hex: String,
    val extractedVersionName: String? = null,
    val extractedVersionCode: Int? = null,
    val localCachedFile: File? = null
)

sealed class ApkUploadState {
    object Idle : ApkUploadState()
    data class Processing(val progressMessage: String) : ApkUploadState()
    data class Uploading(val progressMessage: String) : ApkUploadState()
    data class Success(val info: ApkFileInfo, val downloadUrl: String) : ApkUploadState()
    data class HashCalculated(val info: ApkFileInfo, val noticeMessage: String) : ApkUploadState()
    data class Error(val errorMessage: String) : ApkUploadState()
}

object ApkUploadHelper {
    private const val TAG = "ApkUploadHelper"

    /**
     * Reads the APK file from URI, copies it safely to a temporary staging file,
     * computes the SHA-256 checksum on-the-fly, and extracts file metadata.
     */
    suspend fun analyzeApkUri(context: Context, uri: Uri): Result<ApkFileInfo> = withContext(Dispatchers.IO) {
        try {
            var fileName = "cinestream_update.apk"
            var fileSize = 0L

            // 1. Resolve Display Name and size from ContentResolver
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val resolved = cursor.getString(nameIndex)
                            if (!resolved.isNullOrBlank()) fileName = resolved
                        }
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Cursor query failed: ${e.message}")
            }

            // 2. Stream to a temporary staging file while computing SHA-256
            val cacheDir = File(context.cacheDir, "apk_staging").apply { mkdirs() }
            val tempFile = File(cacheDir, "stage_${System.currentTimeMillis()}_$fileName")
            val digest = MessageDigest.getInstance("SHA-256")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                    }
                    if (fileSize <= 0) fileSize = totalRead
                }
            } ?: return@withContext Result.failure(Exception("Cannot open stream for selected file"))

            val hashBytes = digest.digest()
            val sha256Hex = hashBytes.joinToString("") { "%02x".format(it) }

            // Formatted file size (e.g. 24.3 MB)
            val sizeFormatted = formatFileSize(fileSize)

            // Try to deduce version name and code from fileName
            val (verName, verCode) = extractVersionFromFileName(fileName)

            val info = ApkFileInfo(
                fileName = fileName,
                sizeBytes = fileSize,
                sizeFormatted = sizeFormatted,
                sha256Hex = sha256Hex,
                extractedVersionName = verName,
                extractedVersionCode = verCode,
                localCachedFile = tempFile
            )

            AppLogger.i(TAG, "Analyzed APK: $fileName, Size: $sizeFormatted, SHA-256: ${sha256Hex.take(12)}...")
            Result.success(info)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to analyze APK file: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads the staging APK file to Cloudinary with resourceType = "raw".
     */
    suspend fun uploadToCloudinary(
        apkFile: File,
        cloudName: String = CloudinaryManager.getCloudName(),
        uploadPreset: String = CloudinaryManager.getUploadPreset()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            AppLogger.i(TAG, "Uploading APK (${apkFile.name}) to Cloudinary folder 'cinestream/updates'...")
            val result = CloudinaryManager.uploadMediaFile(
                file = apkFile,
                resourceType = "raw",
                folder = "cinestream/updates",
                cloudName = cloudName,
                uploadPreset = uploadPreset
            )
            result.map { media ->
                AppLogger.i(TAG, "APK upload succeeded: ${media.url}")
                media.url
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "APK upload error: ${e.message}", e)
            Result.failure(e)
        } finally {
            // Cleanup staging file
            try {
                if (apkFile.exists()) {
                    apkFile.delete()
                }
            } catch (ignored: Exception) {}
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    private fun extractVersionFromFileName(name: String): Pair<String?, Int?> {
        val lower = name.lowercase()
        // Check for e.g. v2.1.0 or 2.1.0
        val versionRegex = Regex("""(?:v|^|-|_)?(\d+\.\d+(?:\.\d+)?)""")
        val versionMatch = versionRegex.find(lower)?.groupValues?.get(1)

        // Check for build code / build number e.g. b45, build45, code25
        val codeRegex = Regex("""(?:b|build|code)(\d{1,5})""")
        val codeMatch = codeRegex.find(lower)?.groupValues?.get(1)?.toIntOrNull()

        return Pair(versionMatch, codeMatch)
    }
}
