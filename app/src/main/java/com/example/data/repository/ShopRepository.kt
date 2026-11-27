package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.database.Converters
import com.example.data.entity.InventoryPartEntity
import com.example.data.entity.JobCardEntity
import com.example.data.entity.ShopProfileEntity
import com.example.data.model.ConditionStatus
import com.example.data.model.CustomerKhataSummary
import com.example.data.model.IntakeChecklist
import com.example.data.model.RepairStage
import com.example.data.model.normalizeKhataKey
import com.example.data.remote.AppUpdateChecker
import com.example.data.remote.AppUpdateResult
import com.example.data.remote.AuthTokenOutcome
import com.example.data.remote.GoogleSheetsSyncManager
import com.example.data.remote.GoogleSyncConfig
import com.example.data.remote.SyncSummary
import com.example.data.remote.VersionCheckService
import com.example.util.LanguageManager
import com.example.util.PickupReminders
import com.example.util.ReceiptPdfGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShopRepository(
    private val appContext: Context,
    private val database: AppDatabase,
    private val syncManager: GoogleSheetsSyncManager? = null
) {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Text in the language chosen in Settings. */
    private fun tr(@androidx.annotation.StringRes id: Int, vararg args: Any): String =
        LanguageManager.getString(appContext, id, *args)

    private val jobCardDao = database.jobCardDao()
    private val inventoryDao = database.inventoryDao()
    private val shopProfileDao = database.shopProfileDao()

    val allJobs: Flow<List<JobCardEntity>> = jobCardDao.getAllJobs()
    val allParts: Flow<List<InventoryPartEntity>> = inventoryDao.getAllParts()
    val lowStockParts: Flow<List<InventoryPartEntity>> = inventoryDao.getLowStockParts()
    val shopProfile: Flow<ShopProfileEntity?> = shopProfileDao.getShopProfile()
    val googleSyncConfig: Flow<GoogleSyncConfig> = syncManager?.syncConfig ?: emptyFlow()

    private fun scheduleBackgroundAutoSync() {
        val manager = syncManager ?: return
        syncScope.launch {
            try {
                manager.tryBackgroundAutoSync()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun tryBackgroundAutoSync(): Boolean {
        return try {
            syncManager?.tryBackgroundAutoSync() ?: false
        } catch (_: Exception) {
            false
        }
    }

    suspend fun requestGoogleAuth(activityContext: Context? = null): AuthTokenOutcome? =
        syncManager?.let {
            if (activityContext != null) it.requestAuthorization(activityContext) else it.requestAuthorization()
        }

    fun extractGoogleToken(data: Intent?, activityContext: Context? = null): String? =
        syncManager?.let {
            if (activityContext != null) it.extractTokenFromIntent(data, activityContext) else it.extractTokenFromIntent(data)
        }

    suspend fun syncWithGoogleToken(accessToken: String): SyncSummary? = syncManager?.syncWithToken(accessToken)

    suspend fun saveCustomSpreadsheetId(rawUrlOrId: String) {
        syncManager?.saveCustomSpreadsheetId(rawUrlOrId)
    }

    suspend fun disconnectGoogleSheets() {
        syncManager?.clearConnection()
    }

    suspend fun runFullDriveBackupNow(
        accessToken: String,
        forceNewSnapshot: Boolean = true
    ): String? = syncManager?.createDatedFullBackup(accessToken, forceNewSnapshot)

    suspend fun setNightlyBackupEnabled(enabled: Boolean) {
        syncManager?.setNightlyBackupEnabled(enabled)
    }

    fun getJobById(id: Long): Flow<JobCardEntity?> = jobCardDao.getJobById(id)

    fun searchJobs(query: String): Flow<List<JobCardEntity>> {
        return if (query.isBlank()) {
            jobCardDao.getAllJobs()
        } else {
            jobCardDao.searchJobs(query.trim())
        }
    }

    private fun formatJobNumber(prefix: String, yymmdd: String, seq: Int): String {
        val cleanPrefix = prefix.trim()
            .replace(Regex("\\s+"), "-")
            .trim('-')
            .ifBlank { "JOB" }
        return "$cleanPrefix-$yymmdd-${String.format(Locale.US, "%03d", seq)}"
    }

    private fun collectedOn(job: JobCardEntity?): Double =
        when {
            job == null -> 0.0
            job.isPaid -> repairRevenue(job)
            else -> job.advancePaid
        }

    /** Writes the change in money collected on a job (advance, payment, delivery) to the cash register. */
    private suspend fun recordCashDelta(before: JobCardEntity?, after: JobCardEntity, mode: String) {
        val delta = collectedOn(after) - collectedOn(before)
        if (kotlin.math.abs(delta) < 0.005) return
        database.storeDao().insertMovement(
            com.example.data.entity.CashMovementEntity(
                type = "REPAIR",
                amount = delta,
                note = "#${after.jobNumber}",
                jobId = after.id,
                paymentMode = mode.ifBlank { "Cash" }
            )
        )
    }

    suspend fun createJob(
        customerName: String,
        customerPhone: String,
        deviceBrand: String,
        deviceModel: String,
        deviceColor: String,
        imeiOrSerial: String,
        securityLockCode: String,
        issueDescription: String,
        issueTags: List<String>,
        checklist: IntakeChecklist,
        estimatedCost: Double,
        advancePaid: Double,
        technicianNotes: String = "",
        damageMarks: String = "",
        signatureData: String = ""
    ): Long = withContext(Dispatchers.IO) {
        var profile = shopProfileDao.getShopProfileSync()
        if (profile == null) {
            profile = ShopProfileEntity(id = 1)
            shopProfileDao.insertProfile(profile)
        }

        val now = System.currentTimeMillis()
        val currentMonth = SimpleDateFormat("yyMM", Locale.US).format(Date(now))
        val todayYymmdd = SimpleDateFormat("yyMMdd", Locale.US).format(Date(now))

        var seq = if (profile.lastJobMonth == currentMonth && profile.nextJobNumberSequence >= 1) {
            profile.nextJobNumberSequence
        } else {
            1
        }
        var jobNumber = formatJobNumber(profile.jobNumberPrefix, todayYymmdd, seq)
        while (jobCardDao.getJobByNumberSync(jobNumber) != null) {
            seq++
            jobNumber = formatJobNumber(profile.jobNumberPrefix, todayYymmdd, seq)
        }

        val job = JobCardEntity(
            recordUuid = java.util.UUID.randomUUID().toString(),
            jobNumber = jobNumber,
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            deviceBrand = deviceBrand.trim(),
            deviceModel = deviceModel.trim(),
            deviceColor = deviceColor.trim(),
            imeiOrSerial = imeiOrSerial.trim(),
            securityLockCode = securityLockCode.trim(),
            issueDescription = issueDescription.trim(),
            issueTagsCsv = issueTags.joinToString(","),
            checklistJson = Converters.serializeChecklist(checklist),
            stage = RepairStage.DIAGNOSTIC,
            estimatedCost = estimatedCost,
            advancePaid = advancePaid,
            technicianNotes = technicianNotes.trim(),
            damageMarks = damageMarks,
            signatureData = signatureData,
            statusLogsJson = Converters.serializeStatusLogs(
                listOf(
                    com.example.data.model.StatusLogEntry(
                        stage = RepairStage.DIAGNOSTIC,
                        timestamp = now,
                        note = tr(R.string.log_job_intake)
                    )
                )
            ),
            createdAt = now,
            updatedAt = now
        )

        val insertedId = jobCardDao.insertJob(job)
        recordCashDelta(null, job.copy(id = insertedId), job.paymentMode)
        shopProfileDao.updateMonthlyJobSequence(
            nextSequence = seq + 1,
            monthKey = currentMonth,
            updatedAt = now
        )
        scheduleBackgroundAutoSync()
        insertedId
    }

    suspend fun updateJobStage(jobId: Long, newStage: RepairStage, note: String = "") = withContext(Dispatchers.IO) {
        val existing = jobCardDao.getJobByIdSync(jobId) ?: return@withContext
        val deliveredTimestamp = if (newStage == RepairStage.DELIVERED) System.currentTimeMillis() else null
        val currentLogs = existing.statusLogs.toMutableList()
        currentLogs.add(
            com.example.data.model.StatusLogEntry(
                stage = newStage,
                timestamp = System.currentTimeMillis(),
                note = note.trim()
            )
        )
        val updated = existing.copy(
            stage = newStage,
            deliveredAt = deliveredTimestamp,
            statusLogsJson = Converters.serializeStatusLogs(currentLogs),
            updatedAt = System.currentTimeMillis()
        )
        jobCardDao.updateJob(updated)
        scheduleBackgroundAutoSync()
    }

    suspend fun completeJobAndDeliver(
        jobId: Long,
        finalCost: Double,
        discount: Double,
        isPaid: Boolean,
        paymentMode: String,
        pickedUpByOriginalCustomer: Boolean,
        recipientName: String,
        recipientPhone: String,
        deliveryNote: String = ""
    ) = withContext(Dispatchers.IO) {
        val existing = jobCardDao.getJobByIdSync(jobId) ?: return@withContext
        val profile = shopProfileDao.getShopProfileSync()
        val currency = profile?.currencySymbol?.ifBlank { "₹" } ?: "₹"
        val now = System.currentTimeMillis()
        val currentLogs = existing.statusLogs.toMutableList()

        val effectiveTotal = if (finalCost > 0.0) finalCost else existing.estimatedCost
        val netCollectedOrDue = (effectiveTotal - discount - existing.advancePaid).coerceAtLeast(0.0)
        val recipientLabel = if (pickedUpByOriginalCustomer) {
            tr(R.string.log_customer_suffix, existing.customerName)
        } else {
            tr(R.string.log_recipient, recipientName.trim(), recipientPhone.trim())
        }
        val paymentStatusLabel = if (isPaid) tr(R.string.log_paid_via, paymentMode) else tr(R.string.log_payment_due)
        val logDetails = buildString {
            append(
                tr(
                    R.string.log_handover,
                    recipientLabel,
                    "$currency${String.format(java.util.Locale.US, "%.2f", effectiveTotal)}",
                    "$currency${String.format(java.util.Locale.US, "%.2f", netCollectedOrDue)}",
                    paymentStatusLabel
                )
            )
            if (deliveryNote.isNotBlank()) {
                append(" • ").append(tr(R.string.log_handover_note, deliveryNote.trim()))
            }
        }

        currentLogs.add(
            com.example.data.model.StatusLogEntry(
                stage = RepairStage.DELIVERED,
                timestamp = now,
                note = logDetails
            )
        )

        val updated = existing.copy(
            stage = RepairStage.DELIVERED,
            finalCost = finalCost,
            discount = discount,
            isPaid = isPaid,
            paymentMode = paymentMode,
            pickedUpByOriginalCustomer = pickedUpByOriginalCustomer,
            recipientName = if (pickedUpByOriginalCustomer) "" else recipientName.trim(),
            recipientPhone = if (pickedUpByOriginalCustomer) "" else recipientPhone.trim(),
            deliveredAt = now,
            statusLogsJson = Converters.serializeStatusLogs(currentLogs),
            updatedAt = now
        )
        jobCardDao.updateJob(updated)
        recordCashDelta(existing, updated, paymentMode)
        scheduleBackgroundAutoSync()
    }

    suspend fun updateJobPayment(
        jobId: Long,
        finalCost: Double,
        discount: Double,
        isPaid: Boolean,
        paymentMode: String
    ) = withContext(Dispatchers.IO) {
        val existing = jobCardDao.getJobByIdSync(jobId) ?: return@withContext
        val updated = existing.copy(
            finalCost = finalCost,
            discount = discount,
            isPaid = isPaid,
            paymentMode = paymentMode,
            updatedAt = System.currentTimeMillis()
        )
        jobCardDao.updateJob(updated)
        recordCashDelta(existing, updated, paymentMode)
        scheduleBackgroundAutoSync()
    }

    suspend fun recordCustomerKhataPayment(
        customerKey: String,
        amount: Double,
        paymentMode: String,
        note: String = ""
    ): Int = withContext(Dispatchers.IO) {
        if (amount <= 0.0) return@withContext 0
        val profile = shopProfileDao.getShopProfileSync()
        val currency = profile?.currencySymbol?.ifBlank { "₹" } ?: "₹"
        val now = System.currentTimeMillis()

        val unpaidJobs = jobCardDao.getAllJobsSync()
            .filter {
                normalizeKhataKey(it.customerName, it.customerPhone) == customerKey &&
                    it.stage != RepairStage.CANCELLED &&
                    !it.isPaid &&
                    it.balanceDue > 0.01
            }
            .sortedBy { it.createdAt }

        var remaining = amount
        var updatedCount = 0

        for (job in unpaidJobs) {
            if (remaining <= 0.001) break
            val due = job.balanceDue
            val effectiveTotal = if (job.finalCost > 0.0) job.finalCost else job.estimatedCost
            val applied = minOf(remaining, due)
            val newAdvance = job.advancePaid + applied
            val newDue = (effectiveTotal - job.discount - newAdvance).coerceAtLeast(0.0)
            val settled = newDue <= 0.01

            val logs = job.statusLogs.toMutableList()
            val extraNote = if (note.isNotBlank()) " • ${note.trim()}" else ""
            logs.add(
                com.example.data.model.StatusLogEntry(
                    stage = job.stage,
                    timestamp = now,
                    note = "Khata payment received: $currency${String.format(Locale.US, "%.2f", applied)} via $paymentMode$extraNote"
                )
            )

            val updatedJob = job.copy(
                finalCost = effectiveTotal,
                advancePaid = newAdvance,
                isPaid = settled,
                paymentMode = paymentMode,
                statusLogsJson = Converters.serializeStatusLogs(logs),
                updatedAt = now
            )
            jobCardDao.updateJob(updatedJob)
            recordCashDelta(job, updatedJob, paymentMode)
            remaining -= applied
            updatedCount++
        }

        if (updatedCount > 0) {
            scheduleBackgroundAutoSync()
        }
        updatedCount
    }

    suspend fun updateJobNotes(jobId: Long, notes: String) = withContext(Dispatchers.IO) {
        val existing = jobCardDao.getJobByIdSync(jobId) ?: return@withContext
        val updated = existing.copy(
            technicianNotes = notes,
            updatedAt = System.currentTimeMillis()
        )
        jobCardDao.updateJob(updated)
        scheduleBackgroundAutoSync()
    }

    suspend fun updateJobCard(job: JobCardEntity) = withContext(Dispatchers.IO) {
        val before = jobCardDao.getJobByIdSync(job.id)
        val after = job.copy(updatedAt = System.currentTimeMillis())
        jobCardDao.updateJob(after)
        recordCashDelta(before, after, after.paymentMode)
        scheduleBackgroundAutoSync()
    }

    suspend fun deleteJob(jobId: Long) = withContext(Dispatchers.IO) {
        val existing = jobCardDao.getJobByIdSync(jobId)
        jobCardDao.deleteJobById(jobId)
        existing?.let {
            val tombstoneKey = it.recordUuid.ifBlank { it.jobNumber }
            syncManager?.markJobDeletedForSync(tombstoneKey)
        }
        scheduleBackgroundAutoSync()
    }

    // --- Inventory Operations ---
    suspend fun addInventoryPart(part: InventoryPartEntity): Long = withContext(Dispatchers.IO) {
        inventoryDao.insertPart(part)
    }

    suspend fun updateInventoryPart(part: InventoryPartEntity) = withContext(Dispatchers.IO) {
        inventoryDao.updatePart(part.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteInventoryPart(part: InventoryPartEntity) = withContext(Dispatchers.IO) {
        inventoryDao.deletePart(part)
    }

    suspend fun adjustPartStock(partId: Long, delta: Int) = withContext(Dispatchers.IO) {
        val part = inventoryDao.getPartById(partId) ?: return@withContext
        val newQty = (part.quantityInStock + delta).coerceAtLeast(0)
        inventoryDao.updatePart(part.copy(quantityInStock = newQty, updatedAt = System.currentTimeMillis()))
    }

    // --- Shop Profile Operations ---
    suspend fun updateShopProfile(profile: ShopProfileEntity) = withContext(Dispatchers.IO) {
        shopProfileDao.updateProfile(profile.copy(updatedAt = System.currentTimeMillis()))
        scheduleBackgroundAutoSync()
    }

    // --- Customer Communications (WhatsApp / SMS / Dial) ---
    fun createWhatsAppIntent(job: JobCardEntity, profile: ShopProfileEntity, customMessage: String? = null): Intent {
        val message = customMessage ?: generateWhatsAppTemplate(job, profile)
        val cleanPhone = job.customerPhone.replace("[^0-9+]".toRegex(), "")
        val encodedMessage = try {
            URLEncoder.encode(message, "UTF-8")
        } catch (e: Exception) {
            message
        }

        val uri = if (cleanPhone.isNotEmpty()) {
            Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage")
        } else {
            Uri.parse("https://api.whatsapp.com/send?text=$encodedMessage")
        }

        return Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun launchWhatsAppSafely(
        context: Context,
        job: JobCardEntity,
        customMessage: String,
        targetPackage: String? = null
    ) {
        val formattedPhone = formatWhatsAppPhoneDigits(job.customerPhone)

        val encodedMessage = try {
            URLEncoder.encode(customMessage, "UTF-8")
        } catch (e: Exception) {
            Uri.encode(customMessage)
        }

        val waUri = if (formattedPhone.isNotEmpty()) {
            Uri.parse("whatsapp://send?phone=$formattedPhone&text=$encodedMessage")
        } else {
            Uri.parse("whatsapp://send?text=$encodedMessage")
        }

        val isExplicitPackage = !targetPackage.isNullOrBlank() && targetPackage != "auto"
        var launched = false

        // 1. Explicit target package requested (e.g. com.whatsapp or com.whatsapp.w4b)
        if (isExplicitPackage) {
            if (!isAppInstalled(context, targetPackage!!)) {
                Toast.makeText(context, context.getString(R.string.whatsapp_not_installed), Toast.LENGTH_SHORT).show()
                return
            }
            try {
                val waIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
                    setPackage(targetPackage)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(waIntent)
                launched = true
            } catch (_: Exception) {
                Toast.makeText(context, context.getString(R.string.whatsapp_not_installed), Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 2. Try WhatsApp standard application com.whatsapp if auto mode
        if (!launched) {
            try {
                val waIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
                    setPackage("com.whatsapp")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(waIntent)
                launched = true
            } catch (_: Exception) {}
        }

        // 3. Try WhatsApp Business application com.whatsapp.w4b if not launched yet
        if (!launched) {
            try {
                val w4bIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
                    setPackage("com.whatsapp.w4b")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(w4bIntent)
                launched = true
            } catch (_: Exception) {}
        }

        // 4. Try generic whatsapp://send scheme directly
        if (!launched) {
            try {
                val schemeIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(schemeIntent)
                launched = true
            } catch (_: Exception) {}
        }

        // 5. Try browser API https://api.whatsapp.com/send
        if (!launched) {
            try {
                val webUri = if (formattedPhone.isNotEmpty()) {
                    Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMessage")
                } else {
                    Uri.parse("https://api.whatsapp.com/send?text=$encodedMessage")
                }
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                launched = true
            } catch (_: Exception) {}
        }

        // 6. Universal fallback: Android Share Sheet
        if (!launched) {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, customMessage)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val chooser = Intent.createChooser(shareIntent, tr(R.string.wa_chooser_send)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                Toast.makeText(context, "Communication error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun generateWhatsAppTemplate(job: JobCardEntity, profile: ShopProfileEntity): String {
        val currency = profile.currencySymbol
        val shopName = profile.shopName
        val shopContact = profile.whatsappNumber.ifBlank { profile.contactPhone }
        val due = String.format("%.2f", job.balanceDue)
        val advance = String.format("%.2f", job.advancePaid)
        val est = String.format("%.2f", job.estimatedCost)

        val device = "${job.deviceBrand} ${job.deviceModel}"
        return when (job.stage) {
            RepairStage.DIAGNOSTIC -> tr(
                R.string.wa_intake, shopName, job.customerName, device, job.jobNumber,
                job.issueDescription, "$currency$est", "$currency$advance", shopContact
            )
            RepairStage.WAITING_FOR_PARTS -> tr(
                R.string.wa_waiting_parts, shopName, job.customerName, device, job.jobNumber, shopContact
            )
            RepairStage.IN_PROGRESS -> tr(
                R.string.wa_in_progress, shopName, job.customerName, device, job.jobNumber
            )
            RepairStage.READY_FOR_PICKUP -> tr(
                R.string.wa_ready, shopName, job.customerName, device, job.jobNumber,
                "$currency$due", profile.address, PickupReminders.formatDeadline(appContext, job)
            )
            RepairStage.DELIVERED -> tr(
                R.string.wa_delivered, shopName, job.customerName, device, profile.warrantyDays, shopContact
            )
            RepairStage.CANCELLED -> tr(
                R.string.wa_cancelled, shopName, job.customerName, device, job.jobNumber
            )
        }
    }

    fun generateKhataReminderText(summary: CustomerKhataSummary, profile: ShopProfileEntity): String {
        val currency = profile.currencySymbol.ifBlank { "₹" }
        val shopName = profile.shopName
        val shopContact = profile.whatsappNumber.ifBlank { profile.contactPhone }
        val pendingJobsLines = summary.jobs
            .filter { it.stage != RepairStage.CANCELLED && !it.isPaid && it.balanceDue > 0.01 }
            .joinToString("\n") { job ->
                "• ${job.jobNumber} (${job.deviceBrand} ${job.deviceModel}): $currency${String.format(Locale.US, "%.2f", job.balanceDue)}"
            }
        val dueFormatted = String.format(Locale.US, "%.2f", summary.totalPendingDue)

        return if (summary.totalPendingDue > 0.01) {
            tr(
                R.string.wa_khata_due, shopName, summary.customerName, pendingJobsLines,
                "$currency$dueFormatted", shopContact
            )
        } else {
            tr(
                R.string.wa_khata_settled, shopName, summary.customerName,
                summary.totalJobsCount, "${currency}0.00"
            )
        }
    }

    fun createDialIntent(phoneNumber: String): Intent {
        val cleanPhone = phoneNumber.replace("[^0-9+]".toRegex(), "")
        return Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun generateJobReceiptText(job: JobCardEntity, profile: ShopProfileEntity): String {
        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
        val createdDate = dateFormat.format(java.util.Date(job.createdAt))
        val est = String.format("%.2f", job.estimatedCost)
        val adv = String.format("%.2f", job.advancePaid)
        val bal = String.format("%.2f", job.balanceDue)
        val fin = String.format("%.2f", job.finalCost)
        val cur = profile.currencySymbol
        val waLine = if (profile.whatsappNumber.isNotBlank() && profile.whatsappNumber != profile.contactPhone) {
            "\n💬 ${tr(R.string.rt_wa)}: ${profile.whatsappNumber}"
        } else ""

        val deliveryInfo = if (job.stage == RepairStage.DELIVERED && job.deliveredAt != null) {
            val recipient = if (!job.pickedUpByOriginalCustomer && job.recipientName.isNotBlank()) {
                "${job.recipientName} (${job.recipientPhone})"
            } else {
                "${job.customerName} (${tr(R.string.rt_self)})"
            }
            "\n*${tr(R.string.rt_picked_up_by)}:* $recipient"
        } else ""

        return """
            ==============================
            *${profile.shopName.uppercase()}*
            ${profile.tagline}
            📞 ${tr(R.string.rt_tel)}: ${profile.contactPhone}$waLine
            📍 ${profile.address}
            ==============================
            📋 *${tr(R.string.rt_job_card, job.jobNumber)}*
            📅 ${tr(R.string.rt_date)}: $createdDate
            ------------------------------
            👤 *${tr(R.string.rt_customer)}:* ${job.customerName}
            📱 *${tr(R.string.rt_phone)}:* ${job.customerPhone}
            🛠️ *${tr(R.string.rt_device)}:* ${job.deviceBrand} ${job.deviceModel}
            🎨 *${tr(R.string.rt_color)}:* ${job.deviceColor.ifBlank { tr(R.string.rt_na) }}
            🔢 *${tr(R.string.rt_imei)}:* ${job.imeiOrSerial.ifBlank { tr(R.string.rt_na) }}
            ------------------------------
            ⚠️ *${tr(R.string.rt_reported_issue)}:*
            ${job.issueDescription}
            ------------------------------
            📊 *${tr(R.string.rt_status)}:* ${tr(com.example.util.Labels.stageRes(job.stage))}
            💰 *${tr(R.string.rt_estimated)}:* $cur$est
            💵 *${tr(R.string.rt_advance)}:* $cur$adv
            ${if (job.finalCost > 0.0) "🏷️ *${tr(R.string.rt_final)}:* $cur$fin\n" else ""}⚖️ *${tr(R.string.rt_balance_due)}:* $cur$bal
            💳 *${tr(R.string.rt_payment)}:* ${if (job.isPaid) tr(R.string.rt_paid_full, job.paymentMode) else tr(R.string.rt_balance_pending)}$deliveryInfo
            ==============================
            📜 *${tr(R.string.rt_terms)}:*
            ${profile.termsAndConditions}
            ==============================
            _${tr(R.string.rt_thanks, profile.shopName)}_
        """.trimIndent()
    }

    fun shareJobReceiptToWhatsApp(
        context: Context,
        job: JobCardEntity,
        profile: ShopProfileEntity,
        targetPackage: String? = null
    ) {
        val receiptText = generateJobReceiptText(job, profile)
        launchWhatsAppSafely(context, job, receiptText, targetPackage)
    }

    suspend fun generateReceiptPdfFile(
        context: Context,
        job: JobCardEntity,
        profile: ShopProfileEntity
    ): File = withContext(Dispatchers.IO) {
        ReceiptPdfGenerator.generateJobReceiptPdf(context, job, profile)
    }

    fun getReceiptPdfUri(context: Context, pdfFile: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
    }

    private fun formatWhatsAppPhoneDigits(rawPhone: String): String {
        val cleanDigits = rawPhone.filter { it.isDigit() }
        val cc = LanguageManager.getCountryCode(appContext)
        if (cc.isEmpty()) return cleanDigits
        return when {
            cleanDigits.length == 10 -> "$cc$cleanDigits"
            cleanDigits.startsWith("0") && cleanDigits.length == 11 -> "$cc${cleanDigits.drop(1)}"
            else -> cleanDigits
        }
    }

    fun shareReceiptPdfToWhatsApp(
        context: Context,
        pdfUri: Uri,
        job: JobCardEntity,
        profile: ShopProfileEntity,
        targetPackage: String? = null
    ): Boolean {
        val formattedPhone = formatWhatsAppPhoneDigits(job.customerPhone)
        val caption = tr(
            R.string.wa_receipt_caption, profile.shopName, job.jobNumber,
            job.customerName, "${job.deviceBrand} ${job.deviceModel}"
        )

        fun buildSendIntent(pkg: String?): Intent {
            return Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                putExtra(Intent.EXTRA_TEXT, caption)
                if (formattedPhone.isNotEmpty()) {
                    putExtra("jid", "$formattedPhone@s.whatsapp.net")
                }
                if (!pkg.isNullOrBlank()) {
                    setPackage(pkg)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        // Explicitly grant URI read access to target WhatsApp packages
        listOf("com.whatsapp", "com.whatsapp.w4b").forEach { pkg ->
            try {
                context.grantUriPermission(pkg, pdfUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
        }

        val isExplicitPackage = !targetPackage.isNullOrBlank() && targetPackage != "auto"

        // 1. Explicit target package requested
        if (isExplicitPackage) {
            if (!isAppInstalled(context, targetPackage!!)) {
                Toast.makeText(context, context.getString(R.string.whatsapp_not_installed), Toast.LENGTH_SHORT).show()
                return false
            }
            return try {
                context.startActivity(buildSendIntent(targetPackage))
                true
            } catch (_: Exception) {
                Toast.makeText(context, context.getString(R.string.whatsapp_not_installed), Toast.LENGTH_SHORT).show()
                false
            }
        }

        var launched = false

        // 2. Try standard WhatsApp in auto mode
        try {
            context.startActivity(buildSendIntent("com.whatsapp"))
            launched = true
        } catch (_: Exception) {
        }

        // 3. Fallback to WhatsApp Business in auto mode
        if (!launched) {
            try {
                context.startActivity(buildSendIntent("com.whatsapp.w4b"))
                launched = true
            } catch (_: Exception) {
            }
        }

        // 4. Universal fallback: Android PDF share chooser in auto mode
        if (!launched) {
            shareReceiptPdfGeneral(context, pdfUri, job)
            launched = true
        }
        return launched
    }

    fun shareReceiptPdfGeneral(
        context: Context,
        pdfUri: Uri,
        job: JobCardEntity
    ) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                putExtra(Intent.EXTRA_SUBJECT, "Repair Job Card #${job.jobNumber}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, tr(R.string.wa_chooser_share_pdf)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "PDF share error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun printReceiptPdf(
        context: Context,
        pdfFile: File,
        job: JobCardEntity
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            val pdfUri = getReceiptPdfUri(context, pdfFile)
            shareReceiptPdfGeneral(context, pdfUri, job)
            return
        }

        val jobName = "JobCard_${job.jobNumber}"
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (destination == null) {
                    callback?.onWriteFailed("Missing destination file descriptor")
                    return
                }
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onWriteCancelled()
                    } else {
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    }
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, adapter, null)
    }

    // --- Clean Initial Setup (No Demo Data) ---
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingProfile = shopProfileDao.getShopProfileSync()
        if (existingProfile != null) {
            // Keep the currency the user chose. Only fill it in when it is empty.
            if (existingProfile.currencySymbol.isBlank()) {
                shopProfileDao.updateProfile(existingProfile.copy(currencySymbol = "$"))
            }
            return@withContext
        }

        val defaultProfile = ShopProfileEntity(
            id = 1,
            shopName = "Mobile Repair Shop",
            tagline = "",
            contactPhone = "",
            whatsappNumber = "",
            email = "",
            address = "",
            currencySymbol = "₹",
            warrantyDays = 90,
            termsAndConditions = "",
            nextJobNumberSequence = 1,
            updatedAt = 0L
        )
        shopProfileDao.insertProfile(defaultProfile)
    }

    // --- GitHub version.json & Release Version Check ---
    suspend fun checkAppUpdate(
        repoSlug: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): AppUpdateResult {
        return VersionCheckService.checkLatestVersion(
            repoSlug = repoSlug,
            currentVersionCode = currentVersionCode,
            currentVersionName = currentVersionName
        )
    }
}
