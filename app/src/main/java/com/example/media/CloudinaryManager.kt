package com.example.media

import com.example.diagnostics.AppLogger
import com.example.models.CloudinaryMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * CineStream Official Media Storage & Delivery Manager
 * 
 * ARCHITECTURE PRINCIPLES:
 * 1. Cloudinary is the authoritative Media Storage and Delivery platform for CineStream.
 * 2. Firebase Storage is NOT USED for CineStream application media.
 * 3. Firestore stores only metadata, Public IDs, and secure delivery URLs (never binary files).
 * 4. STRICT SECURITY: Cloudinary API Secret is NEVER stored in client applications (APK, BuildConfig,
 *    resources, SharedPreferences, or code). Uploads use Unsigned Upload Presets or backend signatures.
 */
object CloudinaryManager {

    private const val TAG = "CloudinaryManager"

    // Default configuration (Safe client parameters - NO SECRETS)
    const val DEFAULT_CLOUD_NAME = "cinestream"
    const val DEFAULT_UPLOAD_PRESET = "cinestream_unsigned"
    const val CLOUDINARY_HOST = "res.cloudinary.com"

    @Volatile
    private var configuredCloudName: String? = null

    @Volatile
    private var configuredUploadPreset: String? = null

    fun updateConfig(cloudName: String, uploadPreset: String) {
        configuredCloudName = cloudName.ifBlank { null }
        configuredUploadPreset = uploadPreset.ifBlank { null }
    }

    fun getCloudName(): String = configuredCloudName ?: DEFAULT_CLOUD_NAME
    fun getUploadPreset(): String = configuredUploadPreset ?: DEFAULT_UPLOAD_PRESET

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Checks if a given URL is hosted on Cloudinary CDN.
     */
    fun isCloudinaryUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return url.contains(CLOUDINARY_HOST) || url.startsWith("https://res.cloudinary.com/")
    }

    /**
     * Checks if a URL points to Firebase Storage.
     * CineStream contract mandates that media MUST NOT use Firebase Storage.
     */
    fun isFirebaseStorageUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return url.contains("firebasestorage.googleapis.com") || url.contains(".appspot.com")
    }

    /**
     * Validates whether a media URL adheres to CineStream storage policy.
     * Returns true if valid Cloudinary or acceptable CDN link, false if Firebase Storage.
     */
    fun validateMediaUrl(url: String): Boolean {
        if (url.isBlank()) return true
        if (isFirebaseStorageUrl(url)) {
            AppLogger.w(TAG, "Legacy/Forbidden Firebase Storage URL detected: $url. Migration to Cloudinary required.")
            return false
        }
        return true
    }

    /**
     * Generates a transformed, responsive Cloudinary delivery URL.
     * Example: https://res.cloudinary.com/cinestream/image/upload/c_fill,w_300,h_450,f_auto,q_auto/v1/poster_123.jpg
     */
    fun getOptimizedImageUrl(
        originalUrl: String,
        width: Int? = null,
        height: Int? = null,
        crop: String = "fill",
        quality: String = "auto",
        format: String = "auto"
    ): String {
        if (!isCloudinaryUrl(originalUrl)) return originalUrl

        val uploadMarker = "/upload/"
        val index = originalUrl.indexOf(uploadMarker)
        if (index == -1) return originalUrl

        val transformations = buildList {
            add("f_$format")
            add("q_$quality")
            if (width != null) add("w_$width")
            if (height != null) add("h_$height")
            if (width != null || height != null) add("c_$crop")
        }.joinToString(",")

        val prefix = originalUrl.substring(0, index + uploadMarker.length)
        val suffix = originalUrl.substring(index + uploadMarker.length)
        
        // If suffix already has transformation parameters, avoid duplicate prepending
        if (suffix.startsWith("f_") || suffix.startsWith("w_") || suffix.startsWith("c_")) {
            return originalUrl
        }

        return "$prefix$transformations/$suffix"
    }

    /**
     * Returns a standard thumbnail URL (e.g. for user avatars or media cards).
     */
    fun getThumbnailUrl(originalUrl: String, size: Int = 160): String {
        return getOptimizedImageUrl(originalUrl, width = size, height = size, crop = "thumb")
    }

    /**
     * Safe client-side media upload using Unsigned Upload Preset.
     * NO API Secret is required or used.
     *
     * @param file Local file to upload
     * @param resourceType Cloudinary resource type: "image", "video", or "raw"
     * @param folder Destination folder within Cloudinary (e.g. "cinestream/posters", "cinestream/avatars")
     * @param cloudName Target Cloudinary Cloud Name
     * @param uploadPreset Unsigned Upload Preset name
     */
    suspend fun uploadMediaFile(
        file: File,
        resourceType: String = "image",
        folder: String = "cinestream/media",
        cloudName: String = getCloudName(),
        uploadPreset: String = getUploadPreset()
    ): Result<CloudinaryMedia> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                return@withContext Result.failure(IllegalArgumentException("File does not exist: ${file.absolutePath}"))
            }

            val endpoint = "https://api.cloudinary.com/v1_1/$cloudName/$resourceType/upload"
            val fileBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, fileBody)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart("folder", folder)
                .build()

            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            AppLogger.d(TAG, "Uploading media to Cloudinary: ${file.name} ($resourceType) to $folder")

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errorMsg = "Cloudinary upload failed (HTTP ${response.code}): $responseBody"
                    AppLogger.e(TAG, errorMsg)
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val json = JSONObject(responseBody)
                val media = CloudinaryMedia(
                    url = json.optString("secure_url", json.optString("url", "")),
                    publicId = json.optString("public_id", ""),
                    resourceType = json.optString("resource_type", resourceType),
                    format = json.optString("format", ""),
                    width = if (json.has("width")) json.optInt("width") else null,
                    height = if (json.has("height")) json.optInt("height") else null,
                    duration = if (json.has("duration")) json.optDouble("duration") else null,
                    bytes = if (json.has("bytes")) json.optLong("bytes") else file.length(),
                    createdAt = System.currentTimeMillis()
                )

                AppLogger.i(TAG, "Successfully uploaded to Cloudinary: ${media.publicId} -> ${media.url}")
                Result.success(media)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Cloudinary upload exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Upload in-memory ByteArray to Cloudinary using Unsigned Upload Preset.
     */
    suspend fun uploadMediaBytes(
        bytes: ByteArray,
        fileName: String,
        mimeType: String = "image/jpeg",
        resourceType: String = "image",
        folder: String = "cinestream/media",
        cloudName: String = getCloudName(),
        uploadPreset: String = getUploadPreset()
    ): Result<CloudinaryMedia> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "https://api.cloudinary.com/v1_1/$cloudName/$resourceType/upload"
            val fileBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileBody)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart("folder", folder)
                .build()

            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errorMsg = "Cloudinary upload failed (HTTP ${response.code}): $responseBody"
                    AppLogger.e(TAG, errorMsg)
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val json = JSONObject(responseBody)
                val media = CloudinaryMedia(
                    url = json.optString("secure_url", json.optString("url", "")),
                    publicId = json.optString("public_id", ""),
                    resourceType = json.optString("resource_type", resourceType),
                    format = json.optString("format", ""),
                    width = if (json.has("width")) json.optInt("width") else null,
                    height = if (json.has("height")) json.optInt("height") else null,
                    duration = if (json.has("duration")) json.optDouble("duration") else null,
                    bytes = if (json.has("bytes")) json.optLong("bytes") else bytes.size.toLong(),
                    createdAt = System.currentTimeMillis()
                )

                Result.success(media)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Cloudinary byte upload exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
