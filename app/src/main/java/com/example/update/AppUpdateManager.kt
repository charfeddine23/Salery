package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
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
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

const val GITHUB_REPO_OWNER = "charfeddine23"
const val GITHUB_REPO_NAME = "Salery"
const val GITHUB_REPO_URL = "https://github.com/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME"
const val GITHUB_RELEASES_WEB_URL = "$GITHUB_REPO_URL/releases"
private const val GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/latest"
private const val GITHUB_RELEASES_LIST_API = "https://api.github.com/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases?per_page=1"

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
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    suspend fun fetchLatestRelease(): Result<GitHubReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val latestRequest = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_API)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "SaleryPulse-Android")
                .build()

            httpClient.newCall(latestRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val parsed = parseGitHubReleaseJson(body)
                    if (parsed != null) return@withContext Result.success(parsed)
                }
            }

            // Fallback to /releases?per_page=1 if latest endpoint returns 404 (e.g. prerelease/tag list)
            val listRequest = Request.Builder()
                .url(GITHUB_RELEASES_LIST_API)
                .header("Accept", "application/vnd.github+json")
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
                val parsed = parseGitHubReleaseJson(arr.getJSONObject(0).toString())
                    ?: return@withContext Result.failure(IllegalStateException("Invalid release metadata"))
                Result.success(parsed)
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

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        // Clean stale APK/temp files
        updatesDir.listFiles()?.forEach { oldFile ->
            if (oldFile.name.endsWith(".apk") || oldFile.name.endsWith(".tmp")) {
                oldFile.delete()
            }
        }

        val safeFileName = (release.apkName ?: "SaleryPulse-${release.versionName}.apk")
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
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

                val contentLength = body.contentLength().let {
                    if (it > 0L) it else release.apkSizeBytes
                }

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
                            if (now - lastEmitTime >= 80L || (contentLength > 0 && totalRead >= contentLength)) {
                                lastEmitTime = now
                                emit(DownloadState.Progress(totalRead, contentLength))
                            }
                        }
                        output.flush()
                    }
                }

                if (targetFile.exists()) targetFile.delete()
                val renamed = tempFile.renameTo(targetFile)
                val finalFile = if (renamed) targetFile else tempFile
                emit(DownloadState.Completed(finalFile))
            }
        } catch (e: CancellationException) {
            tempFile.delete()
            throw e
        } catch (e: Exception) {
            tempFile.delete()
            emit(DownloadState.Failed(e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun buildManageUnknownAppSourcesIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    fun installApk(context: Context, apkFile: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openReleaseInBrowser(context: Context, url: String = GITHUB_RELEASES_WEB_URL) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
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
        val htmlUrl = obj.optString("html_url", GITHUB_RELEASES_WEB_URL).ifBlank { GITHUB_RELEASES_WEB_URL }

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
