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
    private val database: AppDatabase,
    private val syncManager: GoogleSheetsSyncManager? = null
) {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        technicianNotes: String = ""
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
            statusLogsJson = Converters.serializeStatusLogs(
                listOf(
                    com.example.data.model.StatusLogEntry(
                        stage = RepairStage.DIAGNOSTIC,
                        timestamp = now,
                        note = "Job intake registered"
                    )
                )
            ),
            createdAt = now,
            updatedAt = now
        )

        val insertedId = jobCardDao.insertJob(job)
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
            "${existing.customerName} (Customer)"
        } else {
            "${recipientName.trim()} (Phone: ${recipientPhone.trim()})"
        }
        val paymentStatusLabel = if (isPaid) "Paid via $paymentMode" else "Payment Due"
        val logDetails = buildString {
            append("Handed over to: $recipientLabel • Final: $currency${String.format(java.util.Locale.US, "%.2f", effectiveTotal)} • Net: $currency${String.format(java.util.Locale.US, "%.2f", netCollectedOrDue)} ($paymentStatusLabel)")
            if (deliveryNote.isNotBlank()) {
                append(" • Note: ${deliveryNote.trim()}")
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
        jobCardDao.updateJob(job.copy(updatedAt = System.currentTimeMillis()))
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
                val chooser = Intent.createChooser(shareIntent, "Send via WhatsApp / Message").apply {
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

        return when (job.stage) {
            RepairStage.DIAGNOSTIC -> """
                *${shopName}* - Job Intake Confirmation
                
                Hello *${job.customerName}*,
                Your device *${job.deviceBrand} ${job.deviceModel}* has been booked for diagnosis.
                
                📋 *Job Card:* #${job.jobNumber}
                🔧 *Reported Issue:* ${job.issueDescription}
                💵 *Estimated Cost:* $currency$est
                💰 *Advance Paid:* $currency$advance
                
                We will update you as soon as our workbench diagnosis is completed.
                WhatsApp / Support: $shopContact
                Thank you for choosing ${shopName}!
            """.trimIndent()

            RepairStage.WAITING_FOR_PARTS -> """
                *${shopName}* - Repair Update
                
                Hello *${job.customerName}*,
                Update on your *${job.deviceBrand} ${job.deviceModel}* (Job: #${job.jobNumber}).
                We have ordered genuine replacement parts and will resume repair as soon as stock arrives.
                
                Contact us: $shopContact
            """.trimIndent()

            RepairStage.IN_PROGRESS -> """
                *${shopName}* - Repair In Progress
                
                Hello *${job.customerName}*,
                Our technician is currently working on your *${job.deviceBrand} ${job.deviceModel}* (Job: #${job.jobNumber}).
                Quality testing is underway!
            """.trimIndent()

            RepairStage.READY_FOR_PICKUP -> """
                *${shopName}* - Device Ready for Pickup! 📱✅
                
                Hello *${job.customerName}*,
                Good news! Your *${job.deviceBrand} ${job.deviceModel}* (Job: #${job.jobNumber}) is fully repaired, tested, and ready for pickup.
                
                💰 *Balance Due:* $currency$due
                📍 *Pickup Location:* ${profile.address}
                
                Please present this message or job number upon arrival.
            """.trimIndent()

            RepairStage.DELIVERED -> """
                *${shopName}* - Thank You!
                
                Hello *${job.customerName}*,
                Your *${job.deviceBrand} ${job.deviceModel}* has been handed over.
                🛡️ Replaced parts include our *${profile.warrantyDays}-day warranty*.
                
                Need help? Call / WhatsApp $shopContact. Have a wonderful day!
            """.trimIndent()

            RepairStage.CANCELLED -> """
                *${shopName}* - Job Notice
                
                Hello *${job.customerName}*,
                Repair for your *${job.deviceBrand} ${job.deviceModel}* (Job: #${job.jobNumber}) has been cancelled.
                Please collect your device from our counter at your convenience.
            """.trimIndent()
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
            """
            *${shopName}* - Khata / Payment Reminder

            Hello *${summary.customerName}*,
            This is a polite reminder regarding your repair account balance:

            $pendingJobsLines

            💰 *Total Pending Balance:* $currency$dueFormatted
            📞 *Shop Contact:* $shopContact

            Please let us know once paid. Thank you for visiting ${shopName}!
            """.trimIndent()
        } else {
            """
            *${shopName}* - Account Settled ✅

            Hello *${summary.customerName}*,
            Your repair Khata account with *${shopName}* has ₹0.00 pending balance across ${summary.totalJobsCount} visit(s).

            Thank you for choosing ${shopName}!
            """.trimIndent()
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
            "\n💬 WA: ${profile.whatsappNumber}"
        } else ""

        val deliveryInfo = if (job.stage == RepairStage.DELIVERED && job.deliveredAt != null) {
            val recipient = if (!job.pickedUpByOriginalCustomer && job.recipientName.isNotBlank()) {
                "${job.recipientName} (${job.recipientPhone})"
            } else {
                "${job.customerName} (Self)"
            }
            "\n*PICKED UP BY:* $recipient"
        } else ""

        return """
            ==============================
            *${profile.shopName.uppercase()}*
            ${profile.tagline}
            📞 Tel: ${profile.contactPhone}$waLine
            📍 ${profile.address}
            ==============================
            📋 *REPAIR JOB CARD: #${job.jobNumber}*
            📅 Date: $createdDate
            ------------------------------
            👤 *CUSTOMER:* ${job.customerName}
            📱 *PHONE:* ${job.customerPhone}
            🛠️ *DEVICE:* ${job.deviceBrand} ${job.deviceModel}
            🎨 *COLOR:* ${job.deviceColor.ifBlank { "N/A" }}
            🔢 *IMEI/SN:* ${job.imeiOrSerial.ifBlank { "N/A" }}
            ------------------------------
            ⚠️ *REPORTED ISSUE:*
            ${job.issueDescription}
            ------------------------------
            📊 *STATUS:* ${job.stage.title}
            💰 *ESTIMATED:* $cur$est
            💵 *ADVANCE PAID:* $cur$adv
            ${if (job.finalCost > 0.0) "🏷️ *FINAL COST:* $cur$fin\n" else ""}⚖️ *BALANCE DUE:* $cur$bal
            💳 *PAYMENT:* ${if (job.isPaid) "PAID IN FULL (${job.paymentMode})" else "BALANCE PENDING"}$deliveryInfo
            ==============================
            📜 *TERMS & CONDITIONS:*
            ${profile.termsAndConditions}
            ==============================
            _Thank you for choosing ${profile.shopName}!_
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
        return when {
            cleanDigits.startsWith("91") && cleanDigits.length == 12 -> cleanDigits
            cleanDigits.startsWith("0") && cleanDigits.length == 11 -> "91${cleanDigits.drop(1)}"
            cleanDigits.length == 10 -> "91$cleanDigits"
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
        val caption = "*${profile.shopName}* - Job Card Receipt #${job.jobNumber}\n" +
            "Customer: ${job.customerName} | Device: ${job.deviceBrand} ${job.deviceModel}"

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
            val chooser = Intent.createChooser(sendIntent, "Share Job Card PDF").apply {
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
            if (existingProfile.currencySymbol == "$" || existingProfile.currencySymbol.isBlank()) {
                shopProfileDao.updateProfile(existingProfile.copy(currencySymbol = "₹"))
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
