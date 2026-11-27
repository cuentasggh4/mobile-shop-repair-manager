package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.entity.InventoryPartEntity
import com.example.data.entity.SupplierEntity
import com.example.ui.ShopViewModel
import com.example.ui.StoreViewModel
import com.example.ui.theme.ShopTheme

private val PART_CATEGORIES = listOf(
    "Screen", "Battery", "Charging Port", "Camera", "Housing", "Flex Cable", "Accessory", "Other"
)

@Composable
fun InventoryScreen(
    viewModel: ShopViewModel,
    store: StoreViewModel = viewModel()
) {
    val parts by store.filteredParts.collectAsStateWithLifecycle()
    val allParts by store.parts.collectAsStateWithLifecycle()
    val suppliers by store.suppliers.collectAsStateWithLifecycle()
    val query by store.partQuery.collectAsStateWithLifecycle()
    val onlyLow by store.onlyLowStock.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()
    val currency = shopProfile.currencySymbol.ifBlank { "$" }
    val colors = ShopTheme.colors

    var tab by remember { mutableStateOf(0) }
    var editingPart by remember { mutableStateOf<InventoryPartEntity?>(null) }
    var showPartDialog by remember { mutableStateOf(false) }
    var receivingPart by remember { mutableStateOf<InventoryPartEntity?>(null) }
    var deletingPart by remember { mutableStateOf<InventoryPartEntity?>(null) }
    var editingSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var showSupplierDialog by remember { mutableStateOf(false) }
    var deletingSupplier by remember { mutableStateOf<SupplierEntity?>(null) }

    StoreMessageEffect(store)

    StoreScaffold(
        title = stringResource(R.string.store_inventory),
        subtitle = stringResource(R.string.store_inventory_desc),
        onBack = { viewModel.navigateBack() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (tab == 0) {
                        editingPart = null
                        showPartDialog = true
                    } else {
                        editingSupplier = null
                        showSupplierDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = androidx.compose.ui.graphics.Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.common_add))
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
            TabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text("${stringResource(R.string.inv_tab_parts)} (${allParts.size})") }
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text("${stringResource(R.string.inv_tab_suppliers)} (${suppliers.size})") }
                )
            }

            if (tab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { store.onPartQueryChanged(it) },
                            placeholder = { Text(stringResource(R.string.inv_search_hint)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { store.onPartQueryChanged("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !onlyLow,
                                onClick = { store.onOnlyLowStockChanged(false) },
                                label = { Text(stringResource(R.string.inv_filter_all)) }
                            )
                            FilterChip(
                                selected = onlyLow,
                                onClick = { store.onOnlyLowStockChanged(true) },
                                label = { Text(stringResource(R.string.inv_filter_low)) }
                            )
                        }
                    }
                    if (parts.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.inv_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    }
                    items(parts, key = { it.id }) { part ->
                        PartCard(
                            part = part,
                            currency = currency,
                            alertColor = colors.alert,
                            onEdit = {
                                editingPart = part
                                showPartDialog = true
                            },
                            onReceive = { receivingPart = part },
                            onDelete = { deletingPart = part }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (suppliers.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.sup_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    }
                    items(suppliers, key = { it.id }) { supplier ->
                        StoreCard(onClick = {
                            editingSupplier = supplier
                            showSupplierDialog = true
                        }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = supplier.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (supplier.phone.isNotBlank()) {
                                        Text(
                                            text = supplier.phone,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (supplier.notes.isNotBlank()) {
                                        Text(
                                            text = supplier.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(onClick = { deletingSupplier = supplier }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.common_delete),
                                        tint = colors.alert
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPartDialog) {
        PartDialog(
            part = editingPart,
            onDismiss = { showPartDialog = false },
            onSave = {
                store.savePart(it)
                showPartDialog = false
            }
        )
    }

    receivingPart?.let { part ->
        ReceiveStockDialog(
            part = part,
            suppliers = suppliers,
            currency = currency,
            onDismiss = { receivingPart = null },
            onConfirm = { supplier, qty, cost ->
                store.receiveStock(part.id, supplier, qty, cost)
                receivingPart = null
            }
        )
    }

    deletingPart?.let { part ->
        AlertDialog(
            onDismissRequest = { deletingPart = null },
            title = { Text(stringResource(R.string.inv_delete_part_title)) },
            text = { Text(stringResource(R.string.inv_delete_part_body, part.partName)) },
            confirmButton = {
                TextButton(onClick = {
                    store.deletePart(part)
                    deletingPart = null
                }) { Text(stringResource(R.string.common_delete), color = colors.alert) }
            },
            dismissButton = {
                TextButton(onClick = { deletingPart = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    if (showSupplierDialog) {
        SupplierDialog(
            supplier = editingSupplier,
            onDismiss = { showSupplierDialog = false },
            onSave = {
                store.saveSupplier(it)
                showSupplierDialog = false
            }
        )
    }

    deletingSupplier?.let { supplier ->
        AlertDialog(
            onDismissRequest = { deletingSupplier = null },
            title = { Text(stringResource(R.string.sup_delete_title)) },
            text = { Text(supplier.name) },
            confirmButton = {
                TextButton(onClick = {
                    store.deleteSupplier(supplier)
                    deletingSupplier = null
                }) { Text(stringResource(R.string.common_delete), color = colors.alert) }
            },
            dismissButton = {
                TextButton(onClick = { deletingSupplier = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun PartCard(
    part: InventoryPartEntity,
    currency: String,
    alertColor: androidx.compose.ui.graphics.Color,
    onEdit: () -> Unit,
    onReceive: () -> Unit,
    onDelete: () -> Unit
) {
    StoreCard(onClick = onEdit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = part.partName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = listOf(part.category, part.compatibleModels)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatMoney(currency, part.sellingPrice),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                if (part.sellingPrice > 0.0) {
                    val margin = (part.sellingPrice - part.purchaseCost) / part.sellingPrice * 100.0
                    Text(
                        text = stringResource(R.string.inv_margin, "${margin.toInt()}%"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (part.isLowStock) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = alertColor,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            Text(
                text = stringResource(R.string.inv_in_stock, part.quantityInStock) +
                    if (part.isLowStock) " · ${stringResource(R.string.inv_low_stock)}" else "",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (part.isLowStock) alertColor else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onReceive) { Text(stringResource(R.string.inv_receive_stock)) }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = alertColor
                )
            }
        }
    }
}

@Composable
private fun PartDialog(
    part: InventoryPartEntity?,
    onDismiss: () -> Unit,
    onSave: (InventoryPartEntity) -> Unit
) {
    var name by remember { mutableStateOf(part?.partName ?: "") }
    var category by remember { mutableStateOf(part?.category ?: "Screen") }
    var models by remember { mutableStateOf(part?.compatibleModels ?: "") }
    var stock by remember { mutableStateOf((part?.quantityInStock ?: 0).toString()) }
    var minStock by remember { mutableStateOf((part?.minStockThreshold ?: 3).toString()) }
    var cost by remember { mutableStateOf(part?.purchaseCost?.let { trimNumber(it) } ?: "") }
    var price by remember { mutableStateOf(part?.sellingPrice?.let { trimNumber(it) } ?: "") }
    var location by remember { mutableStateOf(part?.locationRack ?: "") }
    var barcode by remember { mutableStateOf(part?.barcode ?: "") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (part == null) stringResource(R.string.inv_add_part) else stringResource(R.string.inv_edit_part)
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StoreField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = stringResource(R.string.inv_field_name)
                )
                if (nameError) {
                    Text(
                        text = stringResource(R.string.inv_field_name),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PART_CATEGORIES) { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c) }
                        )
                    }
                }
                StoreField(
                    value = category,
                    onValueChange = { category = it },
                    label = stringResource(R.string.inv_field_category)
                )
                StoreField(
                    value = models,
                    onValueChange = { models = it },
                    label = stringResource(R.string.inv_field_models)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StoreField(
                        value = stock,
                        onValueChange = { stock = it.filter { ch -> ch.isDigit() } },
                        label = stringResource(R.string.inv_field_stock),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    StoreField(
                        value = minStock,
                        onValueChange = { minStock = it.filter { ch -> ch.isDigit() } },
                        label = stringResource(R.string.inv_field_min_stock),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StoreField(
                        value = cost,
                        onValueChange = { cost = sanitizeDecimal(it) },
                        label = stringResource(R.string.inv_field_cost),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    StoreField(
                        value = price,
                        onValueChange = { price = sanitizeDecimal(it) },
                        label = stringResource(R.string.inv_field_price),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                StoreField(
                    value = location,
                    onValueChange = { location = it },
                    label = stringResource(R.string.inv_field_location)
                )
                val scanContext = androidx.compose.ui.platform.LocalContext.current
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StoreField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = stringResource(R.string.inv_field_barcode),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            com.example.util.CodeScanner.scan(scanContext, onResult = { barcode = it }, onError = {})
                        }
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.scan_code),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    nameError = true
                } else {
                    val base = part ?: InventoryPartEntity(
                        partName = "",
                        category = "",
                        compatibleModels = "",
                        quantityInStock = 0,
                        purchaseCost = 0.0,
                        sellingPrice = 0.0
                    )
                    onSave(
                        base.copy(
                            partName = name.trim(),
                            category = category.trim().ifBlank { "Other" },
                            compatibleModels = models.trim(),
                            quantityInStock = stock.toIntOrNull() ?: 0,
                            minStockThreshold = minStock.toIntOrNull() ?: 3,
                            purchaseCost = parseDecimal(cost) ?: 0.0,
                            sellingPrice = parseDecimal(price) ?: 0.0,
                            locationRack = location.trim(),
                            barcode = barcode.trim()
                        )
                    )
                }
            }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun ReceiveStockDialog(
    part: InventoryPartEntity,
    suppliers: List<SupplierEntity>,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (SupplierEntity?, Int, Double) -> Unit
) {
    var quantity by remember { mutableStateOf("") }
    var unitCost by remember { mutableStateOf(trimNumber(part.purchaseCost)) }
    var supplierId by remember { mutableStateOf(part.supplierId) }

    val qty = quantity.toIntOrNull() ?: 0
    val cost = parseDecimal(unitCost) ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.inv_receive_title, part.partName)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StoreField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { ch -> ch.isDigit() } },
                    label = stringResource(R.string.inv_field_quantity),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                StoreField(
                    value = unitCost,
                    onValueChange = { unitCost = sanitizeDecimal(it) },
                    label = "${stringResource(R.string.inv_field_unit_cost)} ($currency)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                if (suppliers.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = supplierId == 0L,
                                onClick = { supplierId = 0L },
                                label = { Text(stringResource(R.string.inv_supplier_none)) }
                            )
                        }
                        items(suppliers, key = { it.id }) { s ->
                            FilterChip(
                                selected = supplierId == s.id,
                                onClick = { supplierId = s.id },
                                label = { Text(s.name) }
                            )
                        }
                    }
                }
                if (qty > 0) {
                    Text(
                        text = "${qty} × ${formatMoney(currency, cost)} = ${formatMoney(currency, qty * cost)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = qty > 0,
                onClick = { onConfirm(suppliers.find { it.id == supplierId }, qty, cost) }
            ) { Text(stringResource(R.string.inv_receive_stock)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun SupplierDialog(
    supplier: SupplierEntity?,
    onDismiss: () -> Unit,
    onSave: (SupplierEntity) -> Unit
) {
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }
    var notes by remember { mutableStateOf(supplier?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (supplier == null) stringResource(R.string.sup_add) else stringResource(R.string.sup_edit))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StoreField(value = name, onValueChange = { name = it }, label = stringResource(R.string.sup_field_name))
                StoreField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = stringResource(R.string.sup_field_phone),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                StoreField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = stringResource(R.string.sup_field_notes),
                    singleLine = false
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val base = supplier ?: SupplierEntity(name = "")
                    onSave(base.copy(name = name.trim(), phone = phone.trim(), notes = notes.trim()))
                }
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

/** 12.0 -> "12", 12.5 -> "12.5" (for editable fields). */
internal fun trimNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
