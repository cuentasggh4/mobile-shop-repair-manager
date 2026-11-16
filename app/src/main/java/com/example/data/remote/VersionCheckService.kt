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

object VersionCheckService {

    private val rawApi: GitHubUpdateApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubUpdateApi::class.java)
    }

    fun isNewerVersionCode(remoteVersionCode: Int, currentVersionCode: Int): Boolean {
        return remoteVersionCode > currentVersionCode
    }

    suspend fun checkLatestVersion(
        repoSlug: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): AppUpdateResult = withContext(Dispatchers.IO) {
        val parsed = AppUpdateChecker.parseRepoSlug(repoSlug)
            ?: return@withContext AppUpdateResult.InvalidRepo
        val (owner, repo) = parsed
        val canonicalSlug = "$owner/$repo"
        val currentClean = AppUpdateChecker.cleanVersionName(currentVersionName)
        val candidateUrls = listOf(
            "https://raw.githubusercontent.com/$owner/$repo/main/version.json",
            "https://raw.githubusercontent.com/$owner/$repo/master/version.json"
        )

        var manifest: RemoteVersionManifest? = null
        var lastHttpError: HttpException? = null

        for (url in candidateUrls) {
            try {
                manifest = rawApi.getRemoteVersionManifest(url)
                break
            } catch (e: HttpException) {
                lastHttpError = e
                if (e.code() != 404) {
                    return@withContext when (e.code()) {
                        403 -> AppUpdateResult.Error("GitHub rate limit reached. Try again later.")
                        else -> AppUpdateResult.Error("GitHub HTTP ${e.code()}: ${e.message()}")
                    }
                }
            } catch (e: Exception) {
                return@withContext AppUpdateResult.Error(
                    e.message ?: "Network error while checking version.json"
                )
            }
        }

        if (manifest == null) {
            // Fallback to GitHub Releases API if version.json is not yet published in the repo
            if (lastHttpError?.code() == 404) {
                return@withContext AppUpdateChecker.checkLatestRelease(
                    repoSlug = canonicalSlug,
                    currentVersion = currentVersionName
                )
            }
            return@withContext AppUpdateResult.NoReleasesFound(canonicalSlug)
        }

        val remoteVersionName = AppUpdateChecker.cleanVersionName(manifest.versionName)
            .ifBlank { manifest.versionCode.toString() }
        val releasePageUrl = manifest.releasePageUrl
            ?.takeIf { it.isNotBlank() }
            ?: "https://github.com/$canonicalSlug/releases/latest"
        val apkDownloadUrl = manifest.apkDownloadUrl
            ?.takeIf { it.isNotBlank() }
            ?: releasePageUrl

        val isNewer = if (manifest.versionCode > 0) {
            isNewerVersionCode(manifest.versionCode, currentVersionCode)
        } else {
            AppUpdateChecker.isRemoteVersionNewer(remoteVersionName, currentVersionName)
        }

        if (isNewer) {
            AppUpdateResult.UpdateAvailable(
                currentVersion = currentClean,
                latestVersion = remoteVersionName,
                releaseTitle = manifest.releaseTitle?.takeIf { it.isNotBlank() }
                    ?: "v$remoteVersionName",
                releaseNotes = manifest.releaseNotes?.trim().orEmpty(),
                releasePageUrl = releasePageUrl,
                apkDownloadUrl = apkDownloadUrl,
                publishedAt = manifest.publishedAt
            )
        } else {
            AppUpdateResult.UpToDate(
                currentVersion = currentClean,
                latestVersion = remoteVersionName,
                releasePageUrl = releasePageUrl
            )
        }
    }
}
