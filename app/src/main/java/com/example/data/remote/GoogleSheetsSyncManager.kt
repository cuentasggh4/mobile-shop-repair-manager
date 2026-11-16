package com.example.data.remote

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.IntentSender
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import com.example.data.dao.JobCardDao
import com.example.data.dao.ShopProfileDao
import com.example.data.entity.JobCardEntity
import com.example.data.entity.ShopProfileEntity
import com.example.data.model.RepairStage
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val Context.sheetsSyncDataStore: DataStore<Preferences> by preferencesDataStore(name = "google_sheets_sync")

data class GoogleSyncConfig(
    val isConnected: Boolean = false,
    val accountEmail: String = "",
    val spreadsheetId: String = "",
    val driveFolderId: String = "",
    val lastSyncedAt: Long = 0L,
    val pendingDeleteCount: Int = 0,
    val nightlyBackupEnabled: Boolean = true,
    val lastBackupAt: Long = 0L,
    val lastBackupName: String = ""
)

data class SyncSummary(
    val pulledCount: Int = 0,
    val pushedCount: Int = 0,
    val deletedCount: Int = 0,
    val spreadsheetId: String = "",
    val accountEmail: String = ""
)

sealed interface AuthTokenOutcome {
    data class Ready(val accessToken: String) : AuthTokenOutcome
    data class NeedsConsent(val intentSender: IntentSender) : AuthTokenOutcome
}

