package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.entity.JobCardEntity
import com.example.data.model.RepairStage
import com.example.ui.AppScreen
import com.example.ui.ShopViewModel
import com.example.ui.UpdateCheckUiState
import com.example.ui.components.AppCard
import com.example.ui.components.RepairStatusBadge
import com.example.ui.theme.ShopTheme
import com.example.ui.theme.StatusPendingAmber
import com.example.ui.theme.StatusRepairedEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val jobs by viewModel.filteredJobs.collectAsStateWithLifecycle()
    val allJobsList by viewModel.allJobs.collectAsStateWithLifecycle()
    val kpis by viewModel.kpiMetrics.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedStage by viewModel.selectedStageFilter.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val syncConfig by viewModel.googleSyncConfig.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val showUpdateBanner by viewModel.showUpdateBanner.collectAsStateWithLifecycle()
    val updateCheckState by viewModel.updateCheckState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val shopColors = ShopTheme.colors

    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSearchActive) {
                // Active Search TopAppBar
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = {
                                Text(
                                    stringResource(R.string.search_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("topbar_search_input")
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                isSearchActive = false
                                viewModel.onSearchQueryChanged("")
                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                                .testTag("close_search_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close search",
                                tint = shopColors.topBarContent
                            )
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.onSearchQueryChanged("") },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                                    .testTag("clear_search_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = shopColors.topBarContent
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = shopColors.topBarContainer
                    )
                )
            } else {
                // Standard TopAppBar with Search beside Settings
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = shopProfile.shopName,
                                style = MaterialTheme.typography.titleMedium,
                                color = shopColors.topBarContent
                            )
                            Text(
                                text = stringResource(R.string.dashboard_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = shopColors.topBarContent.copy(alpha = 0.8f)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.KHATA_BOOK) },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                                .testTag("topbar_khata_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = stringResource(R.string.khata_book_title),
                                tint = shopColors.topBarContent
                            )
                        }
                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                                .testTag("topbar_search_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.search_hint),
                                tint = shopColors.topBarContent
                            )
                        }
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.SYNC_MANAGER) },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                                .testTag("topbar_sync_btn")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = shopColors.topBarContent
                                )
                            } else {
                                Icon(
                                    imageVector = if (syncConfig.isConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = if (syncConfig.isConnected) {
                                        stringResource(R.string.sheets_status_connected)
                                    } else {
                                        stringResource(R.string.sheets_status_disconnected)
                                    },
                                    tint = if (syncConfig.isConnected) StatusRepairedEmerald else StatusPendingAmber
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.SETTINGS_HOME) },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                                .testTag("nav_settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.nav_settings),
                                tint = shopColors.topBarContent
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = shopColors.topBarContainer
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.resetNewJobForm()
                    viewModel.navigateTo(AppScreen.NEW_JOB)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("fab_new_job")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Text(
                        text = stringResource(R.string.nav_new_job),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
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
                    .widthIn(max = 720.dp), // Adaptive tablet & counter layout spec
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                val availableUpdate = updateCheckState as? UpdateCheckUiState.UpdateAvailable
                if (showUpdateBanner && availableUpdate != null) {
                    item(key = "update_available_banner") {
                        UpdateAvailableBanner(
                            update = availableUpdate,
                            onUpdateNow = {
                                val targetUrl = availableUpdate.apkDownloadUrl ?: availableUpdate.releasePageUrl
                                viewModel.openUpdateReleasePage(context, targetUrl)
                            },
                            onDismiss = { viewModel.dismissUpdateBanner() },
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)
                        )
                    }
                }

                // Quick KPI Cards (Starts immediately at top)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        M3KpiCard(
                            title = stringResource(R.string.kpi_active_repairs),
                            value = "${kpis.activeJobsCount}",
                            icon = Icons.Default.Build,
                            tintColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.onStageFilterChanged(null) }
                        )
                        M3KpiCard(
                            title = stringResource(R.string.kpi_ready_deliver),
                            value = "${kpis.readyForPickupCount}",
                            icon = Icons.Default.DoneAll,
                            tintColor = shopColors.repaired,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.onStageFilterChanged(RepairStage.READY_FOR_PICKUP) }
                        )
                        M3KpiCard(
                            title = stringResource(R.string.kpi_pending_dues),
                            value = "${shopProfile.currencySymbol}${String.format("%.0f", kpis.totalPendingDues)}",
                            icon = Icons.Default.Warning,
                            tintColor = shopColors.pending,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(AppScreen.KHATA_BOOK) }
                        )
                    }
                }

                // Status Filter Chips
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedStage == null,
                                onClick = { viewModel.onStageFilterChanged(null) },
                                label = {
                                    Text(
                                        "${stringResource(R.string.filter_all)} (${allJobsList.size})",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("filter_all_jobs")
                            )
                        }
                        items(RepairStage.entries) { stage ->
                            val count = allJobsList.count { it.stage == stage }
                            val labelRes = when (stage) {
                                RepairStage.DIAGNOSTIC -> R.string.status_diagnostics
                                RepairStage.WAITING_FOR_PARTS -> R.string.status_waiting_parts
                                RepairStage.IN_PROGRESS -> R.string.status_in_progress
                                RepairStage.READY_FOR_PICKUP -> R.string.status_ready_deliver
                                RepairStage.DELIVERED -> R.string.status_delivered
                                RepairStage.CANCELLED -> R.string.status_cancelled
                            }
                            FilterChip(
                                selected = selectedStage == stage,
                                onClick = { viewModel.onStageFilterChanged(if (selectedStage == stage) null else stage) },
                                label = {
                                    Text(
                                        "${stringResource(labelRes)} ($count)",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("filter_stage_${stage.name.lowercase()}")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Active Search indicator if query active
                if (searchQuery.isNotBlank()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Filtered by: \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${jobs.size} found",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                // Job Cards List
                if (jobs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = stringResource(R.string.empty_jobs_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.empty_jobs_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(jobs, key = { it.id }) { job ->
                        M3JobItemCard(
                            job = job,
                            currencySymbol = shopProfile.currencySymbol,
                            onClick = { viewModel.navigateTo(AppScreen.JOB_DETAIL, job.id) },
                            onCallClick = { viewModel.callCustomer(context, job.customerPhone) },
                            onWhatsAppClick = { viewModel.prepareWhatsAppMessage(job) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun M3KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    tintColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    AppCard(
        modifier = modifier,
        onClick = onClick,
        testTag = "kpi_${title.lowercase().replace(" ", "_")}"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun M3JobItemCard(
    job: JobCardEntity,
    currencySymbol: String,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier,
        onClick = onClick,
        testTag = "job_card_item_${job.jobNumber}"
    ) {
        // Row 1: Job Number & Stage Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "#${job.jobNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "• ${formatRelativeTime(job.updatedAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RepairStatusBadge(stage = job.stage)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 2: Device Brand & Model
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = job.deviceBrand.uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(
                text = job.deviceModel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Row 3: Issue Description
        Text(
            text = job.issueDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Row 4: Actions & Dues
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column {
                    Text(
                        text = job.customerName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = job.customerPhone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                        .testTag("call_btn_${job.jobNumber}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = stringResource(R.string.btn_call_customer),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onWhatsAppClick,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                        .testTag("whatsapp_btn_${job.jobNumber}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = stringResource(R.string.btn_whatsapp),
                        tint = ShopTheme.colors.repaired,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Payment Status Label
            val isPaid = job.isPaid || job.balanceDue <= 0.0
            val itemColors = ShopTheme.colors
            Surface(
                color = if (isPaid) itemColors.repairedContainer else itemColors.pendingContainer,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = if (isPaid) {
                        stringResource(R.string.payment_paid)
                    } else {
                        "${stringResource(R.string.payment_due)}: $currencySymbol${String.format("%.2f", job.balanceDue)}"
                    },
                    color = if (isPaid) itemColors.repaired else itemColors.pending,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun UpdateAvailableBanner(
    update: UpdateCheckUiState.UpdateAvailable,
    onUpdateNow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shopColors = ShopTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = shopColors.pendingContainer,
        border = BorderStroke(1.dp, shopColors.pending.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("banner_update_available")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NewReleases,
                contentDescription = null,
                tint = shopColors.pending,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.banner_update_available_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = shopColors.pending,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.banner_update_available_subtitle,
                        update.latestVersion,
                        update.currentVersion
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = onUpdateNow,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("banner_btn_update_now")
            ) {
                Text(
                    text = stringResource(R.string.btn_update_now),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                    .testTag("banner_btn_dismiss_update")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_dismiss_update_banner),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val hours = diff / (1000 * 60 * 60)
    return when {
        hours < 1 -> "Just now"
        hours < 24 -> "${hours}h"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}
