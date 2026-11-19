package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import com.example.data.model.StatusLogEntry
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.database.Converters
import com.example.data.entity.JobCardEntity
import com.example.data.model.RepairStage
import com.example.ui.AppScreen
import com.example.ui.ShopViewModel
import com.example.ui.components.AppButton
import com.example.ui.components.AppCard
import com.example.ui.components.AppTextField
import com.example.ui.components.ChecklistSummaryViewer
import com.example.ui.components.RepairStatusBadge
import com.example.ui.theme.ShopTheme
import com.example.ui.theme.StatusAlertCrimson
import com.example.ui.theme.StatusPendingAmber
import com.example.ui.theme.StatusRepairedEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allJobs by viewModel.allJobs.collectAsStateWithLifecycle()
    val job = allJobs.find { it.id == viewModel.selectedJobId } ?: viewModel.selectedJob
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    var showEditJobDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showStageSelectDialog by remember { mutableStateOf(false) }

    var editFinalCost by remember(job?.id, viewModel.showPaymentDialog) {
        val initial = if ((job?.finalCost ?: 0.0) > 0.0) job!!.finalCost else (job?.estimatedCost ?: 0.0)
        mutableStateOf(formatDecimalForInput(initial))
    }
    var editDiscount by remember(job?.id, viewModel.showPaymentDialog) {
        mutableStateOf(formatDecimalForInput(job?.discount ?: 0.0))
    }
    var editIsPaid by remember(job?.id, viewModel.showPaymentDialog) {
        mutableStateOf(job?.isPaid ?: false)
    }
    var editPaymentMode by remember(job?.id, viewModel.showPaymentDialog) {
        mutableStateOf(job?.paymentMode?.ifBlank { "Cash" } ?: "Cash")
    }

    var techNotesText by remember(job?.id, job?.technicianNotes) { mutableStateOf(job?.technicianNotes ?: "") }

    BackHandler {
        viewModel.navigateBack()
    }

    if (job == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.empty_jobs_title), style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    val checklist = remember(job.checklistJson) {
        Converters.deserializeChecklist(job.checklistJson)
    }

    val shopColors = ShopTheme.colors
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "#${job.jobNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            color = shopColors.topBarContent
                        )
                        Text(
                            text = "${job.deviceBrand} ${job.deviceModel}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = shopColors.topBarContent.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                            .testTag("job_detail_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = shopColors.topBarContent
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.prepareWhatsAppMessage(job) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                            .testTag("job_detail_whatsapp_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = stringResource(R.string.btn_whatsapp),
                            tint = shopColors.topBarContent
                        )
                    }
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.RECEIPT_PREVIEW) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                            .testTag("job_detail_receipt_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = stringResource(R.string.btn_print_receipt),
                            tint = shopColors.topBarContent
                        )
                    }
                    IconButton(
                        onClick = { showEditJobDialog = true },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                            .testTag("job_detail_edit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.btn_edit_job),
                            tint = shopColors.topBarContent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = shopColors.topBarContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp), // Tablet and counter layout spec
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 1: Stage Lifecycle
                item {
                    AppCard(testTag = "card_stage_lifecycle") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.job_details_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                RepairStatusBadge(stage = job.stage)
                                Spacer(modifier = Modifier.height(4.dp))
                                val updatedDateFormatted = remember(job.updatedAt) {
                                    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                    sdf.format(Date(job.updatedAt))
                                }
                                Text(
                                    text = "${stringResource(R.string.status_last_updated)}: $updatedDateFormatted",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { showStageSelectDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("change_stage_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_update_stage), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }

                // Card: Handover & Delivery Details (Shown when job is delivered)
                if (job.stage == RepairStage.DELIVERED) {
                    item {
                        AppCard(testTag = "card_handover_details") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = StatusRepairedEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.card_handover_details_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Surface(
                                    color = if (job.isPaid) shopColors.repairedContainer else shopColors.pendingContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(if (job.isPaid) R.string.payment_paid else R.string.payment_due),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (job.isPaid) shopColors.repaired else shopColors.pending,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val deliveredDateFormatted = remember(job.deliveredAt, job.updatedAt) {
                                val t = job.deliveredAt ?: job.updatedAt
                                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(t))
                            }
                            DetailRow(
                                label = stringResource(R.string.handover_delivered_on),
                                value = deliveredDateFormatted
                            )

                            val recipientLabel = if (job.pickedUpByOriginalCustomer) {
                                "${stringResource(R.string.handover_by_self)} (${job.customerName})"
                            } else {
                                "${stringResource(R.string.handover_by_other)}: ${job.recipientName}"
                            }
                            DetailRow(
                                label = stringResource(R.string.handover_delivered_to),
                                value = recipientLabel,
                                highlightColor = MaterialTheme.colorScheme.primary
                            )

                            if (!job.pickedUpByOriginalCustomer && job.recipientPhone.isNotBlank()) {
                                DetailRow(
                                    label = stringResource(R.string.handover_recipient_contact),
                                    value = job.recipientPhone
                                )
                            }

                            val totalCharge = if (job.finalCost > 0) job.finalCost else job.estimatedCost
                            DetailRow(
                                label = stringResource(R.string.label_final_charge),
                                value = "${shopProfile.currencySymbol}${String.format(Locale.getDefault(), "%.2f", totalCharge)} (${job.paymentMode})"
                            )
                        }
                    }
                }

                // Card: Status Activity Logs (Date & Time with Real-Time Updates)
                item {
                    AppCard(testTag = "card_status_activity_logs") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.status_logs_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${job.statusLogs.size} ${stringResource(R.string.status_logs_count)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (job.statusLogs.isEmpty()) {
                            Text(
                                text = stringResource(R.string.status_logs_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                job.statusLogs.reversed().forEachIndexed { index, logEntry ->
                                    StatusLogTimelineItem(
                                        log = logEntry,
                                        isLatest = index == 0
                                    )
                                }
                            }
                        }
                    }
                }

                // Card 2: Customer Contact
                item {
                    AppCard(testTag = "card_customer_detail") {
                        Text(
                            text = stringResource(R.string.section_customer_info),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = job.customerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = job.customerPhone,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.callCustomer(context, job.customerPhone) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("detail_call_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_call_customer), style = MaterialTheme.typography.bodyMedium)
                                }

                                Button(
                                    onClick = { viewModel.prepareWhatsAppMessage(job) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusRepairedEmerald),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("detail_whatsapp_btn")
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_whatsapp), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                // Card 3: Device Specs & Lock Security
                item {
                    AppCard(testTag = "card_device_detail") {
                        Text(
                            text = stringResource(R.string.section_device_specs),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        DetailRow(stringResource(R.string.label_device_brand), "${job.deviceBrand} ${job.deviceModel}")
                        if (job.deviceColor.isNotBlank()) {
                            DetailRow(stringResource(R.string.label_device_color), job.deviceColor)
                        }
                        if (job.imeiOrSerial.isNotBlank()) {
                            DetailRow(stringResource(R.string.label_device_imei), job.imeiOrSerial)
                        }
                        if (job.securityLockCode.isNotBlank()) {
                            DetailRow(stringResource(R.string.label_device_lock), job.securityLockCode)
                        }
                        DetailRow(
                            "Intake Date",
                            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(job.createdAt))
                        )
                    }
                }

                // Card 4: Reported Issue
                item {
                    AppCard(testTag = "card_fault_detail") {
                        Text(
                            text = stringResource(R.string.section_fault_info),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = job.issueDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Card 5: 12-Point Condition Checklist
                item {
                    AppCard(testTag = "card_checklist_detail") {
                        Text(
                            text = stringResource(R.string.section_checklist),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ChecklistSummaryViewer(checklist = checklist)
                    }
                }

                // Card 6: Billing & Financial Settlement
                item {
                    AppCard(testTag = "card_billing_detail") {
                        Text(
                            text = stringResource(R.string.section_billing),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        DetailRow(stringResource(R.string.label_estimated_cost), "${shopProfile.currencySymbol}${String.format("%.2f", job.estimatedCost)}")
                        DetailRow(stringResource(R.string.label_advance_paid), "${shopProfile.currencySymbol}${String.format("%.2f", job.advancePaid)}")
                        if (job.finalCost > 0) {
                            DetailRow(stringResource(R.string.label_final_charge), "${shopProfile.currencySymbol}${String.format("%.2f", job.finalCost)}")
                        }
                        if (job.discount > 0) {
                            DetailRow(stringResource(R.string.label_discount), "-${shopProfile.currencySymbol}${String.format("%.2f", job.discount)}")
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 4.dp))
                        DetailRow(
                            label = stringResource(R.string.payment_due),
                            value = "${shopProfile.currencySymbol}${String.format("%.2f", job.balanceDue)}",
                            highlightColor = if (job.isPaid || job.balanceDue <= 0) StatusRepairedEmerald else StatusPendingAmber
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        AppButton(
                            text = stringResource(R.string.btn_record_payment),
                            onClick = { viewModel.showPaymentDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.primary,
                            testTag = "record_payment_btn",
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                // Card 7: Technician Work Notes
                item {
                    AppCard(testTag = "card_notes_detail") {
                        Text(
                            text = stringResource(R.string.label_tech_notes),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        AppTextField(
                            value = techNotesText,
                            onValueChange = { techNotesText = it },
                            label = stringResource(R.string.label_tech_notes),
                            singleLine = false,
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "tech_notes_input"
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            AppButton(
                                text = "Save Notes",
                                onClick = {
                                    viewModel.updateTechnicianNotes(techNotesText)
                                    Toast.makeText(context, "Notes saved", Toast.LENGTH_SHORT).show()
                                },
                                testTag = "save_notes_btn"
                            )
                        }
                    }
                }

                // Card 8: Parts used from inventory
                item {
                    JobPartsCard(job = job, currency = shopProfile.currencySymbol)
                }

                // Bottom Action: Print Receipt
                item {
                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.RECEIPT_PREVIEW) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("view_digital_receipt_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_print_receipt), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Stage Selection Dialog
    if (showStageSelectDialog) {
        var statusNoteInput by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                showStageSelectDialog = false
                statusNoteInput = ""
            },
            title = { Text(stringResource(R.string.btn_update_stage), style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppTextField(
                        value = statusNoteInput,
                        onValueChange = { statusNoteInput = it },
                        label = stringResource(R.string.status_log_note_optional),
                        testTag = "input_stage_log_note"
                    )
                    Text(
                        text = stringResource(R.string.select_new_stage_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    RepairStage.entries.forEach { stage ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val note = statusNoteInput
                                    statusNoteInput = ""
                                    showStageSelectDialog = false
                                    if (stage == RepairStage.DELIVERED) {
                                        viewModel.pendingDeliveryNote = note
                                        viewModel.showDeliveryHandoverDialog = true
                                    } else {
                                        viewModel.updateJobStage(stage, note)
                                    }
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RepairStatusBadge(stage = stage)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showStageSelectDialog = false
                    statusNoteInput = ""
                }) {
                    Text(stringResource(R.string.btn_cancel), style = MaterialTheme.typography.bodyMedium)
                }
            }
        )
    }

    // Delivery & Handover Dialog (Complete job, settle payment, record recipient)
    if (viewModel.showDeliveryHandoverDialog && job != null) {
        DeliveryHandoverDialog(
            job = job,
            currencySymbol = shopProfile.currencySymbol,
            initialDeliveryNote = viewModel.pendingDeliveryNote,
            onDismiss = {
                viewModel.pendingDeliveryNote = ""
                viewModel.showDeliveryHandoverDialog = false
            },
            onConfirm = { finalCost, discount, isPaid, paymentMode, pickedUpBySelf, recName, recPhone, deliveryNote ->
                viewModel.pendingDeliveryNote = ""
                viewModel.completeJobAndDeliver(
                    finalCost = finalCost,
                    discount = discount,
                    isPaid = isPaid,
                    paymentMode = paymentMode,
                    pickedUpByOriginalCustomer = pickedUpBySelf,
                    recipientName = recName,
                    recipientPhone = recPhone,
                    deliveryNote = deliveryNote,
                    onSuccess = {
                        Toast.makeText(context, context.getString(R.string.job_delivered_success), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    // Payment Settlement Dialog
    if (viewModel.showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showPaymentDialog = false },
            title = { Text(stringResource(R.string.dialog_settle_title), style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppTextField(
                        value = editFinalCost,
                        onValueChange = { editFinalCost = sanitizeDecimalInput(it) },
                        label = "${stringResource(R.string.label_final_charge)} (${shopProfile.currencySymbol})",
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        testTag = "input_settle_total"
                    )

                    AppTextField(
                        value = editDiscount,
                        onValueChange = { editDiscount = sanitizeDecimalInput(it) },
                        label = "${stringResource(R.string.label_discount)} (${shopProfile.currencySymbol})",
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        testTag = "input_settle_discount"
                    )

                    Text(stringResource(R.string.label_payment_mode), style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Cash", "UPI", "Card", "Bank").forEach { mode ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { editPaymentMode = mode },
                                color = if (editPaymentMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = mode,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (editPaymentMode == mode) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editIsPaid = !editIsPaid }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.5.dp, if (editIsPaid) StatusRepairedEmerald else MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                                .background(if (editIsPaid) StatusRepairedEmerald else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (editIsPaid) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            stringResource(R.string.label_mark_paid),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalCostNum = if (editFinalCost.isBlank()) {
                            0.0
                        } else {
                            editFinalCost.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: run {
                                Toast.makeText(context, context.getString(R.string.error_invalid_amount), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                        }
                        val discountNum = if (editDiscount.isBlank()) {
                            0.0
                        } else {
                            editDiscount.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: run {
                                Toast.makeText(context, context.getString(R.string.error_invalid_amount), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                        }
                        viewModel.recordPayment(finalCostNum, discountNum, editIsPaid, editPaymentMode)
                        Toast.makeText(context, "Settlement recorded", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_settle_btn")
                ) {
                    Text(stringResource(R.string.btn_save_settlement))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showPaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Job Dialog (containing editable fields and the delete job option)
    if (showEditJobDialog && job != null) {
        EditJobDialog(
            job = job,
            onDismiss = { showEditJobDialog = false },
            onSave = { updatedJob ->
                viewModel.updateJobCard(
                    updatedJob = updatedJob,
                    onSuccess = {
                        showEditJobDialog = false
                        Toast.makeText(context, context.getString(R.string.job_updated_success), Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onDeleteClick = {
                showDeleteConfirmDialog = true
            }
        )
    }

    // Delete Confirmation Dialog (Prompted when delete is requested)
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.btn_delete_job), style = MaterialTheme.typography.titleMedium) },
            text = { Text(stringResource(R.string.confirm_delete_msg), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCurrentJob {
                            showDeleteConfirmDialog = false
                            showEditJobDialog = false
                            Toast.makeText(context, context.getString(R.string.job_deleted_success), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAlertCrimson),
                    modifier = Modifier.testTag("confirm_delete_btn")
                ) {
                    Text(stringResource(R.string.btn_delete_job), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_delete_btn")
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
            modifier = Modifier.testTag("delete_confirm_dialog")
        )
    }
}

@Composable
private fun EditJobDialog(
    job: JobCardEntity,
    onDismiss: () -> Unit,
    onSave: (JobCardEntity) -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    var editCustomerName by rememberSaveable(job.id) { mutableStateOf(job.customerName) }
    var editCustomerPhone by rememberSaveable(job.id) { mutableStateOf(job.customerPhone) }
    var editDeviceBrand by rememberSaveable(job.id) { mutableStateOf(job.deviceBrand) }
    var editDeviceModel by rememberSaveable(job.id) { mutableStateOf(job.deviceModel) }
    var editIssueDescription by rememberSaveable(job.id) { mutableStateOf(job.issueDescription) }
    var editEstimatedCost by rememberSaveable(job.id) { mutableStateOf(formatDecimalForInput(job.estimatedCost)) }
    var editAdvancePaid by rememberSaveable(job.id) { mutableStateOf(formatDecimalForInput(job.advancePaid)) }
    var editTechnicianNotes by rememberSaveable(job.id) { mutableStateOf(job.technicianNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = stringResource(R.string.edit_job_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    text = "${job.jobNumber} • " + stringResource(R.string.edit_job_card_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppTextField(
                    value = editCustomerName,
                    onValueChange = { editCustomerName = it },
                    label = stringResource(R.string.label_customer_name),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "edit_customer_name_input"
                )
                AppTextField(
                    value = editCustomerPhone,
                    onValueChange = { editCustomerPhone = it },
                    label = stringResource(R.string.label_customer_phone),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "edit_customer_phone_input"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppTextField(
                        value = editDeviceBrand,
                        onValueChange = { editDeviceBrand = it },
                        label = stringResource(R.string.label_device_brand),
                        modifier = Modifier.weight(1f),
                        testTag = "edit_device_brand_input"
                    )
                    AppTextField(
                        value = editDeviceModel,
                        onValueChange = { editDeviceModel = it },
                        label = stringResource(R.string.label_device_model),
                        modifier = Modifier.weight(1.3f),
                        testTag = "edit_device_model_input"
                    )
                }
                AppTextField(
                    value = editIssueDescription,
                    onValueChange = { editIssueDescription = it },
                    label = stringResource(R.string.label_fault_description),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    singleLine = false,
                    testTag = "edit_issue_description_input"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppTextField(
                        value = editEstimatedCost,
                        onValueChange = { editEstimatedCost = sanitizeDecimalInput(it) },
                        label = stringResource(R.string.label_estimated_cost),
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        testTag = "edit_est_cost_input"
                    )
                    AppTextField(
                        value = editAdvancePaid,
                        onValueChange = { editAdvancePaid = sanitizeDecimalInput(it) },
                        label = stringResource(R.string.label_advance_paid),
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        testTag = "edit_advance_input"
                    )
                }
                AppTextField(
                    value = editTechnicianNotes,
                    onValueChange = { editTechnicianNotes = it },
                    label = stringResource(R.string.label_tech_notes),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    singleLine = false,
                    testTag = "edit_tech_notes_input"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Delete option placed inside the edit card as requested
                OutlinedButton(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusAlertCrimson),
                    border = BorderStroke(1.dp, StatusAlertCrimson),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 44.dp)
                        .testTag("edit_dialog_delete_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = StatusAlertCrimson,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.btn_delete_job),
                        color = StatusAlertCrimson,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = editCustomerName.trim()
                    val trimmedPhone = editCustomerPhone.trim()
                    val trimmedBrand = editDeviceBrand.trim()
                    val trimmedModel = editDeviceModel.trim()
                    val trimmedIssue = editIssueDescription.trim()
                    if (trimmedName.isEmpty() || trimmedPhone.isEmpty() || trimmedBrand.isEmpty() || trimmedModel.isEmpty() || trimmedIssue.isEmpty()) {
                        Toast.makeText(context, context.getString(R.string.error_edit_required_fields), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val estCostDouble = if (editEstimatedCost.isBlank()) {
                        0.0
                    } else {
                        editEstimatedCost.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: run {
                            Toast.makeText(context, context.getString(R.string.error_invalid_amount), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                    }
                    val advPaidDouble = if (editAdvancePaid.isBlank()) {
                        0.0
                    } else {
                        editAdvancePaid.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: run {
                            Toast.makeText(context, context.getString(R.string.error_invalid_amount), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                    }
                    val updated = job.copy(
                        customerName = trimmedName,
                        customerPhone = trimmedPhone,
                        deviceBrand = trimmedBrand,
                        deviceModel = trimmedModel,
                        issueDescription = trimmedIssue,
                        estimatedCost = estCostDouble,
                        advancePaid = advPaidDouble,
                        technicianNotes = editTechnicianNotes.trim()
                    )
                    onSave(updated)
                },
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("edit_save_btn"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.btn_save_changes))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("edit_cancel_btn")
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
        modifier = Modifier.testTag("edit_job_dialog")
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    highlightColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatusLogTimelineItem(
    log: StatusLogEntry,
    isLatest: Boolean,
    modifier: Modifier = Modifier
) {
    val formattedDateTime = remember(log.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isLatest) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        shape = RoundedCornerShape(8.dp),
        border = if (isLatest) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RepairStatusBadge(stage = log.stage)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedDateTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (log.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun DeliveryHandoverDialog(
    job: JobCardEntity,
    currencySymbol: String,
    initialDeliveryNote: String = "",
    onDismiss: () -> Unit,
    onConfirm: (
        finalCost: Double,
        discount: Double,
        isPaid: Boolean,
        paymentMode: String,
        pickedUpByOriginalCustomer: Boolean,
        recipientName: String,
        recipientPhone: String,
        deliveryNote: String
    ) -> Unit
) {
    val context = LocalContext.current
    val initialCost = if (job.finalCost > 0) job.finalCost else (if (job.estimatedCost > 0) job.estimatedCost else 0.0)
    var editFinalCost by rememberSaveable(job.id) { mutableStateOf(formatDecimalForInput(initialCost)) }
    var editDiscount by rememberSaveable(job.id) { mutableStateOf(formatDecimalForInput(job.discount)) }
    var editIsPaid by rememberSaveable(job.id) { mutableStateOf(true) }
    var editPaymentMode by rememberSaveable(job.id) { mutableStateOf(if (job.paymentMode.isNotBlank()) job.paymentMode else "Cash") }

    var pickedUpBySelf by rememberSaveable(job.id) { mutableStateOf(job.pickedUpByOriginalCustomer) }
    var recipientName by rememberSaveable(job.id) { mutableStateOf(job.recipientName) }
    var recipientPhone by rememberSaveable(job.id) { mutableStateOf(job.recipientPhone) }
    var deliveryNote by rememberSaveable(job.id, initialDeliveryNote) { mutableStateOf(initialDeliveryNote) }

    var nameError by rememberSaveable(job.id) { mutableStateOf(false) }
    var phoneError by rememberSaveable(job.id) { mutableStateOf(false) }

    val finalCostNum = if (editFinalCost.isBlank()) 0.0 else (editFinalCost.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0)
    val discountNum = if (editDiscount.isBlank()) 0.0 else (editDiscount.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0)
    val netBalanceDue = (finalCostNum - discountNum - job.advancePaid).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = stringResource(R.string.dialog_delivery_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    text = "${job.jobNumber} • ${job.deviceBrand} ${job.deviceModel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Payment Settlement
                Text(
                    text = stringResource(R.string.dialog_settle_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )

                AppTextField(
                    value = editFinalCost,
                    onValueChange = { editFinalCost = sanitizeDecimalInput(it) },
                    label = "${stringResource(R.string.label_final_charge)} ($currencySymbol)",
                    placeholder = "0",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    testTag = "delivery_final_charge_input"
                )

                AppTextField(
                    value = editDiscount,
                    onValueChange = { editDiscount = sanitizeDecimalInput(it) },
                    label = "${stringResource(R.string.label_discount)} ($currencySymbol)",
                    placeholder = "0",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    testTag = "delivery_discount_input"
                )

                if (job.advancePaid > 0) {
                    DetailRow(
                        label = stringResource(R.string.label_advance_paid),
                        value = "$currencySymbol${String.format(Locale.getDefault(), "%.2f", job.advancePaid)}"
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.payment_due),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$currencySymbol${String.format(Locale.getDefault(), "%.2f", netBalanceDue)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (editIsPaid) StatusRepairedEmerald else StatusAlertCrimson,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }

                Text(stringResource(R.string.label_payment_mode), style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Cash", "UPI", "Card", "Bank").forEach { mode ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { editPaymentMode = mode },
                            color = if (editPaymentMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = mode,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (editPaymentMode == mode) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editIsPaid = !editIsPaid }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.5.dp, if (editIsPaid) StatusRepairedEmerald else MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                            .background(if (editIsPaid) StatusRepairedEmerald else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (editIsPaid) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        stringResource(R.string.label_mark_paid),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Section 2: Handover Recipient Selection
                Text(
                    text = stringResource(R.string.handover_recipient_section_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )

                // Option A: Original Customer
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            pickedUpBySelf = true
                            nameError = false
                            phoneError = false
                        },
                    color = if (pickedUpBySelf) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (pickedUpBySelf) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, if (pickedUpBySelf) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                                .background(if (pickedUpBySelf) MaterialTheme.colorScheme.primary else Color.Transparent)
                        )
                        Column {
                            Text(
                                text = "${stringResource(R.string.handover_original_customer)}: ${job.customerName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (pickedUpBySelf) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                            Text(
                                text = job.customerPhone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Option B: Other Person / Representative
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { pickedUpBySelf = false },
                    color = if (!pickedUpBySelf) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (!pickedUpBySelf) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, if (!pickedUpBySelf) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                                .background(if (!pickedUpBySelf) MaterialTheme.colorScheme.primary else Color.Transparent)
                        )
                        Text(
                            text = stringResource(R.string.handover_other_person),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (!pickedUpBySelf) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                        )
                    }
                }

                // If other person picked up, show Name and Phone inputs
                if (!pickedUpBySelf) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppTextField(
                            value = recipientName,
                            onValueChange = {
                                recipientName = it
                                if (it.isNotBlank()) nameError = false
                            },
                            label = stringResource(R.string.handover_recipient_name),
                            isError = nameError,
                            errorMessage = if (nameError) stringResource(R.string.handover_name_required_error) else null,
                            testTag = "delivery_recipient_name_input"
                        )

                        AppTextField(
                            value = recipientPhone,
                            onValueChange = {
                                recipientPhone = it
                                if (it.isNotBlank()) phoneError = false
                            },
                            label = stringResource(R.string.handover_recipient_phone),
                            isError = phoneError,
                            errorMessage = if (phoneError) stringResource(R.string.handover_phone_required_error) else null,
                            testTag = "delivery_recipient_phone_input"
                        )
                    }
                }

                AppTextField(
                    value = deliveryNote,
                    onValueChange = { deliveryNote = it },
                    label = stringResource(R.string.handover_delivery_note),
                    testTag = "delivery_note_input"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validatedFinalCost = if (editFinalCost.isBlank()) {
                        0.0
                    } else {
                        editFinalCost.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: run {
                            Toast.makeText(context, context.getString(R.string.error_invalid_amount), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                    }
                    val validatedDiscount = if (editDiscount.isBlank()) {
                        0.0
                    } else {
                        editDiscount.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: run {
                            Toast.makeText(context, context.getString(R.string.error_invalid_amount), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                    }
                    if (!pickedUpBySelf) {
                        var hasErr = false
                        if (recipientName.isBlank()) {
                            nameError = true
                            hasErr = true
                        }
                        if (recipientPhone.isBlank()) {
                            phoneError = true
                            hasErr = true
                        }
                        if (hasErr) return@Button
                    }
                    onConfirm(
                        validatedFinalCost,
                        validatedDiscount,
                        editIsPaid,
                        editPaymentMode,
                        pickedUpBySelf,
                        recipientName,
                        recipientPhone,
                        deliveryNote
                    )
                },
                modifier = Modifier.testTag("confirm_delivery_handover_btn")
            ) {
                Text(stringResource(R.string.btn_confirm_delivery))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

private fun formatDecimalForInput(value: Double): String {
    if (value <= 0.0) return ""
    return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
}

private fun sanitizeDecimalInput(input: String): String {
    var dotSeen = false
    return buildString {
        for (ch in input) {
            when {
                ch.isDigit() -> append(ch)
                ch == '.' && !dotSeen -> {
                    dotSeen = true
                    append(ch)
                }
            }
        }
    }
}
