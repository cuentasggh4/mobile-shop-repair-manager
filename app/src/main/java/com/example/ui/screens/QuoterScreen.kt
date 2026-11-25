package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.ImportState
import com.example.ui.QuoterViewModel
import com.example.ui.ShopViewModel
import com.example.ui.priceOf
import com.example.util.ScreenTariff
import com.example.ui.theme.ShopTheme

@Composable
fun QuoterScreen(
    viewModel: ShopViewModel,
    quoter: QuoterViewModel = viewModel()
) {
    val allItems by quoter.allItems.collectAsStateWithLifecycle()
    val results by quoter.results.collectAsStateWithLifecycle()
    val categories by quoter.categories.collectAsStateWithLifecycle()
    val selectedCategory by quoter.category.collectAsStateWithLifecycle()
    val query by quoter.query.collectAsStateWithLifecycle()
    val importState by quoter.importState.collectAsStateWithLifecycle()
    val message by quoter.message.collectAsStateWithLifecycle()
    val marginText by quoter.marginText.collectAsStateWithLifecycle()
    val laborText by quoter.laborText.collectAsStateWithLifecycle()
    val customerName by quoter.customerName.collectAsStateWithLifecycle()
    val lines by quoter.lines.collectAsStateWithLifecycle()
    val discount by quoter.discountPercent.collectAsStateWithLifecycle()
    val marketplace by quoter.marketplaceQuote.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val currency = shopProfile.currencySymbol.ifBlank { "$" }
    val colors = ShopTheme.colors
    val context = LocalContext.current

    var replaceExisting by remember { mutableStateOf(true) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) quoter.importPdf(uri, replaceExisting)
    }

    LaunchedEffect(message) {
        val text = message
        if (text != null) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            quoter.consumeMessage()
        }
    }

    val margin = quoter.marginPercent()
    val labor = parseDecimal(laborText) ?: 0.0
    val partsTotal = lines.sumOf { priceOf(it.category, it.cost, margin, discount, marketplace) * it.qty }
    val total = partsTotal + labor

    StoreScaffold(
        title = stringResource(R.string.q_title),
        subtitle = stringResource(R.string.q_subtitle),
        onBack = { viewModel.navigateBack() },
        actions = {
            IconButton(onClick = { picker.launch(arrayOf("application/pdf")) }) {
                Icon(
                    Icons.Default.UploadFile,
                    contentDescription = stringResource(R.string.q_import_pdf),
                    tint = ShopTheme.colors.topBarContent
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // --- Import ---
                item {
                    StoreCard {
                        Text(
                            text = stringResource(R.string.q_catalog_count, allItems.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val source = allItems.firstOrNull { it.source.isNotBlank() }?.source
                        if (!source.isNullOrBlank()) {
                            Text(
                                text = source,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = replaceExisting, onCheckedChange = { replaceExisting = it })
                            Text(
                                text = stringResource(R.string.q_replace_list),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { picker.launch(arrayOf("application/pdf")) },
                                enabled = importState != ImportState.Working,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.q_import_pdf), maxLines = 1)
                            }
                            if (allItems.isNotEmpty()) {
                                OutlinedButton(onClick = { quoter.clearCatalog() }) {
                                    Text(stringResource(R.string.q_clear_list), maxLines = 1)
                                }
                            }
                        }
                        when (val st = importState) {
                            ImportState.Working -> {
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(stringResource(R.string.q_importing), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            is ImportState.Done -> {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.q_import_done, st.count, st.skipped),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.repaired
                                )
                            }
                            is ImportState.Failed -> {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = st.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.alert
                                )
                            }
                            ImportState.Idle -> Unit
                        }
                        if (allItems.isEmpty() && importState == ImportState.Idle) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.q_empty_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // --- Quote ---
                item {
                    StoreCard {
                        Text(
                            text = stringResource(R.string.q_quote_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        StoreField(
                            value = customerName,
                            onValueChange = { quoter.customerName.value = it },
                            label = stringResource(R.string.q_customer)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StoreField(
                                value = marginText,
                                onValueChange = { quoter.setMargin(sanitizeDecimal(it)) },
                                label = stringResource(R.string.q_margin),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            StoreField(
                                value = laborText,
                                onValueChange = { quoter.laborText.value = sanitizeDecimal(it) },
                                label = stringResource(R.string.q_labor),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = marketplace,
                                onCheckedChange = { quoter.marketplaceQuote.value = it }
                            )
                            Text(
                                text = stringResource(R.string.q_marketplace),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = stringResource(R.string.q_discount_title),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ScreenTariff.DISCOUNTS) { pct ->
                                FilterChip(
                                    selected = (if (marketplace) 0 else discount) == pct,
                                    enabled = !marketplace,
                                    onClick = { quoter.discountPercent.value = pct },
                                    label = {
                                        Text(
                                            when (pct) {
                                                0 -> stringResource(R.string.q_no_discount)
                                                25 -> "25% " + stringResource(R.string.q_max)
                                                else -> "$pct%"
                                            }
                                        )
                                    }
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.q_tariff_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        if (lines.isEmpty()) {
                            Text(
                                text = stringResource(R.string.q_quote_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            lines.forEach { line ->
                                val unit = priceOf(line.category, line.cost, margin, discount, marketplace)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(line.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                                        Text(
                                            text = formatMoney(currency, unit) + " × " + line.qty,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { quoter.changeQty(line.itemId, -1) }) {
                                        Icon(Icons.Default.Remove, contentDescription = null)
                                    }
                                    Text(line.qty.toString(), fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { quoter.changeQty(line.itemId, 1) }) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            QuoteRow(stringResource(R.string.q_parts_total), formatMoney(currency, partsTotal))
                            QuoteRow(stringResource(R.string.q_labor), formatMoney(currency, labor))
                            QuoteRow(stringResource(R.string.q_total), formatMoney(currency, total), bold = true)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val text = buildQuoteText(
                                            context = context,
                                            shop = shopProfile.shopName,
                                            customer = customerName,
                                            currency = currency,
                                            lines = lines.map { Triple(it.name, priceOf(it.category, it.cost, margin, discount, marketplace), it.qty) },
                                            labor = labor,
                                            total = total
                                        )
                                        val send = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, text)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(send, context.getString(R.string.q_share)).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.q_share), maxLines = 1)
                                }
                                OutlinedButton(onClick = { quoter.clearQuote() }) {
                                    Text(stringResource(R.string.q_clear_quote), maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // --- Search ---
                item {
                    StoreField(
                        value = query,
                        onValueChange = { quoter.query.value = it },
                        label = stringResource(R.string.q_search_hint)
                    )
                }
                if (categories.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = { quoter.category.value = null },
                                    label = { Text(stringResource(R.string.q_all)) }
                                )
                            }
                            items(categories) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { quoter.category.value = if (selectedCategory == cat) null else cat },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    }
                }

                // --- Results ---
                if (allItems.isNotEmpty() && results.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.q_no_results),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                items(results, key = { "p${it.id}" }) { item ->
                    StoreCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 3
                                )
                                Text(
                                    text = stringResource(
                                        R.string.q_cost_price,
                                        formatMoney(currency, item.cost),
                                        formatMoney(currency, priceOf(item.category, item.cost, margin, discount, marketplace))
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { quoter.addToInventory(item) }) {
                                Icon(
                                    Icons.Default.Inventory2,
                                    contentDescription = stringResource(R.string.q_to_inventory),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { quoter.addLine(item) }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = stringResource(R.string.q_add_to_quote),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuoteRow(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

private fun buildQuoteText(
    context: android.content.Context,
    shop: String,
    customer: String,
    currency: String,
    lines: List<Triple<String, Double, Int>>,
    labor: Double,
    total: Double
): String {
    val sb = StringBuilder()
    sb.append("*").append(shop).append("* - ").append(context.getString(R.string.q_quote_title)).append("\n")
    if (customer.isNotBlank()) sb.append(context.getString(R.string.q_customer)).append(": ").append(customer.trim()).append("\n")
    sb.append("\n")
    lines.forEach { (name, unit, qty) ->
        sb.append("• ").append(name).append(" × ").append(qty).append(": ")
            .append(formatMoney(currency, unit * qty)).append("\n")
    }
    if (labor > 0.0) {
        sb.append(context.getString(R.string.q_labor)).append(": ").append(formatMoney(currency, labor)).append("\n")
    }
    sb.append("\n*").append(context.getString(R.string.q_total)).append(": ").append(formatMoney(currency, total)).append("*")
    return sb.toString()
}
