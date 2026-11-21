package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class RemoteVersionManifest(
    @Json(name = "versionCode")
    val versionCode: Int = 0,
    @Json(name = "versionName")
    val versionName: String = "",
    @Json(name = "releaseTitle")
    val releaseTitle: String? = null,
    @Json(name = "releaseNotes")
    val releaseNotes: String? = null,
    @Json(name = "apkDownloadUrl")
    val apkDownloadUrl: String? = null,
    @Json(name = "releasePageUrl")
    val releasePageUrl: String? = null,
    @Json(name = "publishedAt")
    val publishedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubReleaseAsset(
    val name: String = "",
    @Json(name = "browser_download_url")
    val browserDownloadUrl: String = "",
    val size: Long = 0L
)

@JsonClass(generateAdapter = true)
data class GitHubReleaseResponse(
    @Json(name = "tag_name")
    val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @Json(name = "html_url")
    val htmlUrl: String = "",
    @Json(name = "published_at")
    val publishedAt: String? = null,
    val assets: List<GitHubReleaseAsset> = emptyList()
)

interface GitHubUpdateApi {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Accept") accept: String = "application/vnd.github+json",
        @Header("User-Agent") userAgent: String = "MobileRepairShop-Android"
    ): GitHubReleaseResponse

    @GET
    suspend fun getRemoteVersionManifest(
        @Url url: String,
        @Header("Cache-Control") cacheControl: String = "no-cache",
        @Header("User-Agent") userAgent: String = "MobileRepairShop-Android"
    ): RemoteVersionManifest
}
