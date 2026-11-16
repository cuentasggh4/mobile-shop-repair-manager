package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.RepairStage
import com.example.ui.theme.ShopTheme

@Composable
fun RepairStatusBadge(
    stage: RepairStage,
    modifier: Modifier = Modifier
) {
    val shopColors = ShopTheme.colors
    // Semantic Badge Colors:
    // Amber/Yellow: Pending / In Progress
    // Emerald/Green: Repaired / Ready to Deliver
    // Crimson/Red: Cancelled / Cannot Repair
    // Slate/Muted: Delivered
    val (dotColor, containerColor, textRes) = when (stage) {
        RepairStage.DIAGNOSTIC -> Triple(shopColors.pending, shopColors.pendingContainer, R.string.status_diagnostics)
        RepairStage.WAITING_FOR_PARTS -> Triple(shopColors.pending, shopColors.pendingContainer, R.string.status_waiting_parts)
        RepairStage.IN_PROGRESS -> Triple(shopColors.pending, shopColors.pendingContainer, R.string.status_in_progress)
        RepairStage.READY_FOR_PICKUP -> Triple(shopColors.repaired, shopColors.repairedContainer, R.string.status_ready_deliver)
        RepairStage.DELIVERED -> Triple(shopColors.delivered, shopColors.deliveredContainer, R.string.status_delivered)
        RepairStage.CANCELLED -> Triple(shopColors.alert, shopColors.alertContainer, R.string.status_cancelled)
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.testTag("repair_status_badge_${stage.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = stringResource(textRes),
                color = dotColor,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}
