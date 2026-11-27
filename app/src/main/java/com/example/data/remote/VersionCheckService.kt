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
        // Siempre se consulta la última Release publicada por Actions (tag = versionName).
        // El antiguo version.json del repo quedó desactualizado (decía 1.0) y no se usa.
        return@withContext AppUpdateChecker.checkLatestRelease(
            repoSlug = canonicalSlug,
            currentVersion = currentVersionName
        )
    }
}
