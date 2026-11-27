package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.entity.JobCardEntity
import com.example.ui.components.DamageDiagram
import com.example.util.DamageMarks
import com.example.util.SignatureCodec

/** Damage diagram and customer signature captured when the device was received. */
@Composable
fun JobIntakeCard(job: JobCardEntity) {
    val marks = remember(job.damageMarks) { DamageMarks.parse(job.damageMarks) }
    val signature = remember(job.signatureData) { SignatureCodec.decode(job.signatureData) }
    if (marks.isEmpty() && signature == null) return

    StoreCard {
        Text(
            text = stringResource(R.string.intake_card_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (marks.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            DamageDiagram(marks = marks, height = 200.dp)
        }
        if (signature != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.sig_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Image(
                bitmap = signature.asImageBitmap(),
                contentDescription = stringResource(R.string.sig_title),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
            )
        }
    }
}
