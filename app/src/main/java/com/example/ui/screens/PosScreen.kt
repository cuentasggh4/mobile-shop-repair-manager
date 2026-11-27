package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.entity.InventoryPartEntity
import com.example.ui.ShopViewModel
import com.example.ui.StoreViewModel
import com.example.ui.theme.ShopTheme

@Composable
fun PosScreen(
    viewModel: ShopViewModel,
    store: StoreViewModel = viewModel()
) {
    val parts by store.parts.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val currency = shopProfile.currencySymbol.ifBlank { "$" }
    val colors = ShopTheme.colors
    val context = LocalContext.current
    var showCustomDialog by remember { mutableStateOf(false) }

    StoreMessageEffect(store)

    val query = store.posQuery
    val visibleParts = remember(parts, query) {
        val needle = query.trim().lowercase()
        val matches = if (needle.isEmpty()) {
            parts
        } else {
            parts.filter {
                it.partName.lowercase().contains(needle) ||
                    it.category.lowercase().contains(needle) ||
                    it.compatibleModels.lowercase().contains(needle) ||
                    it.barcode.lowercase() == needle
            }
        }
        matches.take(60)
    }

    StoreScaffold(
        title = stringResource(R.string.store_pos),
        subtitle = stringResource(R.string.store_pos_desc),
        onBack = { viewModel.navigateBack() }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Product picker
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = store.posQuery,
                        onValueChange = { store.posQuery = it },
                        placeholder = { Text(stringResource(R.string.pos_search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (store.posQuery.isNotEmpty()) {
                                    IconButton(onClick = { store.posQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        com.example.util.CodeScanner.scan(
                                            context,
                                            onResult = { store.scanBarcode(it) },
                                            onError = {}
                                        )
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = stringResource(R.string.scan_code),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = { showCustomDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (visibleParts.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.pos_no_products),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }
                    items(visibleParts, key = { it.id }) { part ->
                        ProductRow(
                            part = part,
                            currency = currency,
                            alertColor = colors.alert,
                            onClick = { store.addPartToCart(part) }
                        )
                    }
                }
            }

            // Cart panel
            HorizontalDivider()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.pos_cart_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (store.cart.isNotEmpty()) {
                        TextButton(onClick = { store.clearCart() }) {
                            Text(stringResource(R.string.pos_clear))
                        }
                    }
                }

                if (store.cart.isEmpty()) {
                    Text(
                        text = stringResource(R.string.pos_cart_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                store.cart.forEach { line ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = line.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatMoney(currency, line.unitPrice)} × ${line.quantity} = ${formatMoney(currency, line.lineTotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { store.changeCartQuantity(line, -1) }) {
                            Icon(Icons.Default.Remove, contentDescription = "-")
                        }
                        Text(text = line.quantity.toString(), fontWeight = FontWeight.Bold)
                        IconButton(onClick = { store.changeCartQuantity(line, 1) }) {
                            Icon(Icons.Default.Add, contentDescription = "+")
                        }
                        IconButton(onClick = { store.removeFromCart(line) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.common_delete),
                                tint = colors.alert
                            )
                        }
                    }
                }

                if (store.cart.isNotEmpty()) {
                    HorizontalDivider()
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(PAYMENT_MODES) { mode ->
                            FilterChip(
                                selected = store.cartPaymentMode == mode,
                                onClick = { store.cartPaymentMode = mode },
                                label = { Text(paymentModeLabel(mode)) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StoreField(
                            value = store.cartDiscountText,
                            onValueChange = { store.cartDiscountText = sanitizeDecimal(it) },
                            label = "${stringResource(R.string.pos_discount)} ($currency)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        StoreField(
                            value = store.cartCustomerName,
                            onValueChange = { store.cartCustomerName = it },
                            label = stringResource(R.string.pos_customer),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.pos_subtotal),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatMoney(currency, store.cartSubtotal),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.pos_total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatMoney(currency, store.cartTotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Button(
                        onClick = {
                            store.checkout { total ->
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.pos_sale_done, formatMoney(currency, total)),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = "${stringResource(R.string.pos_charge)} ${formatMoney(currency, store.cartTotal)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomItemDialog(
            currency = currency,
            onDismiss = { showCustomDialog = false },
            onAdd = { name, price ->
                store.addCustomItemToCart(name, price)
                showCustomDialog = false
            }
        )
    }
}

@Composable
fun paymentModeLabel(mode: String): String = when (mode) {
    "Cash" -> stringResource(R.string.pay_cash)
    "Card" -> stringResource(R.string.pay_card)
    "Transfer" -> stringResource(R.string.pay_transfer)
    "UPI" -> stringResource(R.string.pay_upi)
    else -> mode
}

@Composable
private fun ProductRow(
    part: InventoryPartEntity,
    currency: String,
    alertColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val outOfStock = part.quantityInStock <= 0
    StoreCard(onClick = if (outOfStock) null else onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = part.partName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (outOfStock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.inv_in_stock, part.quantityInStock),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (outOfStock || part.isLowStock) alertColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatMoney(currency, part.sellingPrice),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun CustomItemDialog(
    currency: String,
    onDismiss: () -> Unit,
    onAdd: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    val parsed = parseDecimal(price) ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pos_custom_item)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StoreField(value = name, onValueChange = { name = it }, label = stringResource(R.string.pos_custom_name))
                StoreField(
                    value = price,
                    onValueChange = { price = sanitizeDecimal(it) },
                    label = "${stringResource(R.string.pos_custom_price)} ($currency)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && parsed > 0.0,
                onClick = { onAdd(name, parsed) }
            ) { Text(stringResource(R.string.common_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
