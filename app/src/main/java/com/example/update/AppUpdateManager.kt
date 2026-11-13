package com.example.update

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

const val GITHUB_REPO_OWNER = "charfeddine23"
const val GITHUB_REPO_NAME = "Salery"
const val GITHUB_REPO_URL = "https://github.com/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME"
private const val GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/latest"
private const val GITHUB_RELEASES_LIST_API = "https://api.github.com/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases?per_page=10"
private const val MIN_VALID_APK_BYTES = 100_000L

data class GitHubReleaseInfo(
    val tagName: String,
    val versionName: String,
    val title: String,
    val releaseNotes: String,
    val publishedAt: String,
    val htmlUrl: String,
    val apkName: String?,
    val apkDownloadUrl: String?,
    val apkSizeBytes: Long
)

sealed interface AppUpdateUiState {
    data object Idle : AppUpdateUiState
    data object Checking : AppUpdateUiState
    data class UpToDate(val currentVersion: String) : AppUpdateUiState
    data class Available(val release: GitHubReleaseInfo) : AppUpdateUiState
    data class Downloading(
        val release: GitHubReleaseInfo,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : AppUpdateUiState {
        val progress: Float
            get() = if (totalBytes > 0L) {
                (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            } else 0f
    }
    data class ReadyToInstall(
        val release: GitHubReleaseInfo,
        val apkFile: File
    ) : AppUpdateUiState
    data class Error(
        val message: String,
        val release: GitHubReleaseInfo? = null
    ) : AppUpdateUiState
}

sealed interface DownloadState {
    data class Progress(val bytesDownloaded: Long, val totalBytes: Long) : DownloadState
    data class Completed(val file: File) : DownloadState
    data class Failed(val errorMessage: String) : DownloadState
}

object AppUpdateManager {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private fun getUpdatesDirectory(context: Context): File {
        return File(context.filesDir, "updates").apply { mkdirs() }
    }

    fun cleanupObsoleteUpdates(context: Context) {
        try {
            val currentVersionCode = getCurrentAppVersionCode(context)
            val dirs = listOf(
                File(context.cacheDir, "updates"),
                File(context.filesDir, "updates")
            )
            dirs.forEach { dir ->
                if (!dir.exists()) return@forEach
                dir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".tmp")) {
                        file.delete()
                    } else if (file.name.endsWith(".apk")) {
                        val pkgInfo = readApkPackageInfo(context, file)
                        val apkVer = pkgInfo?.let { PackageInfoCompat.getLongVersionCode(it) } ?: -1L
                        if (pkgInfo == null ||
                            pkgInfo.packageName != context.packageName ||
                            apkVer <= currentVersionCode
                        ) {
                            file.delete()
                        }
                    } else {
                        file.delete()
                    }
                }
            }
        } catch (_: Throwable) {
        }
    }

    fun findReadyUpdateApk(context: Context, release: GitHubReleaseInfo): File? {
        return try {
            val updatesDir = getUpdatesDirectory(context)
            val safeFileName = sanitizeApkFileName(release)
            val candidate = File(updatesDir, safeFileName)
            if (isValidUpdateApk(context, candidate, release.apkSizeBytes)) candidate else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun sanitizeApkFileName(release: GitHubReleaseInfo): String {
        return (release.apkName ?: "SaleryPulse-${release.versionName}.apk")
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .let { if (it.lowercase(Locale.US).endsWith(".apk")) it else "$it.apk" }
    }

    suspend fun fetchLatestRelease(): Result<GitHubReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val latestRequest = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_API)
                .header("Accept", "application/vnd.github+json")
                .header("Cache-Control", "no-cache")
                .header("User-Agent", "SaleryPulse-Android")
                .build()

            httpClient.newCall(latestRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val parsed = parseGitHubReleaseJson(body)
                    if (parsed != null && !parsed.apkDownloadUrl.isNullOrBlank()) {
                        return@withContext Result.success(parsed)
                    }
                }
            }

            // Fallback to /releases?per_page=10 to locate the newest published release with an APK asset
            val listRequest = Request.Builder()
                .url(GITHUB_RELEASES_LIST_API)
                .header("Accept", "application/vnd.github+json")
                .header("Cache-Control", "no-cache")
                .header("User-Agent", "SaleryPulse-Android")
                .build()

            httpClient.newCall(listRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("GitHub API HTTP ${response.code}")
                    )
                }
                val body = response.body?.string().orEmpty()
                val arr = JSONArray(body)
                if (arr.length() == 0) {
                    return@withContext Result.failure(IllegalStateException("No releases published yet"))
                }
                for (i in 0 until arr.length()) {
                    val releaseObj = arr.optJSONObject(i) ?: continue
                    if (releaseObj.optBoolean("draft", false)) continue
                    val parsed = parseGitHubReleaseJson(releaseObj.toString())
                    if (parsed != null && !parsed.apkDownloadUrl.isNullOrBlank()) {
                        return@withContext Result.success(parsed)
                    }
                }
                Result.failure(IllegalStateException("No downloadable APK package attached to release"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun downloadApk(
        context: Context,
        release: GitHubReleaseInfo
    ): Flow<DownloadState> = flow {
        val downloadUrl = release.apkDownloadUrl
        if (downloadUrl.isNullOrBlank()) {
            emit(DownloadState.Failed("No APK asset attached to this release"))
            return@flow
        }

        // Clear any legacy cache updates folder to avoid stale/corrupted cache files
        try {
            File(context.cacheDir, "updates").deleteRecursively()
        } catch (_: Throwable) {
        }

        val updatesDir = getUpdatesDirectory(context)
        updatesDir.listFiles()?.forEach { oldFile ->
            if (oldFile.name.endsWith(".apk") || oldFile.name.endsWith(".tmp")) {
                oldFile.delete()
            }
        }

        val safeFileName = sanitizeApkFileName(release)
        val tempFile = File(updatesDir, "$safeFileName.tmp")
        val targetFile = File(updatesDir, safeFileName)

        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "SaleryPulse-Android")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(DownloadState.Failed("HTTP ${response.code}"))
                    return@flow
                }
                val body = response.body ?: run {
                    emit(DownloadState.Failed("Empty response body"))
                    return@flow
                }

                val streamLength = body.contentLength()
                val expectedLength = if (streamLength > 0L) streamLength else release.apkSizeBytes
                emit(DownloadState.Progress(0L, expectedLength))

                var totalRead = 0L
                var lastEmitTime = 0L

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            totalRead += read

                            val now = System.currentTimeMillis()
                            if (now - lastEmitTime >= 80L || (expectedLength > 0 && totalRead >= expectedLength)) {
                                lastEmitTime = now
                                emit(DownloadState.Progress(totalRead, expectedLength))
                            }
                        }
                        output.flush()
                    }
                }

                // Verify download completeness before renaming
                if (streamLength > 0L && totalRead != streamLength) {
                    tempFile.delete()
                    emit(DownloadState.Failed("Incomplete download ($totalRead / $streamLength bytes)"))
                    return@flow
                }

                if (targetFile.exists()) targetFile.delete()
                val renamed = tempFile.renameTo(targetFile)
                val finalFile = if (renamed) targetFile else tempFile

                // Verify the downloaded file is a valid APK archive for this app
                if (!isValidUpdateApk(context, finalFile, 0L)) {
                    finalFile.delete()
                    emit(DownloadState.Failed("Downloaded package is corrupted or invalid"))
                    return@flow
                }

                emit(DownloadState.Completed(finalFile))
            }
        } catch (e: CancellationException) {
            tempFile.delete()
            throw e
        } catch (t: Throwable) {
            tempFile.delete()
            targetFile.delete()
            emit(DownloadState.Failed(t.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    fun isValidUpdateApk(context: Context, apkFile: File, expectedMinSize: Long = 0L): Boolean {
        if (!hasValidApkHeaderAndSize(apkFile, expectedMinSize)) return false
        val pkgInfo = readApkPackageInfo(context, apkFile) ?: return false
        return pkgInfo.packageName == context.packageName
    }

    fun hasValidApkHeaderAndSize(apkFile: File, expectedSizeBytes: Long = 0L): Boolean {
        if (!apkFile.exists() || !apkFile.isFile) return false
        val len = apkFile.length()
        if (len < MIN_VALID_APK_BYTES) return false
        if (expectedSizeBytes > 0L && len != expectedSizeBytes) return false

        return try {
            FileInputStream(apkFile).use { fis ->
                val header = ByteArray(4)
                fis.read(header) == 4 &&
                    header[0] == 0x50.toByte() &&
                    header[1] == 0x4B.toByte() &&
                    header[2] == 0x03.toByte() &&
                    header[3] == 0x04.toByte()
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun readApkPackageInfo(context: Context, apkFile: File): PackageInfo? {
        return try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun getCurrentAppVersionCode(context: Context): Long {
        return try {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
            PackageInfoCompat.getLongVersionCode(info)
        } catch (_: Throwable) {
            0L
        }
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else {
                true
            }
        } catch (_: Throwable) {
            true
        }
    }

    fun buildManageUnknownAppSourcesIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val specific = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
            if (specific.resolveActivity(context.packageManager) != null) {
                specific
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    fun installApk(context: Context, apkFile: File): Boolean {
        return try {
            if (!isValidUpdateApk(context, apkFile, 0L)) {
                apkFile.delete()
                return false
            }

            val activity = context.findActivity()
            val launchContext: Context = activity ?: context.applicationContext

            val uri = FileProvider.getUriForFile(
                launchContext,
                "${launchContext.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                clipData = ClipData.newRawUri("SaleryPulse Update", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (activity == null) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            // Explicitly grant URI read permissions to resolved package installer(s) (critical on Xiaomi MIUI/HyperOS)
            val resolvedInstallers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launchContext.packageManager.queryIntentActivities(
                    installIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                launchContext.packageManager.queryIntentActivities(installIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }

            resolvedInstallers.forEach { resolveInfo ->
                val pkgName = resolveInfo.activityInfo?.packageName
                if (!pkgName.isNullOrBlank()) {
                    try {
                        launchContext.grantUriPermission(
                            pkgName,
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Throwable) {
                    }
                }
            }

            if (resolvedInstallers.isNotEmpty()) {
                launchContext.startActivity(installIntent)
                return true
            }

            // Fallback to legacy ACTION_INSTALL_PACKAGE if ACTION_VIEW has no handler
            @Suppress("DEPRECATION")
            val fallbackIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = uri
                clipData = ClipData.newRawUri("SaleryPulse Update", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                if (activity == null) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            if (fallbackIntent.resolveActivity(launchContext.packageManager) != null) {
                launchContext.startActivity(fallbackIntent)
                true
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

fun parseGitHubReleaseJson(jsonStr: String): GitHubReleaseInfo? {
    return try {
        val obj = JSONObject(jsonStr)
        val tagName = obj.optString("tag_name", "").trim()
        if (tagName.isBlank()) return null

        val cleanVersion = tagName.removePrefix("v").removePrefix("V").trim()
        val title = obj.optString("name", "").ifBlank { "SaleryPulse $tagName" }
        val notes = obj.optString("body", "").trim()
        val publishedAt = obj.optString("published_at", "").trim()
        val htmlUrl = obj.optString("html_url", GITHUB_REPO_URL).ifBlank { GITHUB_REPO_URL }

        var apkName: String? = null
        var apkDownloadUrl: String? = null
        var apkSize = 0L

        val assets = obj.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name", "")
                val url = asset.optString("browser_download_url", "")
                if (name.lowercase(Locale.US).endsWith(".apk") && url.isNotBlank()) {
                    apkName = name
                    apkDownloadUrl = url
                    apkSize = asset.optLong("size", 0L)
                    break
                }
            }
        }

        GitHubReleaseInfo(
            tagName = tagName,
            versionName = cleanVersion,
            title = title,
            releaseNotes = notes,
            publishedAt = publishedAt,
            htmlUrl = htmlUrl,
            apkName = apkName,
            apkDownloadUrl = apkDownloadUrl,
            apkSizeBytes = apkSize
        )
    } catch (_: Exception) {
        null
    }
}

/**
 * Compares two version strings like "v1.0.14" and "1.0.3".
 * Returns > 0 if [remoteTag] is newer than [currentVersion], 0 if equal, < 0 if older.
 */
fun compareVersionTags(remoteTag: String, currentVersion: String): Int {
    val remoteParts = extractVersionParts(remoteTag)
    val currentParts = extractVersionParts(currentVersion)
    val maxLen = maxOf(remoteParts.size, currentParts.size)

    for (i in 0 until maxLen) {
        val r = remoteParts.getOrElse(i) { 0 }
        val c = currentParts.getOrElse(i) { 0 }
        if (r != c) return r.compareTo(c)
    }
    return 0
}

private fun extractVersionParts(tag: String): List<Int> {
    val normalized = tag.trim().removePrefix("v").removePrefix("V")
    return Regex("\\d+")
        .findAll(normalized)
        .mapNotNull { it.value.toIntOrNull() }
        .toList()
}

fun formatBytesToMb(bytes: Long): String {
    if (bytes <= 0L) return "0.0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f MB", mb)
}
