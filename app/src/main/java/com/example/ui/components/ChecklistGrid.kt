package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ConditionStatus
import com.example.data.model.IntakeChecklist
import com.example.ui.theme.ShopTheme

data class ChecklistItemData(
    val key: String,
    val titleRes: Int,
    val status: ConditionStatus
)

fun getChecklistDefinitions(checklist: IntakeChecklist): List<ChecklistItemData> {
    return listOf(
        ChecklistItemData("Screen Touch", R.string.check_touch, checklist.screenTouch),
        ChecklistItemData("Display / LCD", R.string.check_display, checklist.displayGlass),
        ChecklistItemData("Front Camera", R.string.check_camera_front, checklist.frontCamera),
        ChecklistItemData("Rear Camera", R.string.check_camera_rear, checklist.rearCamera),
        ChecklistItemData("Microphone", R.string.check_mic, checklist.microphone),
        ChecklistItemData("Speaker / Audio", R.string.check_speaker, checklist.speaker),
        ChecklistItemData("Charging Port", R.string.check_port, checklist.chargingPort),
        ChecklistItemData("Power / Vol Buttons", R.string.check_buttons, checklist.physicalButtons),
        ChecklistItemData("Wi-Fi / BT", R.string.check_wireless, checklist.wifiBluetooth),
        ChecklistItemData("Fingerprint / FaceID", R.string.check_biometrics, checklist.biometrics),
        ChecklistItemData("Body Housing / Dent", R.string.check_housing, checklist.housingCondition),
        ChecklistItemData("SIM / Cellular", R.string.check_sim, checklist.simNetwork)
    )
}

@Composable
fun InteractiveChecklistGrid(
    checklist: IntakeChecklist,
    onStatusChange: (String, ConditionStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = getChecklistDefinitions(checklist)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { item ->
            ChecklistItemCard(
                item = item,
                onStatusClick = { nextStatus -> onStatusChange(item.key, nextStatus) }
            )
        }
    }
}

@Composable
fun ChecklistItemCard(
    item: ChecklistItemData,
    onStatusClick: (ConditionStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val shopColors = ShopTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("checklist_item_${item.key.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(10.dp),
        color = when (item.status) {
            ConditionStatus.WORKING -> shopColors.repairedContainer.copy(alpha = 0.45f)
            ConditionStatus.DEFECTIVE -> shopColors.alertContainer.copy(alpha = 0.45f)
            ConditionStatus.UNTESTED -> MaterialTheme.colorScheme.surface
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when (item.status) {
                ConditionStatus.WORKING -> shopColors.repaired.copy(alpha = 0.6f)
                ConditionStatus.DEFECTIVE -> shopColors.alert.copy(alpha = 0.6f)
                ConditionStatus.UNTESTED -> MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(item.titleRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TriStateToggleOption(
                    label = stringResource(R.string.check_status_ok),
                    icon = Icons.Default.Check,
                    isSelected = item.status == ConditionStatus.WORKING,
                    activeColor = shopColors.repaired,
                    activeBg = shopColors.repairedContainer,
                    onClick = { onStatusClick(ConditionStatus.WORKING) }
                )
                TriStateToggleOption(
                    label = stringResource(R.string.check_status_issue),
                    icon = Icons.Default.Close,
                    isSelected = item.status == ConditionStatus.DEFECTIVE,
                    activeColor = shopColors.alert,
                    activeBg = shopColors.alertContainer,
                    onClick = { onStatusClick(ConditionStatus.DEFECTIVE) }
                )
                TriStateToggleOption(
                    label = stringResource(R.string.check_status_na),
                    icon = Icons.Default.Remove,
                    isSelected = item.status == ConditionStatus.UNTESTED,
                    activeColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    activeBg = MaterialTheme.colorScheme.surfaceVariant,
                    onClick = { onStatusClick(ConditionStatus.UNTESTED) }
                )
            }
        }
    }
}

@Composable
private fun TriStateToggleOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    activeBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp, minWidth = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isSelected) activeBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, activeColor) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
            )
        }
    }
}

@Composable
fun ChecklistSummaryViewer(
    checklist: IntakeChecklist,
    modifier: Modifier = Modifier
) {
    val shopColors = ShopTheme.colors
    val items = getChecklistDefinitions(checklist)
    val defectiveCount = items.count { it.status == ConditionStatus.DEFECTIVE }
    val workingCount = items.count { it.status == ConditionStatus.WORKING }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "${stringResource(R.string.check_status_ok)}: $workingCount",
                style = MaterialTheme.typography.titleSmall,
                color = shopColors.repaired
            )
            Text(
                text = "${stringResource(R.string.check_status_issue)}: $defectiveCount",
                style = MaterialTheme.typography.titleSmall,
                color = if (defectiveCount > 0) shopColors.alert else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { item ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when (item.status) {
                                        ConditionStatus.WORKING -> shopColors.repairedContainer.copy(alpha = 0.55f)
                                        ConditionStatus.DEFECTIVE -> shopColors.alertContainer.copy(alpha = 0.55f)
                                        ConditionStatus.UNTESTED -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(item.titleRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = when (item.status) {
                                    ConditionStatus.WORKING -> Icons.Default.Check
                                    ConditionStatus.DEFECTIVE -> Icons.Default.Close
                                    ConditionStatus.UNTESTED -> Icons.Default.Remove
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = when (item.status) {
                                    ConditionStatus.WORKING -> shopColors.repaired
                                    ConditionStatus.DEFECTIVE -> shopColors.alert
                                    ConditionStatus.UNTESTED -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    if (pair.size == 1) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
