package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

sealed interface AppUpdateResult {
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val releaseTitle: String,
        val releaseNotes: String,
        val releasePageUrl: String,
        val apkDownloadUrl: String?,
        val publishedAt: String?
    ) : AppUpdateResult

    data class UpToDate(
        val currentVersion: String,
        val latestVersion: String,
        val releasePageUrl: String
    ) : AppUpdateResult

    data class NoReleasesFound(val repoSlug: String) : AppUpdateResult
    data object InvalidRepo : AppUpdateResult
    data class Error(val message: String) : AppUpdateResult
}

object AppUpdateChecker {

    private val api: GitHubUpdateApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubUpdateApi::class.java)
    }

    fun parseRepoSlug(rawInput: String): Pair<String, String>? {
        val trimmed = rawInput.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .removePrefix("github.com/")
            .trim('/')
        if (trimmed.isBlank()) return null

        val parts = trimmed.split('/').filter { it.isNotBlank() }
        if (parts.size < 2) return null

        val owner = parts[0].trim()
        val repo = parts[1].removeSuffix(".git").trim()
        if (owner.isBlank() || repo.isBlank()) return null
        if (!owner.matches(Regex("^[A-Za-z0-9_.-]+$")) || !repo.matches(Regex("^[A-Za-z0-9_.-]+$"))) {
            return null
        }
        return owner to repo
    }

    fun normalizeRepoSlug(rawInput: String): String {
        val parsed = parseRepoSlug(rawInput) ?: return rawInput.trim()
        return "${parsed.first}/${parsed.second}"
    }

    fun cleanVersionName(rawVersion: String): String {
        return rawVersion.trim()
            .removePrefix("v")
            .removePrefix("V")
            .trim()
    }

    fun isRemoteVersionNewer(remoteTag: String, currentVersion: String): Boolean {
        val remoteNormalized = cleanVersionName(remoteTag)
        val currentNormalized = cleanVersionName(currentVersion)
        if (remoteNormalized.isBlank()) return false
        if (currentNormalized.isBlank()) return true

        val (remoteCore, remotePre) = splitCoreAndPreRelease(remoteNormalized)
        val (currentCore, currentPre) = splitCoreAndPreRelease(currentNormalized)

        val remoteParts = parseNumericParts(remoteCore)
        val currentParts = parseNumericParts(currentCore)
        val maxLen = maxOf(remoteParts.size, currentParts.size)

        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r != c) return r > c
        }

        // Same numeric version: stable release > pre-release
        if (remotePre == null && currentPre != null) return true
        if (remotePre != null && currentPre == null) return false
        if (remotePre != null && currentPre != null) {
            return remotePre.compareTo(currentPre, ignoreCase = true) > 0
        }
        return false
    }

    private fun splitCoreAndPreRelease(version: String): Pair<String, String?> {
        val withoutBuildMeta = version.substringBefore('+')
        val dashIdx = withoutBuildMeta.indexOf('-')
        return if (dashIdx >= 0) {
            withoutBuildMeta.substring(0, dashIdx) to withoutBuildMeta.substring(dashIdx + 1)
        } else {
            withoutBuildMeta to null
        }
    }

    private fun parseNumericParts(core: String): List<Int> {
        return core.split('.')
            .mapNotNull { segment ->
                val digits = segment.takeWhile { it.isDigit() }
                digits.toIntOrNull()
            }
            .ifEmpty { listOf(0) }
    }

    suspend fun checkLatestRelease(
        repoSlug: String,
        currentVersion: String
    ): AppUpdateResult = withContext(Dispatchers.IO) {
        val parsed = parseRepoSlug(repoSlug) ?: return@withContext AppUpdateResult.InvalidRepo
        val (owner, repo) = parsed
        val canonicalSlug = "$owner/$repo"

        try {
            val release = api.getLatestRelease(owner = owner, repo = repo)
            if (release.draft) {
                return@withContext AppUpdateResult.NoReleasesFound(canonicalSlug)
            }
            val remoteVersion = cleanVersionName(release.tagName).ifBlank { release.tagName }
            val currentClean = cleanVersionName(currentVersion)
            val apkAssetUrl = release.assets
                .firstOrNull { it.name.endsWith(".apk", ignoreCase = true) && it.browserDownloadUrl.isNotBlank() }
                ?.browserDownloadUrl

            if (isRemoteVersionNewer(release.tagName, currentVersion)) {
                AppUpdateResult.UpdateAvailable(
                    currentVersion = currentClean,
                    latestVersion = remoteVersion,
                    releaseTitle = release.name?.takeIf { it.isNotBlank() } ?: "v$remoteVersion",
                    releaseNotes = release.body?.trim().orEmpty(),
                    releasePageUrl = release.htmlUrl.ifBlank { "https://github.com/$canonicalSlug/releases" },
                    apkDownloadUrl = apkAssetUrl,
                    publishedAt = release.publishedAt
                )
            } else {
                AppUpdateResult.UpToDate(
                    currentVersion = currentClean,
                    latestVersion = remoteVersion,
                    releasePageUrl = release.htmlUrl.ifBlank { "https://github.com/$canonicalSlug/releases" }
                )
            }
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> AppUpdateResult.NoReleasesFound(canonicalSlug)
                403 -> AppUpdateResult.Error("GitHub API rate limit reached. Try again later.")
                else -> AppUpdateResult.Error("GitHub HTTP ${e.code()}: ${e.message()}")
            }
        } catch (e: Exception) {
            AppUpdateResult.Error(e.message ?: "Network error while checking GitHub")
        }
    }
}
