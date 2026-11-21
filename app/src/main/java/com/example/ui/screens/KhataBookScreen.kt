package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.entity.JobCardEntity
import com.example.data.model.CustomerKhataSummary
import com.example.data.model.RepairStage
import com.example.ui.AppScreen
import com.example.ui.ShopViewModel
import com.example.ui.components.AppButton
import com.example.ui.components.AppCard
import com.example.ui.components.AppTextField
import com.example.ui.components.RepairStatusBadge
import com.example.ui.theme.ShopTheme
import com.example.ui.theme.StatusAlertCrimson
import com.example.ui.theme.StatusRepairedEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataBookScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.filteredKhataCustomers.collectAsStateWithLifecycle()
    val allCustomers by viewModel.khataCustomers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.khataSearchQuery.collectAsStateWithLifecycle()
    val onlyDues by viewModel.khataOnlyDuesFilter.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val currency = shopProfile.currencySymbol.ifBlank { "₹" }

    val totalDues = remember(allCustomers) { allCustomers.sumOf { it.totalPendingDue } }
    val customersWithDues = remember(allCustomers) { allCustomers.count { it.totalPendingDue > 0.01 } }

    BackHandler {
        viewModel.navigateBack()
    }

    val shopColors = ShopTheme.colors
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.khata_book_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = shopColors.topBarContent
                        )
                        Text(
                            text = stringResource(R.string.khata_book_subtitle),
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
                            .testTag("khata_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AppCard(testTag = "card_khata_summary") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.khata_total_dues_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$currency${String.format(Locale.US, "%.2f", totalDues)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (totalDues > 0.01) shopColors.alert else shopColors.repaired
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.khata_customers_with_dues, customersWithDues),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (totalDues > 0.01) shopColors.alertContainer else shopColors.repairedContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = if (totalDues > 0.01) shopColors.alert else shopColors.repaired,
                                    modifier = Modifier.padding(12.dp).size(28.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onKhataSearchQueryChanged(it) },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.khata_search_hint),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onKhataSearchQueryChanged("") },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("input_khata_search")
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !onlyDues,
                            onClick = { viewModel.onKhataOnlyDuesChanged(false) },
                            label = {
                                Text("${stringResource(R.string.khata_filter_all)} (${allCustomers.size})")
                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("chip_khata_all")
                        )
                        FilterChip(
                            selected = onlyDues,
                            onClick = { viewModel.onKhataOnlyDuesChanged(true) },
                            label = {
                                Text("${stringResource(R.string.khata_filter_dues)} ($customersWithDues)")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = shopColors.alertContainer,
                                selectedLabelColor = shopColors.alert
                            ),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("chip_khata_dues")
                        )
                    }
                }

                if (customers.isEmpty()) {
                    item {
                        AppCard(testTag = "card_khata_empty") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.khata_empty_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(customers, key = { it.customerKey }) { customer ->
                        CustomerKhataCard(
                            summary = customer,
                            currency = currency,
                            onClick = { viewModel.openKhataDetail(customer.customerKey) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerKhataCard(
    summary: CustomerKhataSummary,
    currency: String,
    onClick: () -> Unit
) {
    val hasDue = summary.totalPendingDue > 0.01
    val cardColors = ShopTheme.colors
    AppCard(
        onClick = onClick,
        testTag = "khata_customer_card_${summary.customerKey}"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.customerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (summary.customerPhone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = summary.customerPhone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (hasDue) cardColors.alertContainer else cardColors.repairedContainer
            ) {
                Text(
                    text = if (hasDue) {
                        stringResource(
                            R.string.khata_due_badge,
                            "$currency${String.format(Locale.US, "%.2f", summary.totalPendingDue)}"
                        )
                    } else {
                        stringResource(R.string.khata_settled_badge)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (hasDue) cardColors.alert else cardColors.repaired,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.khata_visits_count, summary.totalJobsCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(
                    R.string.khata_total_billed,
                    "$currency${String.format(Locale.US, "%.0f", summary.totalBilled)}"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.khata_total_paid,
                    "$currency${String.format(Locale.US, "%.0f", summary.totalPaid)}"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = cardColors.repaired
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataDetailScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customer by viewModel.selectedKhataCustomer.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val currency = shopProfile.currencySymbol.ifBlank { "₹" }

    var showReceivePaymentDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler {
        viewModel.navigateBack()
    }

    val detailColors = ShopTheme.colors
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = customer?.customerName ?: stringResource(R.string.khata_detail_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = detailColors.topBarContent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!customer?.customerPhone.isNullOrBlank()) {
                            Text(
                                text = customer?.customerPhone.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = detailColors.topBarContent.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                            .testTag("khata_detail_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = detailColors.topBarContent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = detailColors.topBarContainer
                )
            )
        }
    ) { innerPadding ->
        val current = customer
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.khata_empty_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AppCard(testTag = "card_khata_customer_overview") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = stringResource(
                                        R.string.khata_total_billed,
                                        "$currency${String.format(Locale.US, "%.2f", current.totalBilled)}"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(
                                        R.string.khata_total_paid,
                                        "$currency${String.format(Locale.US, "%.2f", current.totalPaid)}"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StatusRepairedEmerald
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(R.string.khata_due_badge, "$currency${String.format(Locale.US, "%.2f", current.totalPendingDue)}"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (current.totalPendingDue > 0.01) StatusAlertCrimson else StatusRepairedEmerald
                                )
                                Text(
                                    text = stringResource(R.string.khata_visits_count, current.totalJobsCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (current.customerPhone.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { viewModel.callCustomer(context, current.customerPhone) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("btn_khata_call"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = stringResource(R.string.btn_call_customer), maxLines = 1)
                                }
                            }
                            OutlinedButton(
                                onClick = { viewModel.prepareKhataWhatsAppReminder(current) },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("btn_khata_whatsapp"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = stringResource(R.string.khata_whatsapp_reminder), maxLines = 1)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppButton(
                                text = stringResource(R.string.khata_receive_payment),
                                onClick = {
                                    if (current.totalPendingDue > 0.01) {
                                        showReceivePaymentDialog = true
                                    } else {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.khata_no_pending_dues),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                enabled = current.totalPendingDue > 0.01,
                                containerColor = StatusRepairedEmerald,
                                modifier = Modifier.weight(1f),
                                testTag = "btn_khata_receive_payment",
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            )
                            AppButton(
                                text = stringResource(R.string.khata_new_job_for_customer),
                                onClick = {
                                    viewModel.startNewJobForCustomer(current.customerName, current.customerPhone)
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "btn_khata_new_job",
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.khata_job_history_header),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(current.jobs, key = { it.id }) { job ->
                    KhataJobRowCard(
                        job = job,
                        currency = currency,
                        onClick = { viewModel.navigateTo(AppScreen.JOB_DETAIL, job.id) }
                    )
                }
            }
        }
    }

    if (showReceivePaymentDialog && customer != null) {
        ReceiveKhataPaymentDialog(
            customer = customer!!,
            currency = currency,
            onDismiss = { showReceivePaymentDialog = false },
            onConfirm = { amount, mode, note ->
                viewModel.recordKhataPayment(
                    customerKey = customer!!.customerKey,
                    amount = amount,
                    paymentMode = mode,
                    note = note,
                    onSuccess = { count ->
                        showReceivePaymentDialog = false
                        Toast.makeText(
                            context,
                            context.getString(R.string.khata_payment_success, count),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onError = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
}

@Composable
private fun KhataJobRowCard(
    job: JobCardEntity,
    currency: String,
    onClick: () -> Unit
) {
    val dateFormatted = remember(job.createdAt) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(job.createdAt))
    }
    val effectiveTotal = if (job.finalCost > 0.0) job.finalCost else job.estimatedCost
    val netTotal = (effectiveTotal - job.discount).coerceAtLeast(0.0)

    AppCard(
        onClick = onClick,
        testTag = "khata_job_item_${job.id}"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.jobNumber,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${job.deviceBrand} ${job.deviceModel}".trim(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$dateFormatted • ${job.issueDescription}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            RepairStatusBadge(stage = job.stage)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.khata_total_billed, "$currency${String.format(Locale.US, "%.0f", netTotal)}"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val due = job.balanceDue
            Text(
                text = if (job.stage == RepairStage.CANCELLED) {
                    stringResource(R.string.status_cancelled)
                } else if (job.isPaid || due <= 0.01) {
                    stringResource(R.string.khata_settled_badge)
                } else {
                    stringResource(R.string.khata_due_badge, "$currency${String.format(Locale.US, "%.2f", due)}")
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (job.stage != RepairStage.CANCELLED && !job.isPaid && due > 0.01) {
                    StatusAlertCrimson
                } else {
                    StatusRepairedEmerald
                }
            )
        }
    }
}

@Composable
private fun ReceiveKhataPaymentDialog(
    customer: CustomerKhataSummary,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String) -> Unit
) {
    val context = LocalContext.current
    var amountText by rememberSaveable {
        mutableStateOf(String.format(Locale.US, "%.2f", customer.totalPendingDue))
    }
    var paymentMode by rememberSaveable { mutableStateOf("Cash") }
    var note by rememberSaveable { mutableStateOf("") }
    val modes = listOf("Cash", "UPI", "Card", "Transfer")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.khata_payment_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${customer.customerName} (${stringResource(R.string.khata_due_badge, "$currency${String.format(Locale.US, "%.2f", customer.totalPendingDue)}")})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AppTextField(
                    value = amountText,
                    onValueChange = { input ->
                        amountText = input.filter { it.isDigit() || it == '.' }
                    },
                    label = stringResource(R.string.khata_payment_amount_label),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    testTag = "input_khata_payment_amount"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    modes.forEach { mode ->
                        FilterChip(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            label = { Text(mode, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                        )
                    }
                }
                AppTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = stringResource(R.string.khata_payment_note_label),
                    testTag = "input_khata_payment_note"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = amountText.trim().toDoubleOrNull()
                    if (parsed == null || !parsed.isFinite() || parsed <= 0.0) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.khata_payment_invalid),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@TextButton
                    }
                    onConfirm(parsed, paymentMode, note)
                },
                modifier = Modifier.testTag("btn_confirm_khata_payment")
            ) {
                Text(
                    text = stringResource(R.string.khata_receive_payment),
                    color = StatusRepairedEmerald,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
