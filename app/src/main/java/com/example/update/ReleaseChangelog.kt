package com.example.update

import android.content.Context
import com.example.ui.i18n.APP_VERSION
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class ReleaseFeatureNote(
    val titleAr: String = "",
    val titleEn: String = "",
    val descAr: String = "",
    val descEn: String = "",
    val badge: String = ""
)

@JsonClass(generateAdapter = true)
data class LatestChangelogBundle(
    val versionTag: String = "",
    val features: List<ReleaseFeatureNote> = emptyList()
)

object ReleaseChangelogProvider {

    private const val WHATS_NEW_ASSET = "whats_new.json"
    private val embeddedJsonRegex = Regex(
        pattern = "<!--\\s*WHATS_NEW_JSON:(.*?)-->",
        options = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    private val metadataLinePrefixes = listOf(
        "developer:",
        "version:",
        "package:",
        "direct upgrade:",
        "automatically built"
    )

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val bundleAdapter by lazy {
        moshi.adapter(LatestChangelogBundle::class.java)
    }

    fun parseJsonBundle(json: String): LatestChangelogBundle? {
        if (json.isBlank()) return null
        return runCatching {
            val parsed = bundleAdapter.fromJson(json) ?: return null
            val validFeatures = parsed.features.filter {
                it.titleAr.isNotBlank() || it.titleEn.isNotBlank() ||
                    it.descAr.isNotBlank() || it.descEn.isNotBlank()
            }
            parsed.copy(features = validFeatures)
        }.getOrNull()
    }

    fun loadInstalledLatestNotes(context: Context): LatestChangelogBundle {
        val rawJson = runCatching {
            context.assets.open(WHATS_NEW_ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull().orEmpty()
        return parseJsonBundle(rawJson) ?: LatestChangelogBundle(versionTag = APP_VERSION)
    }

    /**
     * Parses ONLY the feature notes belonging to the fetched latest GitHub release.
     * Supports structured HTML-comment JSON payloads as well as standard markdown bullets.
     * Metadata lines (Developer/Package/Build boilerplate) are stripped automatically.
     */
    fun parseRemoteReleaseNotes(releaseNotes: String): LatestChangelogBundle? {
        if (releaseNotes.isBlank()) return null

        embeddedJsonRegex.find(releaseNotes)?.let { match ->
            val jsonCandidate = match.groupValues.getOrNull(1)?.trim().orEmpty()
            val parsedBundle = parseJsonBundle(jsonCandidate)
            if (parsedBundle != null && parsedBundle.features.isNotEmpty()) {
                return parsedBundle
            }
        }

        val cleanedFeatures = mutableListOf<ReleaseFeatureNote>()
        for (rawLine in releaseNotes.lines()) {
            val line = rawLine.trim()
            if (line.isBlank()) continue
            if (line.startsWith("#") || line.startsWith("<!--")) continue
            val bulletStripped = line
                .removePrefix("- ")
                .removePrefix("* ")
                .removePrefix("• ")
                .trim()
            if (bulletStripped.isBlank()) continue

            val markdownClean = bulletStripped.replace("**", "").trim()
            val lower = markdownClean.lowercase()
            if (metadataLinePrefixes.any { lower.startsWith(it) }) continue

            val sepIdx = when {
                markdownClean.contains(" — ") -> markdownClean.indexOf(" — ") to 3
                markdownClean.contains(": ") -> markdownClean.indexOf(": ") to 2
                else -> -1 to 0
            }

            if (sepIdx.first > 0) {
                val title = markdownClean.substring(0, sepIdx.first).trim()
                val desc = markdownClean.substring(sepIdx.first + sepIdx.second).trim()
                if (title.isNotBlank()) {
                    cleanedFeatures.add(
                        ReleaseFeatureNote(
                            titleAr = title,
                            titleEn = title,
                            descAr = desc,
                            descEn = desc
                        )
                    )
                }
            } else {
                cleanedFeatures.add(
                    ReleaseFeatureNote(
                        titleAr = markdownClean,
                        titleEn = markdownClean
                    )
                )
            }
        }

        return if (cleanedFeatures.isNotEmpty()) {
            LatestChangelogBundle(features = cleanedFeatures)
        } else {
            null
        }
    }

    /**
     * Resolves the "What's New" list for the Update Dialog:
     * - When a newer GitHub release is active (`release != null`) and carries feature notes,
     *   shows ONLY that latest release's added features.
     * - Otherwise (e.g., `UpToDate` or checking locally), loads `whats_new.json` containing ONLY
     *   the latest installed update's features.
     */
    fun resolveForUpdateDialog(
        context: Context,
        release: GitHubReleaseInfo?
    ): LatestChangelogBundle {
        if (release != null) {
            val remoteBundle = parseRemoteReleaseNotes(release.releaseNotes)
            if (remoteBundle != null && remoteBundle.features.isNotEmpty()) {
                return remoteBundle.copy(
                    versionTag = remoteBundle.versionTag.ifBlank { release.tagName }
                )
            }
        }
        val localBundle = loadInstalledLatestNotes(context)
        return if (release != null && localBundle.features.isNotEmpty()) {
            localBundle.copy(versionTag = release.tagName.ifBlank { localBundle.versionTag })
        } else {
            localBundle
        }
    }
}
