package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.util.DamageMark
import com.example.util.DamageMarks

/** Phone outline (front or back) inside one half of the canvas. */
private fun phoneRect(back: Boolean, width: Float, height: Float): Rect {
    val half = width / 2f
    val pw = minOf(half * 0.62f, height * 0.5f)
    val ph = pw * 2f
    val cx = if (back) half + half / 2f else half / 2f
    return Rect(cx - pw / 2f, (height - ph) / 2f, cx + pw / 2f, (height + ph) / 2f)
}

@Composable
private fun damageTypeLabel(type: Char): String = when (type) {
    DamageMarks.CRACK -> stringResource(R.string.dmg_crack)
    DamageMarks.DENT -> stringResource(R.string.dmg_dent)
    else -> stringResource(R.string.dmg_scratch)
}

private fun DrawScope.drawPhone(rect: Rect, back: Boolean, outline: Color, fill: Color) {
    val radius = CornerRadius(rect.width * 0.16f)
    drawRoundRect(fill, rect.topLeft, rect.size, radius)
    drawRoundRect(outline, rect.topLeft, rect.size, radius, style = Stroke(width = 3f))
    if (back) {
        // camera module
        val cam = Rect(rect.left + rect.width * 0.08f, rect.top + rect.width * 0.08f, rect.left + rect.width * 0.46f, rect.top + rect.width * 0.46f)
        drawRoundRect(outline.copy(alpha = 0.25f), cam.topLeft, cam.size, CornerRadius(cam.width * 0.25f))
        drawCircle(outline, cam.width * 0.18f, Offset(cam.center.x - cam.width * 0.2f, cam.center.y - cam.width * 0.2f))
        drawCircle(outline, cam.width * 0.18f, Offset(cam.center.x + cam.width * 0.2f, cam.center.y + cam.width * 0.2f))
    } else {
        // earpiece and screen area
        drawRoundRect(
            outline.copy(alpha = 0.6f),
            Offset(rect.center.x - rect.width * 0.14f, rect.top + rect.width * 0.07f),
            Size(rect.width * 0.28f, rect.width * 0.04f),
            CornerRadius(4f)
        )
        drawRoundRect(
            outline.copy(alpha = 0.35f),
            Offset(rect.left + rect.width * 0.07f, rect.top + rect.width * 0.17f),
            Size(rect.width * 0.86f, rect.height - rect.width * 0.34f),
            CornerRadius(rect.width * 0.06f),
            style = Stroke(width = 1.5f)
        )
    }
}

private fun DrawScope.drawMark(center: Offset, type: Char, size: Float) {
    val color = Color(DamageMarks.colorFor(type))
    when (type) {
        DamageMarks.CRACK -> {
            drawLine(color, Offset(center.x - size, center.y - size), Offset(center.x + size, center.y + size), strokeWidth = 5f)
            drawLine(color, Offset(center.x - size, center.y + size), Offset(center.x + size, center.y - size), strokeWidth = 5f)
        }
        DamageMarks.DENT -> {
            drawCircle(color.copy(alpha = 0.85f), size)
        }
        else -> {
            drawLine(color, Offset(center.x - size, center.y + size), Offset(center.x + size, center.y - size), strokeWidth = 6f)
        }
    }
}

/** Front and back phone drawings with the marks placed on them. Tap to add a mark when [onTap] is given. */
@Composable
fun DamageDiagram(
    marks: List<DamageMark>,
    modifier: Modifier = Modifier,
    selectedType: Char = DamageMarks.SCRATCH,
    height: androidx.compose.ui.unit.Dp = 240.dp,
    onTap: ((DamageMark) -> Unit)? = null
) {
    val outline = MaterialTheme.colorScheme.onSurface
    val fill = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.dmg_front),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = stringResource(R.string.dmg_back),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(onTap, selectedType) {
                    if (onTap == null) return@pointerInput
                    detectTapGestures { offset ->
                        val back = offset.x >= size.width / 2f
                        val rect = phoneRect(back, size.width.toFloat(), size.height.toFloat())
                        if (rect.contains(offset)) {
                            onTap(
                                DamageMark(
                                    back = back,
                                    x = (offset.x - rect.left) / rect.width,
                                    y = (offset.y - rect.top) / rect.height,
                                    type = selectedType
                                )
                            )
                        }
                    }
                }
        ) {
            val frontRect = phoneRect(false, size.width, size.height)
            val backRect = phoneRect(true, size.width, size.height)
            drawPhone(frontRect, false, outline, fill)
            drawPhone(backRect, true, outline, fill)
            marks.forEach { m ->
                val rect = if (m.back) backRect else frontRect
                drawMark(Offset(rect.left + m.x * rect.width, rect.top + m.y * rect.height), m.type, 9f)
            }
        }
    }
}

/** Editable diagram: choose the type of damage, tap the phone to mark it. */
@Composable
fun DamageEditor(
    marks: List<DamageMark>,
    onMarksChange: (List<DamageMark>) -> Unit,
    modifier: Modifier = Modifier
) {
    var type by remember { mutableStateOf(DamageMarks.SCRATCH) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.dmg_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            DamageMarks.TYPES.forEach { t ->
                FilterChip(
                    selected = type == t,
                    onClick = { type = t },
                    label = { Text(damageTypeLabel(t)) }
                )
            }
        }
        DamageDiagram(
            marks = marks,
            selectedType = type,
            onTap = { mark -> onMarksChange(marks + mark) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onMarksChange(marks.dropLast(1)) }, enabled = marks.isNotEmpty()) {
                Text(stringResource(R.string.dmg_undo))
            }
            OutlinedButton(onClick = { onMarksChange(emptyList()) }, enabled = marks.isNotEmpty()) {
                Text(stringResource(R.string.dmg_clear))
            }
        }
    }
}