class GoogleSheetsSyncManager(
    private val context: Context,
    private val jobCardDao: JobCardDao,
    private val shopProfileDao: ShopProfileDao
) {
    companion object {
        const val APP_FOLDER_NAME = "Mobile Repair Shop"
        private const val SPREADSHEET_TITLE = "MobileRepairShop_DB"
        private const val BACKUP_PREFIX = "MobileRepairShop_Backup_"
        private const val MIME_FOLDER = "application/vnd.google-apps.folder"
        private const val MIME_SPREADSHEET = "application/vnd.google-apps.spreadsheet"
        private const val SHEET_JOBS = "Jobs"
        private const val SHEET_PROFILE = "ShopProfile"
        private const val SHEET_AUDIT = "AuditLog"

        private val KEY_CONNECTED_EMAIL = stringPreferencesKey("connected_email")
        private val KEY_SPREADSHEET_ID = stringPreferencesKey("spreadsheet_id")
        private val KEY_DRIVE_FOLDER_ID = stringPreferencesKey("drive_folder_id")
        private val KEY_LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        private val KEY_PENDING_DELETES = stringSetPreferencesKey("pending_deleted_job_numbers")
        private val KEY_NIGHTLY_BACKUP_ENABLED = booleanPreferencesKey("nightly_backup_enabled")
        private val KEY_LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        private val KEY_LAST_BACKUP_NAME = stringPreferencesKey("last_backup_name")

        private val REQUIRED_SCOPES = listOf(
            Scope("https://www.googleapis.com/auth/spreadsheets"),
            Scope("https://www.googleapis.com/auth/drive.file")
        )

        private val JOBS_HEADER = listOf(
            "JobNumber", "CustomerName", "CustomerPhone", "DeviceBrand", "DeviceModel",
            "DeviceColor", "ImeiOrSerial", "SecurityLockCode", "IssueDescription",
            "IssueTagsCsv", "Stage", "EstimatedCost", "AdvancePaid", "Discount",
            "FinalCost", "IsPaid", "PaymentMode", "TechnicianNotes",
            "PickedUpByOriginalCustomer", "RecipientName", "RecipientPhone",
            "ChecklistJson", "StatusLogsJson", "CreatedAt", "UpdatedAt", "DeliveredAt",
            "IsDeleted", "RecordUuid"
        )

        private val PROFILE_HEADER = listOf(
            "ShopName", "Tagline", "ContactPhone", "WhatsappNumber", "Email",
            "Address", "CurrencySymbol", "WarrantyDays", "TermsAndConditions",
            "NextJobNumberSequence", "PreferredWhatsAppPackage", "UpdatedAt",
            "JobNumberPrefix", "LastJobMonth"
        )

        private val AUDIT_HEADER = listOf(
            "Timestamp", "IsoTime", "Action", "RecordUuid", "JobNumber", "Details"
        )
    }

    private val appContext = context.applicationContext
    private val syncMutex = Mutex()
    private val verifiedSpreadsheetsThisSession = mutableSetOf<String>()

    private val api: GoogleSheetsApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://sheets.googleapis.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GoogleSheetsApi::class.java)
    }

    val syncConfig: Flow<GoogleSyncConfig> = appContext.sheetsSyncDataStore.data.map { prefs ->
        val email = prefs[KEY_CONNECTED_EMAIL].orEmpty()
        val sheetId = prefs[KEY_SPREADSHEET_ID].orEmpty()
        GoogleSyncConfig(
            isConnected = email.isNotBlank() || sheetId.isNotBlank(),
            accountEmail = email,
            spreadsheetId = sheetId,
            driveFolderId = prefs[KEY_DRIVE_FOLDER_ID].orEmpty(),
            lastSyncedAt = prefs[KEY_LAST_SYNC_AT] ?: 0L,
            pendingDeleteCount = prefs[KEY_PENDING_DELETES]?.size ?: 0,
            nightlyBackupEnabled = prefs[KEY_NIGHTLY_BACKUP_ENABLED] ?: true,
            lastBackupAt = prefs[KEY_LAST_BACKUP_AT] ?: 0L,
            lastBackupName = prefs[KEY_LAST_BACKUP_NAME].orEmpty()
        )
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    private fun formatAuthApiException(e: ApiException): String {
        val statusText = CommonStatusCodes.getStatusCodeString(e.statusCode)
        return when (e.statusCode) {
            CommonStatusCodes.INTERNAL_ERROR ->
                "Google Auth internal error (code 8). Check Android OAuth Client (package + SHA-1), Sheets & Drive APIs & Test Users in Google Cloud."
            CommonStatusCodes.DEVELOPER_ERROR, 12500 ->
                "Google Cloud Android OAuth Client / SHA-1 mismatch (code ${e.statusCode}: $statusText)"
            CommonStatusCodes.CANCELED ->
                "Consent cancelled or blocked by OAuth screen (code ${e.statusCode})"
            CommonStatusCodes.NETWORK_ERROR ->
                "Network error during Google Sign-In (code ${e.statusCode})"
            else ->
                "Google Auth error ${e.statusCode} ($statusText): ${e.localizedMessage ?: ""}"
        }
    }

    suspend fun requestAuthorization(activityContext: Context = appContext): AuthTokenOutcome = suspendCancellableCoroutine { continuation ->
        val authRequest = AuthorizationRequest.builder()
            .setRequestedScopes(REQUIRED_SCOPES)
            .build()

        val client = activityContext.findActivity()?.let { Identity.getAuthorizationClient(it) }
            ?: Identity.getAuthorizationClient(appContext)

        client
            .authorize(authRequest)
            .addOnSuccessListener { result ->
                if (!continuation.isActive) return@addOnSuccessListener
                if (result.hasResolution()) {
                    val sender = result.pendingIntent?.intentSender
                    if (sender != null) {
                        continuation.resume(AuthTokenOutcome.NeedsConsent(sender))
                    } else {
                        continuation.resumeWithException(IllegalStateException("Missing consent intent"))
                    }
                } else {
                    val token = result.accessToken
                    if (!token.isNullOrBlank()) {
                        continuation.resume(AuthTokenOutcome.Ready(token))
                    } else {
                        continuation.resumeWithException(IllegalStateException("Empty Google access token"))
                    }
                }
            }
            .addOnFailureListener { error ->
                if (continuation.isActive) {
                    val mapped = if (error is ApiException) {
                        IllegalStateException(formatAuthApiException(error), error)
                    } else {
                        error
                    }
                    continuation.resumeWithException(mapped)
                }
            }
    }

    fun extractTokenFromIntent(data: android.content.Intent?, activityContext: Context = appContext): String? {
        return try {
            val client = activityContext.findActivity()?.let { Identity.getAuthorizationClient(it) }
                ?: Identity.getAuthorizationClient(appContext)
            val result: AuthorizationResult = client.getAuthorizationResultFromIntent(data)
            result.accessToken
        } catch (e: ApiException) {
            throw IllegalStateException(formatAuthApiException(e), e)
        }
    }

    suspend fun getCachedAccessTokenOrNull(): String? {
        return try {
            when (val outcome = requestAuthorization()) {
                is AuthTokenOutcome.Ready -> outcome.accessToken
                is AuthTokenOutcome.NeedsConsent -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveCustomSpreadsheetId(rawInput: String) {
        val extracted = extractSpreadsheetId(rawInput)
        appContext.sheetsSyncDataStore.edit { prefs ->
            prefs[KEY_SPREADSHEET_ID] = extracted
        }
    }

    suspend fun setNightlyBackupEnabled(enabled: Boolean) {
        appContext.sheetsSyncDataStore.edit { prefs ->
            prefs[KEY_NIGHTLY_BACKUP_ENABLED] = enabled
        }
    }

    suspend fun markJobDeletedForSync(recordKey: String) {
        if (recordKey.isBlank()) return
        appContext.sheetsSyncDataStore.edit { prefs ->
            val current = prefs[KEY_PENDING_DELETES].orEmpty().toMutableSet()
            current.add(recordKey)
            prefs[KEY_PENDING_DELETES] = current
        }
    }

    suspend fun clearConnection() {
        verifiedSpreadsheetsThisSession.clear()
        appContext.sheetsSyncDataStore.edit { prefs ->
            prefs.remove(KEY_CONNECTED_EMAIL)
            prefs.remove(KEY_SPREADSHEET_ID)
            prefs.remove(KEY_DRIVE_FOLDER_ID)
            prefs.remove(KEY_LAST_SYNC_AT)
            prefs.remove(KEY_PENDING_DELETES)
            prefs.remove(KEY_LAST_BACKUP_AT)
            prefs.remove(KEY_LAST_BACKUP_NAME)
        }
    }

    suspend fun syncWithToken(accessToken: String): SyncSummary = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            syncWithTokenLocked(accessToken, fastMode = false)
        }
    }

    private suspend fun syncWithTokenLocked(
        accessToken: String,
        fastMode: Boolean = false
    ): SyncSummary {
        val bearer = "Bearer $accessToken"
        val prefs = appContext.sheetsSyncDataStore.data.first()
        val cachedEmail = prefs[KEY_CONNECTED_EMAIL].orEmpty()
        val cachedFolderId = prefs[KEY_DRIVE_FOLDER_ID].orEmpty()
        val cachedSheetId = prefs[KEY_SPREADSHEET_ID].orEmpty()

        val userEmail = if (fastMode && cachedEmail.isNotBlank()) {
            cachedEmail
        } else {
            try {
                api.getUserInfo(bearer).email.orEmpty()
            } catch (_: Exception) {
                cachedEmail
            }
        }

        val (folderId, spreadsheetId) = if (fastMode && cachedFolderId.isNotBlank() && cachedSheetId.isNotBlank()) {
            if (!verifiedSpreadsheetsThisSession.contains(cachedSheetId)) {
                val meta = api.getSpreadsheetMetadata(bearer, cachedSheetId)
                ensureTabsAndHeaders(bearer, cachedSheetId, meta)
                verifiedSpreadsheetsThisSession.add(cachedSheetId)
            }
            cachedFolderId to cachedSheetId
        } else {
            val resolvedFolder = ensureAppDriveFolder(bearer)
            val resolvedSheet = ensureShopSpreadsheet(accessToken, resolvedFolder)
            verifiedSpreadsheetsThisSession.add(resolvedSheet)
            resolvedFolder to resolvedSheet
        }

        val result = performTwoWaySync(bearer, spreadsheetId, userEmail)

        appContext.sheetsSyncDataStore.edit { editPrefs ->
            if (userEmail.isNotBlank()) editPrefs[KEY_CONNECTED_EMAIL] = userEmail
            editPrefs[KEY_SPREADSHEET_ID] = spreadsheetId
            editPrefs[KEY_DRIVE_FOLDER_ID] = folderId
            editPrefs[KEY_LAST_SYNC_AT] = System.currentTimeMillis()
        }
        return result
    }

    suspend fun createDatedFullBackup(
        accessToken: String,
        forceNewSnapshot: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val summary = syncWithTokenLocked(accessToken)
            val bearer = "Bearer $accessToken"
            val folderId = ensureAppDriveFolder(bearer)

            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val baseName = "$BACKUP_PREFIX$dateStr"
            val existingTodayQuery = "name = '$baseName' and '$folderId' in parents and mimeType = '$MIME_SPREADSHEET' and trashed = false"
            val existingToday = api.findSpreadsheets(bearer, existingTodayQuery).files.orEmpty().firstOrNull()

            if (existingToday != null && !forceNewSnapshot) {
                appContext.sheetsSyncDataStore.edit { prefs ->
                    prefs[KEY_LAST_BACKUP_AT] = System.currentTimeMillis()
                    prefs[KEY_LAST_BACKUP_NAME] = existingToday.name ?: baseName
                }
                return@withLock existingToday.name ?: baseName
            }

            val backupName = if (existingToday != null && forceNewSnapshot) {
                val timeStr = SimpleDateFormat("HHmmss", Locale.US).format(Date())
                "${baseName}_$timeStr"
            } else {
                baseName
            }

            try {
                api.copyDriveFile(
                    bearerToken = bearer,
                    fileId = summary.spreadsheetId,
                    request = DriveCopyFileRequest(
                        name = backupName,
                        parents = listOf(folderId)
                    )
                )
            } catch (_: Exception) {
                // Fallback for externally-linked spreadsheets that drive.file cannot copy directly
                val created = api.createDriveFile(
                    bearerToken = bearer,
                    request = DriveCreateFileRequest(
                        name = backupName,
                        mimeType = MIME_SPREADSHEET,
                        parents = listOf(folderId)
                    )
                )
                val meta = api.getSpreadsheetMetadata(bearer, created.id)
                ensureTabsAndHeaders(bearer, created.id, meta)
                val allLocal = jobCardDao.getAllJobsSync()
                if (allLocal.isNotEmpty()) {
                    api.appendValues(
                        bearerToken = bearer,
                        spreadsheetId = created.id,
                        range = "$SHEET_JOBS!A:AB",
                        body = ValueRangeRequest(allLocal.map { serializeJobRow(it, isDeleted = false) })
                    )
                }
                shopProfileDao.getShopProfileSync()?.let { profile ->
                    api.updateValues(
                        bearerToken = bearer,
                        spreadsheetId = created.id,
                        range = "$SHEET_PROFILE!A2:L2",
                        body = ValueRangeRequest(listOf(serializeProfileRow(profile)))
                    )
                }
            }

            val now = System.currentTimeMillis()
            appContext.sheetsSyncDataStore.edit { prefs ->
                prefs[KEY_LAST_BACKUP_AT] = now
                prefs[KEY_LAST_BACKUP_NAME] = backupName
            }
            backupName
        }
    }

    suspend fun tryBackgroundAutoSync(): Boolean {
        val config = syncConfig.first()
        if (!config.isConnected) return false
        val token = getCachedAccessTokenOrNull() ?: return false
        return try {
            withContext(Dispatchers.IO) {
                syncMutex.withLock {
                    syncWithTokenLocked(token, fastMode = true)
                }
            }
            true
        } catch (_: Exception) {
            // Keep offline-first UX resilient; manual sync surfaces exact error
            false
        }
    }

    suspend fun ensureAppDriveFolder(bearer: String): String {
        val folderQuery = "name = '$APP_FOLDER_NAME' and mimeType = '$MIME_FOLDER' and trashed = false"
        val existingFolder = api.findSpreadsheets(bearer, folderQuery).files.orEmpty().firstOrNull()
        if (existingFolder != null) {
            appContext.sheetsSyncDataStore.edit { prefs ->
                prefs[KEY_DRIVE_FOLDER_ID] = existingFolder.id
            }
            return existingFolder.id
        }

        val createdFolder = api.createDriveFile(
            bearerToken = bearer,
            request = DriveCreateFileRequest(
                name = APP_FOLDER_NAME,
                mimeType = MIME_FOLDER
            )
        )
        appContext.sheetsSyncDataStore.edit { prefs ->
            prefs[KEY_DRIVE_FOLDER_ID] = createdFolder.id
        }
        return createdFolder.id
    }

    private suspend fun moveFileIntoFolderIfNeeded(
        bearer: String,
        fileItem: DriveFileItem,
        folderId: String
    ) {
        val currentParents = fileItem.parents.orEmpty()
        if (currentParents.contains(folderId)) return
        try {
            api.moveDriveFileToFolder(
                bearerToken = bearer,
                fileId = fileItem.id,
                addParents = folderId,
                removeParents = currentParents.takeIf { it.isNotEmpty() }?.joinToString(",")
            )
        } catch (_: Exception) {
            // Non-fatal if external file permissions restrict parent change
        }
    }

    suspend fun ensureShopSpreadsheet(
        accessToken: String,
        targetFolderId: String? = null
    ): String = withContext(Dispatchers.IO) {
        val bearer = "Bearer $accessToken"
        val folderId = targetFolderId ?: ensureAppDriveFolder(bearer)
        val storedId = appContext.sheetsSyncDataStore.data.first()[KEY_SPREADSHEET_ID].orEmpty()

        if (storedId.isNotBlank()) {
            try {
                val meta = api.getSpreadsheetMetadata(bearer, storedId)
                try {
                    val driveMeta = api.getDriveFileMetadata(bearer, storedId)
                    if (driveMeta.name == SPREADSHEET_TITLE) {
                        moveFileIntoFolderIfNeeded(bearer, driveMeta, folderId)
                    }
                } catch (_: Exception) {
                }
                ensureTabsAndHeaders(bearer, meta.spreadsheetId, meta)
                return@withContext meta.spreadsheetId
            } catch (e: HttpException) {
                if (e.code() != 404) throw e
            }
        }

        val inFolderQuery = "name = '$SPREADSHEET_TITLE' and '$folderId' in parents and mimeType = '$MIME_SPREADSHEET' and trashed = false"
        val existingInFolder = api.findSpreadsheets(bearer, inFolderQuery).files.orEmpty().firstOrNull()
        if (existingInFolder != null) {
            val meta = api.getSpreadsheetMetadata(bearer, existingInFolder.id)
            ensureTabsAndHeaders(bearer, existingInFolder.id, meta)
            return@withContext existingInFolder.id
        }

        val driveQuery = "name = '$SPREADSHEET_TITLE' and mimeType = '$MIME_SPREADSHEET' and trashed = false"
        val existing = api.findSpreadsheets(bearer, driveQuery).files.orEmpty().firstOrNull()
        if (existing != null) {
            moveFileIntoFolderIfNeeded(bearer, existing, folderId)
            val meta = api.getSpreadsheetMetadata(bearer, existing.id)
            ensureTabsAndHeaders(bearer, existing.id, meta)
            return@withContext existing.id
        }

        val createdFile = api.createDriveFile(
            bearerToken = bearer,
            request = DriveCreateFileRequest(
                name = SPREADSHEET_TITLE,
                mimeType = MIME_SPREADSHEET,
                parents = listOf(folderId)
            )
        )
        val meta = api.getSpreadsheetMetadata(bearer, createdFile.id)
        ensureTabsAndHeaders(bearer, createdFile.id, meta)
        createdFile.id
    }

    private suspend fun ensureTabsAndHeaders(
        bearer: String,
        spreadsheetId: String,
        meta: SpreadsheetMetadataResponse
    ) {
        val existingTitles = meta.sheets.orEmpty().mapNotNull { it.properties?.title }.toSet()
        val missingRequests = mutableListOf<BatchUpdateRequestItem>()
        if (!existingTitles.contains(SHEET_JOBS)) {
            missingRequests.add(BatchUpdateRequestItem(SheetCreateSpec(SheetPropertiesSpec(SHEET_JOBS))))
        }
        if (!existingTitles.contains(SHEET_PROFILE)) {
            missingRequests.add(BatchUpdateRequestItem(SheetCreateSpec(SheetPropertiesSpec(SHEET_PROFILE))))
        }
        if (!existingTitles.contains(SHEET_AUDIT)) {
            missingRequests.add(BatchUpdateRequestItem(SheetCreateSpec(SheetPropertiesSpec(SHEET_AUDIT))))
        }
        if (missingRequests.isNotEmpty()) {
            api.batchUpdateSpreadsheet(bearer, spreadsheetId, BatchUpdateSpreadsheetRequest(missingRequests))
        }
        writeHeaders(bearer, spreadsheetId)
    }

    private suspend fun writeHeaders(bearer: String, spreadsheetId: String) {
        api.batchUpdateValues(
            bearerToken = bearer,
            spreadsheetId = spreadsheetId,
            body = BatchUpdateValuesRequest(
                data = listOf(
                    ValueRangeUpdateItem("$SHEET_JOBS!A1:AB1", listOf(JOBS_HEADER)),
                    ValueRangeUpdateItem("$SHEET_PROFILE!A1:N1", listOf(PROFILE_HEADER)),
                    ValueRangeUpdateItem("$SHEET_AUDIT!A1:F1", listOf(AUDIT_HEADER))
                )
            )
        )
    }

    private suspend fun performTwoWaySync(
        bearer: String,
        spreadsheetId: String,
        userEmail: String
    ): SyncSummary {
        // 1. Ensure local ShopProfile exists
        val profileRows = api.getValues(bearer, spreadsheetId, "$SHEET_PROFILE!A2:N2").values.orEmpty()
        var localProfile = shopProfileDao.getShopProfileSync()
        if (localProfile == null) {
            localProfile = ShopProfileEntity(id = 1, updatedAt = 0L)
            shopProfileDao.insertProfile(localProfile)
        }
        val remoteProfile = profileRows.firstOrNull()?.let { parseProfileRow(it) }

        // 2. Ensure every local job has a durable RecordUuid before matching
        val initialLocalJobs = jobCardDao.getAllJobsSync()
        val localJobs = initialLocalJobs.map { job ->
            if (job.recordUuid.isBlank()) {
                val deterministic = deterministicUuid(job.jobNumber, job.createdAt)
                val fixed = job.copy(recordUuid = deterministic)
                jobCardDao.updateJob(fixed)
                fixed
            } else {
                job
            }
        }

        // 3. Read Remote Jobs with exact 1-based Sheet row numbers (row 2+)
        val rawJobRows = api.getValues(bearer, spreadsheetId, "$SHEET_JOBS!A2:AB").values.orEmpty()
        val remoteRowsByUuid = linkedMapOf<String, SheetJobRow>()
        val usedJobNumbers = mutableSetOf<String>()
        val maxSeqByMonth = mutableMapOf<String, Int>()
        val now = System.currentTimeMillis()
        val currentMonthKey = SimpleDateFormat("yyMM", Locale.US).format(Date(now))
        val todayYymmdd = SimpleDateFormat("yyMMdd", Locale.US).format(Date(now))

        fun recordMonthlySeq(jobNumber: String) {
            extractMonthlyJobParts(jobNumber)?.let { (yymmdd, seqNum) ->
                val monthKey = yymmdd.take(4)
                val prev = maxSeqByMonth[monthKey] ?: 0
                if (seqNum > prev) maxSeqByMonth[monthKey] = seqNum
            }
        }

        rawJobRows.forEachIndexed { index, row ->
            val parsed = parseJobRow(row, sheetRow = index + 2) ?: return@forEachIndexed
            recordMonthlySeq(parsed.job.jobNumber)
            usedJobNumbers.add(parsed.job.jobNumber)

            val existing = remoteRowsByUuid[parsed.job.recordUuid]
            if (existing == null || parsed.job.updatedAt >= existing.job.updatedAt) {
                remoteRowsByUuid[parsed.job.recordUuid] = parsed
            }
        }
        for (localItem in localJobs) {
            recordMonthlySeq(localItem.jobNumber)
        }

        // 4. Reconcile using UUID key (with safe legacy JobNumber fallback)
        val pendingDeletes = appContext.sheetsSyncDataStore.data.first()[KEY_PENDING_DELETES].orEmpty()
        val localByUuid = localJobs.associateBy { it.recordUuid }.toMutableMap()
        val claimedLocalIds = mutableSetOf<Long>()

        var pulledCount = 0
        var pushedCount = 0
        var deletedCount = 0

        val rowUpdates = mutableListOf<ValueRangeUpdateItem>()
        val rowsToAppend = mutableListOf<List<String>>()
        val auditEntries = mutableListOf<List<String>>()
        val sourceLabel = userEmail.ifBlank { "AndroidDevice" }

        for ((uuid, remoteItem) in remoteRowsByUuid) {
            var localItem = localByUuid[uuid]
            if (localItem == null) {
                val fallbackMatch = localJobs.find {
                    !claimedLocalIds.contains(it.id) &&
                        it.jobNumber == remoteItem.job.jobNumber &&
                        (remoteItem.needsUuidBackfill || it.recordUuid.startsWith("DEMO-"))
                }
                if (fallbackMatch != null) {
                    localItem = fallbackMatch
                }
            }
            localItem?.let { claimedLocalIds.add(it.id) }

            val markedDeletedLocally = pendingDeletes.contains(uuid) ||
                pendingDeletes.contains(remoteItem.job.jobNumber)

            if (markedDeletedLocally) {
                if (!remoteItem.isDeleted) {
                    val tombstoneJob = remoteItem.job.copy(updatedAt = now)
                    rowUpdates.add(
                        ValueRangeUpdateItem(
                            range = "$SHEET_JOBS!A${remoteItem.sheetRow}:AB${remoteItem.sheetRow}",
                            values = listOf(serializeJobRow(tombstoneJob, isDeleted = true))
                        )
                    )
                    auditEntries.add(
                        buildAuditRow(
                            timestamp = now,
                            action = "SOFT_DELETE",
                            uuid = uuid,
                            jobNumber = remoteItem.job.jobNumber,
                            details = "Marked deleted from $sourceLabel"
                        )
                    )
                }
                deletedCount++
                continue
            }

            if (remoteItem.isDeleted) {
                if (localItem != null && remoteItem.job.updatedAt >= localItem.updatedAt) {
                    jobCardDao.deleteJobById(localItem.id)
                    deletedCount++
                    auditEntries.add(
                        buildAuditRow(
                            timestamp = now,
                            action = "PULL_DELETE",
                            uuid = uuid,
                            jobNumber = remoteItem.job.jobNumber,
                            details = "Removed deleted record locally on $sourceLabel"
                        )
                    )
                } else if (localItem != null) {
                    val syncedLocal = localItem.copy(recordUuid = uuid)
                    jobCardDao.updateJob(syncedLocal)
                    rowUpdates.add(
                        ValueRangeUpdateItem(
                            range = "$SHEET_JOBS!A${remoteItem.sheetRow}:AB${remoteItem.sheetRow}",
                            values = listOf(serializeJobRow(syncedLocal, isDeleted = false))
                        )
                    )
                    pushedCount++
                    auditEntries.add(
                        buildAuditRow(
                            timestamp = now,
                            action = "RESTORE",
                            uuid = uuid,
                            jobNumber = syncedLocal.jobNumber,
                            details = "Restored newer local edit over delete on $sourceLabel"
                        )
                    )
                }
                continue
            }

            if (localItem == null) {
                jobCardDao.insertJob(remoteItem.job.copy(id = 0))
                pulledCount++
                if (remoteItem.needsUuidBackfill) {
                    rowUpdates.add(
                        ValueRangeUpdateItem(
                            range = "$SHEET_JOBS!A${remoteItem.sheetRow}:AB${remoteItem.sheetRow}",
                            values = listOf(serializeJobRow(remoteItem.job, isDeleted = false))
                        )
                    )
                }
                auditEntries.add(
                    buildAuditRow(
                        timestamp = now,
                        action = "PULL_CREATE",
                        uuid = uuid,
                        jobNumber = remoteItem.job.jobNumber,
                        details = "Pulled ${remoteItem.job.customerName} (${remoteItem.job.deviceModel}) to $sourceLabel"
                    )
                )
            } else if (remoteItem.job.updatedAt > localItem.updatedAt) {
                jobCardDao.updateJob(remoteItem.job.copy(id = localItem.id, recordUuid = uuid))
                pulledCount++
                if (remoteItem.needsUuidBackfill) {
                    rowUpdates.add(
                        ValueRangeUpdateItem(
                            range = "$SHEET_JOBS!A${remoteItem.sheetRow}:AB${remoteItem.sheetRow}",
                            values = listOf(serializeJobRow(remoteItem.job, isDeleted = false))
                        )
                    )
                }
                auditEntries.add(
                    buildAuditRow(
                        timestamp = now,
                        action = "PULL_UPDATE",
                        uuid = uuid,
                        jobNumber = remoteItem.job.jobNumber,
                        details = "Updated local record from Sheet (${remoteItem.job.stage.name})"
                    )
                )
            } else if (localItem.updatedAt > remoteItem.job.updatedAt || remoteItem.needsUuidBackfill) {
                val syncedLocal = if (localItem.recordUuid != uuid) {
                    localItem.copy(recordUuid = uuid).also { jobCardDao.updateJob(it) }
                } else {
                    localItem
                }
                rowUpdates.add(
                    ValueRangeUpdateItem(
                        range = "$SHEET_JOBS!A${remoteItem.sheetRow}:AB${remoteItem.sheetRow}",
                        values = listOf(serializeJobRow(syncedLocal, isDeleted = false))
                    )
                )
                if (localItem.updatedAt > remoteItem.job.updatedAt) {
                    pushedCount++
                    auditEntries.add(
                        buildAuditRow(
                            timestamp = now,
                            action = "PUSH_UPDATE",
                            uuid = uuid,
                            jobNumber = syncedLocal.jobNumber,
                            details = "Updated Sheet row ${remoteItem.sheetRow} (${syncedLocal.stage.name}) by $sourceLabel"
                        )
                    )
                }
            }
        }

        // 5. Append-only push for brand-new local jobs not yet in Sheet
        val hasExistingRemoteData = remoteRowsByUuid.isNotEmpty()
        for (localItem in localJobs) {
            if (claimedLocalIds.contains(localItem.id)) continue
            if (pendingDeletes.contains(localItem.recordUuid) || pendingDeletes.contains(localItem.jobNumber)) continue

            val isUntouchedDemo = localItem.recordUuid.startsWith("DEMO-") && localItem.updatedAt <= 1000L
            if (hasExistingRemoteData && isUntouchedDemo) {
                continue
            }

            val finalLocal = if (usedJobNumbers.contains(localItem.jobNumber)) {
                val activePrefix = localProfile.jobNumberPrefix
                    .trim()
                    .replace(Regex("\\s+"), "-")
                    .trim('-')
                    .ifBlank { "JOB" }
                val parsedParts = extractMonthlyJobParts(localItem.jobNumber)
                val targetYymmdd = parsedParts?.first ?: todayYymmdd
                val targetMonth = targetYymmdd.take(4)
                var candidateSeq = (maxSeqByMonth[targetMonth] ?: 0) + 1
                var reassignedNumber = "$activePrefix-$targetYymmdd-${String.format(Locale.US, "%03d", candidateSeq)}"
                while (usedJobNumbers.contains(reassignedNumber)) {
                    candidateSeq++
                    reassignedNumber = "$activePrefix-$targetYymmdd-${String.format(Locale.US, "%03d", candidateSeq)}"
                }
                maxSeqByMonth[targetMonth] = candidateSeq
                val renamed = localItem.copy(jobNumber = reassignedNumber, updatedAt = now)
                jobCardDao.updateJob(renamed)
                usedJobNumbers.add(reassignedNumber)
                auditEntries.add(
                    buildAuditRow(
                        timestamp = now,
                        action = "COLLISION_RENUMBER",
                        uuid = renamed.recordUuid,
                        jobNumber = reassignedNumber,
                        details = "Auto-renumbered offline duplicate ${localItem.jobNumber} -> $reassignedNumber"
                    )
                )
                renamed
            } else {
                usedJobNumbers.add(localItem.jobNumber)
                localItem
            }

            rowsToAppend.add(serializeJobRow(finalLocal, isDeleted = false))
            pushedCount++
            auditEntries.add(
                buildAuditRow(
                    timestamp = now,
                    action = "CREATE",
                    uuid = finalLocal.recordUuid,
                    jobNumber = finalLocal.jobNumber,
                    details = "Created ${finalLocal.customerName} (${finalLocal.deviceModel}) from $sourceLabel"
                )
            )
        }

        // 6. Execute row-targeted batchUpdate + appendOnly writes
        if (rowUpdates.isNotEmpty()) {
            api.batchUpdateValues(
                bearerToken = bearer,
                spreadsheetId = spreadsheetId,
                body = BatchUpdateValuesRequest(data = rowUpdates)
            )
        }
        if (rowsToAppend.isNotEmpty()) {
            api.appendValues(
                bearerToken = bearer,
                spreadsheetId = spreadsheetId,
                range = "$SHEET_JOBS!A:AB",
                body = ValueRangeRequest(rowsToAppend)
            )
        }
        if (auditEntries.isNotEmpty()) {
            api.appendValues(
                bearerToken = bearer,
                spreadsheetId = spreadsheetId,
                range = "$SHEET_AUDIT!A:F",
                body = ValueRangeRequest(auditEntries)
            )
        }

        // 7. Clear synced pending deletes
        if (pendingDeletes.isNotEmpty()) {
            appContext.sheetsSyncDataStore.edit { prefs ->
                prefs.remove(KEY_PENDING_DELETES)
            }
        }

        // 8. Two-way ShopProfile sync by UpdatedAt + Monthly NextJobNumberSequence
        val remoteProfileTs = remoteProfile?.updatedAt ?: 0L
        val baseProfile = when {
            remoteProfile != null && remoteProfileTs > localProfile.updatedAt -> remoteProfile
            remoteProfile != null &&
                localProfile.updatedAt == 0L &&
                localProfile.shopName == "TechCare Mobile Solutions" &&
                remoteProfile.shopName.isNotBlank() -> remoteProfile
            else -> localProfile
        }

        val currentMonthMaxFromJobs = (maxSeqByMonth[currentMonthKey] ?: 0) + 1
        val localMonthSeq = if (localProfile.lastJobMonth == currentMonthKey) localProfile.nextJobNumberSequence else 1
        val remoteMonthSeq = if (remoteProfile?.lastJobMonth == currentMonthKey) remoteProfile.nextJobNumberSequence else 1
        val hasCurrentMonthActivity =
            localProfile.lastJobMonth == currentMonthKey ||
                remoteProfile?.lastJobMonth == currentMonthKey ||
                (maxSeqByMonth[currentMonthKey] ?: 0) > 0

        val resolvedMonth = if (hasCurrentMonthActivity) currentMonthKey else baseProfile.lastJobMonth
        val resolvedSeq = if (hasCurrentMonthActivity) {
            maxOf(localMonthSeq, remoteMonthSeq, currentMonthMaxFromJobs)
        } else {
            1
        }
        val finalProfile = baseProfile.copy(
            id = 1,
            nextJobNumberSequence = resolvedSeq,
            lastJobMonth = resolvedMonth
        )
        shopProfileDao.insertProfile(finalProfile)

        if (remoteProfile == null ||
            finalProfile.updatedAt > remoteProfileTs ||
            resolvedSeq != remoteProfile.nextJobNumberSequence ||
            resolvedMonth != remoteProfile.lastJobMonth ||
            finalProfile.jobNumberPrefix != remoteProfile.jobNumberPrefix
        ) {
            api.updateValues(
                bearerToken = bearer,
                spreadsheetId = spreadsheetId,
                range = "$SHEET_PROFILE!A2:N2",
                body = ValueRangeRequest(listOf(serializeProfileRow(finalProfile)))
            )
        }

        return SyncSummary(
            pulledCount = pulledCount,
            pushedCount = pushedCount,
            deletedCount = deletedCount,
            spreadsheetId = spreadsheetId,
            accountEmail = userEmail
        )
    }

    private fun extractMonthlyJobParts(jobNumber: String): Pair<String, Int>? {
        val match = Regex("^.*-(\\d{6})-(\\d+)$").find(jobNumber.trim()) ?: return null
        val yymmdd = match.groupValues[1]
        val seq = match.groupValues[2].toIntOrNull() ?: return null
        return yymmdd to seq
    }

    private fun deterministicUuid(jobNumber: String, createdAt: Long): String =
        UUID.nameUUIDFromBytes("${jobNumber}_$createdAt".toByteArray()).toString()

    private fun buildAuditRow(
        timestamp: Long,
        action: String,
        uuid: String,
        jobNumber: String,
        details: String
    ): List<String> {
        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return listOf(
            timestamp.toString(),
            isoFormatter.format(Date(timestamp)),
            action,
            uuid,
            jobNumber,
            details
        )
    }

    private fun extractSpreadsheetId(input: String): String {
        val trimmed = input.trim()
        val regex = Regex("/spreadsheets/d/([a-zA-Z0-9-_]+)")
        return regex.find(trimmed)?.groupValues?.getOrNull(1) ?: trimmed
    }

    private fun serializeJobRow(job: JobCardEntity, isDeleted: Boolean): List<String> = listOf(
        job.jobNumber,
        job.customerName,
        job.customerPhone,
        job.deviceBrand,
        job.deviceModel,
        job.deviceColor,
        job.imeiOrSerial,
        job.securityLockCode,
        job.issueDescription,
        job.issueTagsCsv,
        job.stage.name,
        job.estimatedCost.toString(),
        job.advancePaid.toString(),
        job.discount.toString(),
        job.finalCost.toString(),
        job.isPaid.toString(),
        job.paymentMode,
        job.technicianNotes,
        job.pickedUpByOriginalCustomer.toString(),
        job.recipientName,
        job.recipientPhone,
        job.checklistJson,
        job.statusLogsJson,
        job.createdAt.toString(),
        job.updatedAt.toString(),
        job.deliveredAt?.toString() ?: "",
        isDeleted.toString(),
        job.recordUuid
    )

    private fun parseJobRow(row: List<String>, sheetRow: Int): SheetJobRow? {
        fun cell(idx: Int): String = row.getOrNull(idx).orEmpty()
        val jobNumber = cell(0).trim()
        if (jobNumber.isEmpty()) return null

        val stage = RepairStage.entries.find { it.name == cell(10).trim() } ?: RepairStage.DIAGNOSTIC
        val now = System.currentTimeMillis()
        val createdAt = cell(23).toLongOrNull() ?: now
        val updatedAt = cell(24).toLongOrNull() ?: 2000L
        val rawUuid = cell(27).trim()
        val needsUuidBackfill = rawUuid.isEmpty()
        val resolvedUuid = if (needsUuidBackfill) deterministicUuid(jobNumber, createdAt) else rawUuid

        val entity = JobCardEntity(
            recordUuid = resolvedUuid,
            jobNumber = jobNumber,
            customerName = cell(1),
            customerPhone = cell(2),
            deviceBrand = cell(3),
            deviceModel = cell(4),
            deviceColor = cell(5),
            imeiOrSerial = cell(6),
            securityLockCode = cell(7),
            issueDescription = cell(8),
            issueTagsCsv = cell(9),
            stage = stage,
            estimatedCost = cell(11).toDoubleOrNull() ?: 0.0,
            advancePaid = cell(12).toDoubleOrNull() ?: 0.0,
            discount = cell(13).toDoubleOrNull() ?: 0.0,
            finalCost = cell(14).toDoubleOrNull() ?: 0.0,
            isPaid = cell(15).equals("true", ignoreCase = true),
            paymentMode = cell(16).ifBlank { "Cash" },
            technicianNotes = cell(17),
            pickedUpByOriginalCustomer = !cell(18).equals("false", ignoreCase = true),
            recipientName = cell(19),
            recipientPhone = cell(20),
            checklistJson = cell(21),
            statusLogsJson = cell(22),
            createdAt = createdAt,
            updatedAt = updatedAt,
            deliveredAt = cell(25).toLongOrNull()
        )
        val isDeleted = cell(26).equals("true", ignoreCase = true)
        return SheetJobRow(
            job = entity,
            isDeleted = isDeleted,
            sheetRow = sheetRow,
            needsUuidBackfill = needsUuidBackfill
        )
    }

    private fun serializeProfileRow(profile: ShopProfileEntity): List<String> = listOf(
        profile.shopName,
        profile.tagline,
        profile.contactPhone,
        profile.whatsappNumber,
        profile.email,
        profile.address,
        profile.currencySymbol,
        profile.warrantyDays.toString(),
        profile.termsAndConditions,
        profile.nextJobNumberSequence.toString(),
        profile.preferredWhatsAppPackage,
        profile.updatedAt.toString(),
        profile.jobNumberPrefix,
        profile.lastJobMonth
    )

    private fun parseProfileRow(row: List<String>): ShopProfileEntity? {
        fun cell(idx: Int): String = row.getOrNull(idx).orEmpty()
        val name = cell(0).trim()
        if (name.isEmpty()) return null
        return ShopProfileEntity(
            id = 1,
            shopName = name,
            tagline = cell(1),
            contactPhone = cell(2),
            whatsappNumber = cell(3),
            email = cell(4),
            address = cell(5),
            currencySymbol = cell(6).ifBlank { "₹" },
            warrantyDays = cell(7).toIntOrNull()?.coerceAtLeast(1) ?: 90,
            termsAndConditions = cell(8),
            nextJobNumberSequence = cell(9).toIntOrNull() ?: 1,
            preferredWhatsAppPackage = cell(10).ifBlank { "auto" },
            updatedAt = cell(11).toLongOrNull() ?: 2000L,
            jobNumberPrefix = cell(12).trim().ifBlank { "JOB" },
            lastJobMonth = cell(13).trim()
        )
    }

    private data class SheetJobRow(
        val job: JobCardEntity,
        val isDeleted: Boolean,
        val sheetRow: Int,
        val needsUuidBackfill: Boolean
    )
}
