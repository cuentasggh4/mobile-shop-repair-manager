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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.entity.CashMovementEntity
import com.example.data.entity.SaleEntity
import com.example.ui.ShopViewModel
import com.example.ui.StoreViewModel
import com.example.ui.theme.ShopTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CashRegisterScreen(
    viewModel: ShopViewModel,
    store: StoreViewModel = viewModel()
) {
    val summary by store.todaySummary.collectAsStateWithLifecycle()
    val sales by store.todaySales.collectAsStateWithLifecycle()
    val movements by store.todayMovements.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val currency = shopProfile.currencySymbol.ifBlank { "$" }
    val colors = ShopTheme.colors
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    var movementType by remember { mutableStateOf<String?>(null) }
    var voidingSale by remember { mutableStateOf<SaleEntity?>(null) }

    StoreMessageEffect(store)

    StoreScaffold(
        title = stringResource(R.string.cash_title),
        subtitle = dateFormat.format(Date()),
        onBack = { viewModel.navigateBack() },
        actions = {
            IconButton(onClick = {
                val text = buildString {
                    appendLine("${shopProfile.shopName} — ${stringResource0(context, R.string.cash_title)} ${dateFormat.format(Date())}")
                    appendLine("${stringResource0(context, R.string.cash_expected)}: ${formatMoney(currency, summary.expectedCash)}")
                    summary.byPaymentMode.forEach { (mode, amount) ->
                        appendLine("  $mode: ${formatMoney(currency, amount)}")
                    }
                    appendLine("${stringResource0(context, R.string.rep_sales)}: ${formatMoney(currency, summary.salesTotal)} (${summary.salesCount})")
                    appendLine("${stringResource0(context, R.string.rep_repairs)}: ${formatMoney(currency, summary.repairsTotal)} (${summary.repairsCount})")
                    appendLine("${stringResource0(context, R.string.cash_add_in)}: ${formatMoney(currency, summary.cashIn)}")
                    appendLine("${stringResource0(context, R.string.cash_add_expense)}: ${formatMoney(currency, summary.expenses)}")
                }
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(send, null))
            }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(R.string.cash_share_cut),
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
                    StoreCard {
                        Text(
                            text = stringResource(R.string.cash_expected),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatMoney(currency, summary.expectedCash),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.repaired
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.cash_today_breakdown),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (summary.byPaymentMode.isEmpty()) {
                            Text(
                                text = stringResource(R.string.rep_no_data),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        summary.byPaymentMode.forEach { (mode, amount) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = paymentModeLabel(mode), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = formatMoney(currency, amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.cash_repairs_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { movementType = "IN" },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.cash_add_in)) }
                        OutlinedButton(
                            onClick = { movementType = "EXPENSE" },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.cash_add_expense)) }
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.cash_movements),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (movements.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.cash_no_movements),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(movements, key = { "m${it.id}" }) { m ->
                    StoreCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (m.type == "IN") stringResource(R.string.cash_add_in)
                                    else stringResource(R.string.cash_add_expense),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${timeFormat.format(Date(m.createdAt))} ${m.note}".trim(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = (if (m.type == "IN") "+" else "−") + formatMoney(currency, m.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (m.type == "IN") colors.repaired else colors.alert
                            )
                            IconButton(onClick = { store.deleteMovement(m) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.common_delete),
                                    tint = colors.alert
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.cash_sales_today),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (sales.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.cash_no_sales),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(sales, key = { "s${it.id}" }) { sale ->
                    StoreCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "#${sale.id} · ${timeFormat.format(Date(sale.createdAt))}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = listOf(paymentModeLabelPlain(sale.paymentMode), sale.customerName)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = formatMoney(currency, sale.total),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { voidingSale = sale }) {
                                Text(stringResource(R.string.cash_void), color = colors.alert)
                            }
                        }
                    }
                }
            }
        }
    }

    movementType?.let { type ->
        MovementDialog(
            isExpense = type == "EXPENSE",
            currency = currency,
            onDismiss = { movementType = null },
            onSave = { amount, note ->
                store.addMovement(type, amount, note)
                movementType = null
            }
        )
    }

    voidingSale?.let { sale ->
        AlertDialog(
            onDismissRequest = { voidingSale = null },
            title = { Text(stringResource(R.string.cash_void_title)) },
            text = { Text(stringResource(R.string.cash_void_body)) },
            confirmButton = {
                TextButton(onClick = {
                    store.voidSale(sale)
                    voidingSale = null
                }) { Text(stringResource(R.string.cash_void), color = colors.alert) }
            },
            dismissButton = {
                TextButton(onClick = { voidingSale = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

private fun stringResource0(context: android.content.Context, id: Int): String = context.getString(id)

private fun paymentModeLabelPlain(mode: String): String = mode

@Composable
private fun MovementDialog(
    isExpense: Boolean,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val parsed = parseDecimal(amount) ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isExpense) stringResource(R.string.cash_add_expense) else stringResource(R.string.cash_add_in)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StoreField(
                    value = amount,
                    onValueChange = { amount = sanitizeDecimal(it) },
                    label = "${stringResource(R.string.cash_field_amount)} ($currency)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                StoreField(
                    value = note,
                    onValueChange = { note = it },
                    label = stringResource(R.string.cash_field_note)
                )
            }
        },
        confirmButton = {
            TextButton(enabled = parsed > 0.0, onClick = { onSave(parsed, note) }) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
