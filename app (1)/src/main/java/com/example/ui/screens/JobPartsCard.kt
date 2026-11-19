package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.entity.JobCardEntity
import com.example.ui.StoreViewModel
import com.example.ui.theme.ShopTheme

/**
 * "Parts used" card for the job detail screen. Taking a part here removes it from stock,
 * and the cost shows up in the profit report once the job is paid.
 */
@Composable
fun JobPartsCard(
    job: JobCardEntity,
    currency: String,
    store: StoreViewModel = viewModel()
) {
    val parts by store.parts.collectAsStateWithLifecycle()
    val colors = ShopTheme.colors
    var showPicker by remember { mutableStateOf(false) }

    val usedCounts = remember(job.linkedPartIdsCsv) {
        job.linkedPartIdsCsv.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .groupingBy { it }
            .eachCount()
    }

    StoreMessageEffect(store)

    StoreCard {
        Text(
            text = stringResource(R.string.job_parts_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (usedCounts.isEmpty()) {
            Text(
                text = stringResource(R.string.job_parts_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        usedCounts.forEach { (partId, count) ->
            val part = parts.find { it.id == partId }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${part?.partName ?: "#$partId"} × $count",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (part != null) {
                        Text(
                            text = formatMoney(currency, part.purchaseCost * count),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = { store.returnPartFromJob(job.id, partId) }) {
                    Text(stringResource(R.string.job_parts_remove), color = colors.alert)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { showPicker = true },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.height(0.dp))
            Text(stringResource(R.string.job_parts_add))
        }
    }

    if (showPicker) {
        var query by remember { mutableStateOf("") }
        val matches = remember(parts, query) {
            val needle = query.trim().lowercase()
            parts.filter {
                it.quantityInStock > 0 && (
                    needle.isEmpty() ||
                        it.partName.lowercase().contains(needle) ||
                        it.compatibleModels.lowercase().contains(needle) ||
                        it.category.lowercase().contains(needle)
                    )
            }.take(40)
        }
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(stringResource(R.string.job_parts_pick)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.inv_search_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(matches, key = { it.id }) { part ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = part.partName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(R.string.inv_in_stock, part.quantityInStock),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { store.usePartOnJob(job.id, part.id, 1) }) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.common_add))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.common_back)) }
            }
        )
    }
}
