package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.database.Converters
import com.example.ui.ShopViewModel
import com.example.ui.components.AppButton
import com.example.ui.components.ChecklistSummaryViewer
import com.example.ui.theme.StatusPendingAmber
import com.example.ui.theme.StatusRepairedEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptPreviewScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allJobs by viewModel.allJobs.collectAsStateWithLifecycle()
    val job = allJobs.find { it.id == viewModel.selectedJobId } ?: viewModel.selectedJob
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()

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

    val shopColors = com.example.ui.theme.ShopTheme.colors
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.btn_print_receipt),
                        style = MaterialTheme.typography.titleMedium,
                        color = shopColors.topBarContent
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = shopColors.topBarContent)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openReceiptShareDialog(job) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                            .testTag("receipt_topbar_whatsapp_btn")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Chat, contentDescription = stringResource(R.string.receipt_share_dialog_title), tint = shopColors.topBarContent)
                    }
                    IconButton(
                        onClick = {
                            Toast.makeText(context, context.getString(R.string.pdf_generating_toast), Toast.LENGTH_SHORT).show()
                            viewModel.shareJobReceiptPdf(context, job)
                        },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                            .testTag("receipt_share_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = stringResource(R.string.btn_share_slip), tint = shopColors.topBarContent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = shopColors.topBarContainer)
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
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Printable A5 Slip Paper Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("printable_receipt_card"),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Shop Header
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = shopProfile.shopName.uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = shopProfile.tagline,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "${shopProfile.address} • Tel: ${shopProfile.contactPhone}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }

                            HorizontalDivider(color = Color.Black, thickness = 1.dp)

                            // Barcode & Job Tag
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(stringResource(R.string.rc_receipt_title), style = MaterialTheme.typography.titleSmall, color = Color.Black)
                                    Text(
                                        text = "#${job.jobNumber}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF1E3A8A)
                                    )
                                }

                                // Barcode graphic
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        listOf(3, 1, 2, 4, 1, 3, 2, 1, 4, 2, 1, 3, 2, 4, 1).forEach { barWidth ->
                                            Box(
                                                modifier = Modifier
                                                    .width(barWidth.dp)
                                                    .height(26.dp)
                                                    .background(Color.Black)
                                            )
                                        }
                                    }
                                    Text(
                                        text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(job.createdAt)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.LightGray)

                            // Customer & Device Table
                            SlipRow(stringResource(R.string.rc_customer), job.customerName)
                            SlipRow(stringResource(R.string.rc_phone), job.customerPhone)
                            SlipRow(stringResource(R.string.rc_device), "${job.deviceBrand} ${job.deviceModel}")
                            if (job.deviceColor.isNotBlank()) SlipRow(stringResource(R.string.rc_color), job.deviceColor)
                            if (job.imeiOrSerial.isNotBlank()) SlipRow(stringResource(R.string.rc_imei), job.imeiOrSerial)

                            HorizontalDivider(color = Color.LightGray)

                            Text(
                                text = stringResource(R.string.rc_reported_problem) + ":",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.Black
                            )
                            Text(
                                text = job.issueDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray
                            )

                            HorizontalDivider(color = Color.LightGray)

                            Text(
                                text = "12-POINT INTAKE CHECKLIST:",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.Black
                            )
                            ChecklistSummaryViewer(checklist = checklist)

                            HorizontalDivider(color = Color.LightGray)

                            // Billing Breakdown
                            val totalCharge = if (job.finalCost > 0) job.finalCost else job.estimatedCost
                            SlipRow(stringResource(R.string.rc_estimate_charge), "${shopProfile.currencySymbol}${String.format("%.2f", totalCharge)}")
                            SlipRow(stringResource(R.string.rc_advance_paid), "-${shopProfile.currencySymbol}${String.format("%.2f", job.advancePaid)}")
                            if (job.discount > 0) {
                                SlipRow(stringResource(R.string.rc_discount), "-${shopProfile.currencySymbol}${String.format("%.2f", job.discount)}")
                            }

                            HorizontalDivider(color = Color.Black, thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val balanceLabel = if (job.isPaid) {
                                    "${stringResource(R.string.payment_paid)} (${job.paymentMode})"
                                } else {
                                    stringResource(R.string.rc_net_balance_due)
                                }
                                Text(balanceLabel, style = MaterialTheme.typography.titleMedium, color = Color.Black)
                                Text(
                                    text = "${shopProfile.currencySymbol}${String.format("%.2f", job.balanceDue)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (job.isPaid || job.balanceDue <= 0) StatusRepairedEmerald else StatusPendingAmber
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stringResource(R.string.rc_terms) + ":", style = MaterialTheme.typography.titleSmall, color = Color.Black)
                            Text(
                                text = shopProfile.termsAndConditions,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                lineHeight = 14.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Signatures
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(110.dp).height(1.dp).background(Color.Black))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(stringResource(R.string.rc_customer_signature), style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(110.dp).height(1.dp).background(Color.Black))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(stringResource(R.string.rc_technician_signature), style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                                }
                            }
                        }
                    }
                }

                // Direct WhatsApp, WA Business & Print Controls
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("receipt_direct_share_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${stringResource(R.string.receipt_share_dialog_title)}: ${job.customerPhone}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.receipt_share_dialog_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.sendReceiptPdfViaWhatsApp(context, job, "com.whatsapp")
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("receipt_send_whatsapp_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusRepairedEmerald),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_share_whatsapp), style = MaterialTheme.typography.bodyMedium)
                                }

                                Button(
                                    onClick = {
                                        viewModel.sendReceiptPdfViaWhatsApp(context, job, "com.whatsapp.w4b")
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("receipt_send_w4b_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF075E54)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_share_w4b), style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppButton(
                                    text = stringResource(R.string.btn_share_slip),
                                    onClick = {
                                        Toast.makeText(context, context.getString(R.string.pdf_generating_toast), Toast.LENGTH_SHORT).show()
                                        viewModel.shareJobReceiptPdf(context, job)
                                    },
                                    modifier = Modifier.weight(1f),
                                    testTag = "action_share_receipt_btn",
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                )

                                AppButton(
                                    text = stringResource(R.string.btn_print_receipt),
                                    onClick = {
                                        Toast.makeText(context, context.getString(R.string.pdf_generating_toast), Toast.LENGTH_SHORT).show()
                                        viewModel.printJobReceiptPdf(context, job)
                                    },
                                    modifier = Modifier.weight(1f),
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    testTag = "action_print_btn",
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (viewModel.showReceiptShareDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissReceiptShareDialog() },
            title = {
                Text(stringResource(R.string.receipt_share_dialog_title), style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${stringResource(R.string.receipt_share_dialog_subtitle)} (${job.customerPhone})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            viewModel.sendReceiptPdfViaWhatsApp(context, job, "com.whatsapp")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRepairedEmerald),
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_share_whatsapp), style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        onClick = {
                            viewModel.sendReceiptPdfViaWhatsApp(context, job, "com.whatsapp.w4b")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF075E54)),
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_share_w4b), style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        onClick = {
                            viewModel.dismissReceiptShareDialog()
                            viewModel.printJobReceiptPdf(context, job)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_share_other_print), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.dismissReceiptShareDialog() }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun SlipRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = Color.Black)
    }
}
