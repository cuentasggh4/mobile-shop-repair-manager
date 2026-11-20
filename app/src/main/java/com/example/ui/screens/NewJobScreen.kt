package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.normalizeKhataKey
import com.example.ui.AppScreen
import com.example.ui.ShopViewModel
import com.example.ui.components.AppButton
import com.example.ui.components.AppCard
import com.example.ui.components.AppTextField
import com.example.ui.components.InteractiveChecklistGrid
import com.example.ui.theme.ShopTheme
import java.util.Locale

private val BRAND_LIST = listOf(
    "Apple", "Samsung", "Xiaomi", "Vivo", "Oppo", "OnePlus", "Realme", "Google", "Motorola", "Other"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewJobScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val khataCustomers by viewModel.khataCustomers.collectAsStateWithLifecycle()
    var showCustomerSuggestions by rememberSaveable { mutableStateOf(false) }
    val shopColors = ShopTheme.colors

    val matchingCustomers = remember(viewModel.newCustomerName, khataCustomers) {
        val q = viewModel.newCustomerName.trim().lowercase()
        if (q.isEmpty()) {
            emptyList()
        } else {
            khataCustomers.filter {
                it.customerName.lowercase().contains(q) ||
                    it.customerPhone.lowercase().contains(q)
            }.take(5)
        }
    }

    val matchedCustomerKhata = remember(viewModel.newCustomerName, viewModel.newCustomerPhone, khataCustomers) {
        if (viewModel.newCustomerPhone.isBlank() && viewModel.newCustomerName.isBlank()) {
            null
        } else {
            val key = normalizeKhataKey(viewModel.newCustomerName, viewModel.newCustomerPhone)
            khataCustomers.find { it.customerKey == key }
        }
    }

    BackHandler {
        viewModel.navigateBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.intake_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = shopColors.topBarContent
                        )
                        Text(
                            text = stringResource(R.string.intake_subtitle),
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
                            .testTag("new_job_back_btn")
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
                .padding(top = innerPadding.calculateTopPadding())
                .imePadding()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp), // Tablet & Counter centered constraint
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 1: Customer Contact
                item {
                    AppCard(testTag = "card_customer_info") {
                        Text(
                            text = stringResource(R.string.section_customer_info),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.section_customer_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        AppTextField(
                            value = viewModel.newCustomerName,
                            onValueChange = {
                                viewModel.newCustomerName = it
                                showCustomerSuggestions = it.isNotBlank()
                            },
                            label = stringResource(R.string.label_customer_name),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "input_customer_name"
                        )

                        if (showCustomerSuggestions && matchingCustomers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .testTag("card_customer_suggestions")
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = stringResource(R.string.khata_repeat_suggestions_title),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                    matchingCustomers.forEachIndexed { index, suggestion ->
                                        if (index > 0) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                        }
                                        val hasDue = suggestion.totalPendingDue > 0.01
                                        val cur = profile.currencySymbol.ifBlank { "₹" }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .defaultMinSize(minHeight = 48.dp)
                                                .clickable {
                                                    viewModel.newCustomerName = suggestion.customerName
                                                    if (suggestion.customerPhone.isNotBlank()) {
                                                        viewModel.newCustomerPhone = suggestion.customerPhone
                                                    }
                                                    showCustomerSuggestions = false
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                                .testTag("customer_suggestion_item_$index"),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = suggestion.customerName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${suggestion.customerPhone.ifBlank { stringResource(R.string.label_no_phone) }} • ${stringResource(R.string.khata_visits_count, suggestion.totalJobsCount)}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (hasDue) shopColors.alertContainer else shopColors.repairedContainer
                                            ) {
                                                Text(
                                                    text = if (hasDue) {
                                                        stringResource(
                                                            R.string.khata_due_badge,
                                                            "$cur${String.format(Locale.US, "%.0f", suggestion.totalPendingDue)}"
                                                        )
                                                    } else {
                                                        stringResource(R.string.khata_settled_badge)
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (hasDue) shopColors.alert else shopColors.repaired,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        AppTextField(
                            value = viewModel.newCustomerPhone,
                            onValueChange = { viewModel.newCustomerPhone = it },
                            label = stringResource(R.string.label_customer_phone),
                            placeholder = "10-digit mobile number",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "input_customer_phone"
                        )

                        if (matchedCustomerKhata != null && matchedCustomerKhata.totalPendingDue > 0.01) {
                            val cur = profile.currencySymbol.ifBlank { "₹" }
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = shopColors.alertContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("banner_prior_khata_due")
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.khata_prior_due_warning,
                                        "$cur${String.format(Locale.US, "%.2f", matchedCustomerKhata.totalPendingDue)}",
                                        matchedCustomerKhata.unpaidJobsCount
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = shopColors.alert,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Card 2: Device Specifications
                item {
                    AppCard(testTag = "card_device_specs") {
                        Text(
                            text = stringResource(R.string.section_device_specs),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.section_device_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = stringResource(R.string.label_device_brand),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            items(BRAND_LIST) { brand ->
                                FilterChip(
                                    selected = viewModel.newDeviceBrand.equals(brand, ignoreCase = true),
                                    onClick = { viewModel.newDeviceBrand = brand },
                                    label = { Text(brand, style = MaterialTheme.typography.bodyMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("brand_chip_$brand")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        AppTextField(
                            value = viewModel.newDeviceModel,
                            onValueChange = { viewModel.newDeviceModel = it },
                            label = stringResource(R.string.label_device_model),
                            placeholder = "e.g. Galaxy S23 / iPhone 14",
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "input_device_model"
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppTextField(
                                value = viewModel.newDeviceColor,
                                onValueChange = { viewModel.newDeviceColor = it },
                                label = stringResource(R.string.label_device_color),
                                placeholder = "e.g. Black",
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f),
                                testTag = "input_device_color"
                            )
                            AppTextField(
                                value = viewModel.newSecurityLockCode,
                                onValueChange = { viewModel.newSecurityLockCode = it },
                                label = stringResource(R.string.label_device_lock),
                                placeholder = "PIN / Pattern",
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f),
                                testTag = "input_device_lock"
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        AppTextField(
                            value = viewModel.newImeiOrSerial,
                            onValueChange = { viewModel.newImeiOrSerial = it },
                            label = stringResource(R.string.label_device_imei),
                            placeholder = "15-digit IMEI or Serial (optional)",
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "input_device_imei"
                        )
                    }
                }

                // Card 3: Reported Problem & Fault Tags
                item {
                    AppCard(testTag = "card_fault_info") {
                        Text(
                            text = stringResource(R.string.section_fault_info),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.section_fault_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            stringArrayResource(R.array.fault_tags).forEach { issue ->
                                val isSelected = viewModel.newSelectedIssueTags.contains(issue)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.toggleIssueTag(issue)
                                        if (viewModel.newIssueDescription.isBlank()) {
                                            viewModel.newIssueDescription = issue
                                        }
                                    },
                                    label = { Text(issue, style = MaterialTheme.typography.bodyMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("issue_chip_${issue.replace(" ", "_")}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        AppTextField(
                            value = viewModel.newIssueDescription,
                            onValueChange = { viewModel.newIssueDescription = it },
                            label = stringResource(R.string.label_fault_description),
                            singleLine = false,
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "input_issue_description"
                        )
                    }
                }

                // Card 4: 12-Point Condition Checklist
                item {
                    AppCard(testTag = "card_checklist") {
                        Text(
                            text = stringResource(R.string.section_checklist),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.section_checklist_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        InteractiveChecklistGrid(
                            checklist = viewModel.newChecklist,
                            onStatusChange = { key, status ->
                                viewModel.updateChecklistPoint(key, status)
                            }
                        )
                    }
                }

                // Card 5: Billing Quotation & Notes
                item {
                    AppCard(testTag = "card_billing") {
                        Text(
                            text = stringResource(R.string.section_billing),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.section_billing_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppTextField(
                                value = viewModel.newEstimatedCost,
                                onValueChange = { viewModel.newEstimatedCost = sanitizeDecimalInput(it) },
                                label = "${stringResource(R.string.label_estimated_cost)} (${profile.currencySymbol})",
                                placeholder = "0",
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.weight(1f),
                                testTag = "input_estimated_cost"
                            )
                            AppTextField(
                                value = viewModel.newAdvancePaid,
                                onValueChange = { viewModel.newAdvancePaid = sanitizeDecimalInput(it) },
                                label = "${stringResource(R.string.label_advance_paid)} (${profile.currencySymbol})",
                                placeholder = "0",
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.weight(1f),
                                testTag = "input_advance_paid"
                            )
                        }

                        val estVal = viewModel.newEstimatedCost.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
                        val advVal = viewModel.newAdvancePaid.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
                        val dueVal = (estVal - advVal).coerceAtLeast(0.0)

                        Spacer(modifier = Modifier.height(10.dp))
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
                                    text = "${profile.currencySymbol}${String.format(java.util.Locale.getDefault(), "%.2f", dueVal)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dueVal == 0.0 && estVal > 0.0) shopColors.repaired else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        AppTextField(
                            value = viewModel.newTechnicianNotes,
                            onValueChange = { viewModel.newTechnicianNotes = it },
                            label = stringResource(R.string.label_tech_notes),
                            placeholder = "Internal repair remarks...",
                            singleLine = false,
                            minLines = 2,
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "input_tech_notes"
                        )
                    }
                }

                // Action: Submit Button
                item {
                    AppButton(
                        text = stringResource(R.string.btn_create_job),
                        onClick = {
                            viewModel.submitNewJob(
                                onSuccess = { jobId ->
                                    Toast.makeText(context, context.getString(R.string.toast_job_created), Toast.LENGTH_SHORT).show()
                                    viewModel.navigateTo(AppScreen.JOB_DETAIL, jobId)
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        testTag = "submit_job_card_btn",
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Done, contentDescription = null)
                        }
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
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
