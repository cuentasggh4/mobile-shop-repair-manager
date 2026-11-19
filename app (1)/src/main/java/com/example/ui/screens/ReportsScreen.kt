package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.repository.ReportPeriod
import com.example.ui.ShopViewModel
import com.example.ui.StoreViewModel
import com.example.ui.theme.ShopTheme

@Composable
fun ReportsScreen(
    viewModel: ShopViewModel,
    store: StoreViewModel = viewModel()
) {
    val period by store.reportPeriod.collectAsStateWithLifecycle()
    val summary by store.periodSummary.collectAsStateWithLifecycle()
    val parts by store.parts.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val currency = shopProfile.currencySymbol.ifBlank { "$" }
    val colors = ShopTheme.colors
    val context = LocalContext.current
    val lowStock = parts.filter { it.isLowStock }.take(10)

    val periodLabel = when (period) {
        ReportPeriod.TODAY -> stringResource(R.string.rep_today)
        ReportPeriod.LAST_7_DAYS -> stringResource(R.string.rep_7_days)
        ReportPeriod.LAST_30_DAYS -> stringResource(R.string.rep_30_days)
    }

    StoreScaffold(
        title = stringResource(R.string.rep_title),
        subtitle = periodLabel,
        onBack = { viewModel.navigateBack() },
        actions = {
            IconButton(onClick = {
                val text = buildString {
                    appendLine("${shopProfile.shopName} — ${context.getString(R.string.rep_title)} ($periodLabel)")
                    appendLine("${context.getString(R.string.rep_sales)}: ${formatMoney(currency, summary.salesTotal)} (${summary.salesCount})")
                    appendLine("${context.getString(R.string.rep_repairs)}: ${formatMoney(currency, summary.repairsTotal)} (${summary.repairsCount})")
                    appendLine("${context.getString(R.string.rep_revenue)}: ${formatMoney(currency, summary.revenue)}")
                    appendLine("${context.getString(R.string.rep_expenses)}: ${formatMoney(currency, summary.expenses)}")
                    appendLine("${context.getString(R.string.rep_purchases)}: ${formatMoney(currency, summary.purchasesTotal)}")
                    appendLine("${context.getString(R.string.rep_profit)}: ${formatMoney(currency, summary.profit)}")
                }
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(send, null))
            }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(R.string.rep_share),
                    tint = colors.topBarContent
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = period == ReportPeriod.TODAY,
                            onClick = { store.onReportPeriodChanged(ReportPeriod.TODAY) },
                            label = { Text(stringResource(R.string.rep_today)) }
                        )
                        FilterChip(
                            selected = period == ReportPeriod.LAST_7_DAYS,
                            onClick = { store.onReportPeriodChanged(ReportPeriod.LAST_7_DAYS) },
                            label = { Text(stringResource(R.string.rep_7_days)) }
                        )
                        FilterChip(
                            selected = period == ReportPeriod.LAST_30_DAYS,
                            onClick = { store.onReportPeriodChanged(ReportPeriod.LAST_30_DAYS) },
                            label = { Text(stringResource(R.string.rep_30_days)) }
                        )
                    }
                }

                item {
                    StoreCard {
                        Text(
                            text = stringResource(R.string.rep_profit),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatMoney(currency, summary.profit),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (summary.profit >= 0.0) colors.repaired else colors.alert
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.cash_repairs_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    StoreCard {
                        MetricRow(
                            label = stringResource(R.string.rep_sales),
                            detail = stringResource(R.string.rep_count_sales, summary.salesCount),
                            value = formatMoney(currency, summary.salesTotal)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MetricRow(
                            label = stringResource(R.string.rep_repairs),
                            detail = stringResource(R.string.rep_count_repairs, summary.repairsCount),
                            value = formatMoney(currency, summary.repairsTotal)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MetricRow(
                            label = stringResource(R.string.rep_revenue),
                            detail = null,
                            value = formatMoney(currency, summary.revenue),
                            bold = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MetricRow(
                            label = stringResource(R.string.rep_expenses),
                            detail = null,
                            value = formatMoney(currency, summary.expenses),
                            valueColor = colors.alert
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MetricRow(
                            label = stringResource(R.string.rep_purchases),
                            detail = null,
                            value = formatMoney(currency, summary.purchasesTotal)
                        )
                    }
                }

                item {
                    StoreCard {
                        Text(
                            text = stringResource(R.string.rep_top_items),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (summary.topItems.isEmpty()) {
                            Text(
                                text = stringResource(R.string.rep_no_data),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        summary.topItems.forEach { item ->
                            MetricRow(
                                label = item.name,
                                detail = "× ${item.quantity}",
                                value = formatMoney(currency, item.revenue)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }

                if (lowStock.isNotEmpty()) {
                    item {
                        StoreCard {
                            Text(
                                text = stringResource(R.string.rep_low_stock),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.alert
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            lowStock.forEach { part ->
                                MetricRow(
                                    label = part.partName,
                                    detail = null,
                                    value = "${part.quantityInStock} / ${part.minStockThreshold}",
                                    valueColor = colors.alert
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    detail: String?,
    value: String,
    bold: Boolean = false,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
