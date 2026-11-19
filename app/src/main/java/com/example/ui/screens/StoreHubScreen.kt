package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.AppScreen
import com.example.ui.ShopViewModel
import com.example.ui.StoreViewModel
import com.example.ui.theme.ShopTheme

@Composable
fun StoreHubScreen(
    viewModel: ShopViewModel,
    store: StoreViewModel = viewModel()
) {
    val lowStock by store.lowStockCount.collectAsStateWithLifecycle()
    val colors = ShopTheme.colors

    StoreScaffold(
        title = stringResource(R.string.store_title),
        subtitle = stringResource(R.string.store_subtitle),
        onBack = { viewModel.navigateBack() }
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    HubEntry(
                        icon = Icons.Default.Inventory2,
                        title = stringResource(R.string.store_inventory),
                        description = stringResource(R.string.store_inventory_desc),
                        badge = if (lowStock > 0) stringResource(R.string.store_low_stock_badge, lowStock) else null,
                        badgeColor = colors.alert,
                        onClick = { viewModel.navigateTo(AppScreen.INVENTORY) }
                    )
                }
                item {
                    HubEntry(
                        icon = Icons.Default.PointOfSale,
                        title = stringResource(R.string.store_pos),
                        description = stringResource(R.string.store_pos_desc),
                        onClick = { viewModel.navigateTo(AppScreen.POS) }
                    )
                }
                item {
                    HubEntry(
                        icon = Icons.Default.Savings,
                        title = stringResource(R.string.store_cash),
                        description = stringResource(R.string.store_cash_desc),
                        onClick = { viewModel.navigateTo(AppScreen.CASH_REGISTER) }
                    )
                }
                item {
                    HubEntry(
                        icon = Icons.Default.Assessment,
                        title = stringResource(R.string.store_reports),
                        description = stringResource(R.string.store_reports_desc),
                        onClick = { viewModel.navigateTo(AppScreen.REPORTS) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HubEntry(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    badge: String? = null,
    badgeColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    StoreCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp).size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }
        }
    }
}
