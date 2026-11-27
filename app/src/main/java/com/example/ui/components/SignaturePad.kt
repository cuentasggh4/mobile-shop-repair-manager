package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.util.SignatureCodec

/** Finger signature pad. [onChange] receives the Base64 PNG (empty when cleared). */
@Composable
fun SignaturePad(
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var strokes by remember { mutableStateOf(listOf<List<Offset>>()) }
    var current by remember { mutableStateOf(listOf<Offset>()) }
    var padSize by remember { mutableStateOf(IntSize.Zero) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color.White, RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { padSize = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { start -> current = listOf(start) },
                            onDrag = { change, _ ->
                                change.consume()
                                current = current + change.position
                            },
                            onDragEnd = {
                                if (current.isNotEmpty()) strokes = strokes + listOf(current)
                                current = emptyList()
                                onChange(
                                    SignatureCodec.encode(
                                        strokes.map { s -> s.map { it.x to it.y } },
                                        padSize.width,
                                        padSize.height
                                    )
                                )
                            },
                            onDragCancel = { current = emptyList() }
                        )
                    }
            ) {
                val style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                (strokes + listOf(current)).forEach { points ->
                    if (points.size > 1) {
                        val path = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                        }
                        drawPath(path, Color.Black, style = style)
                    }
                }
            }
        }
        OutlinedButton(
            onClick = {
                strokes = emptyList()
                current = emptyList()
                onChange("")
            },
            enabled = strokes.isNotEmpty(),
            modifier = Modifier.height(40.dp)
        ) {
            Text(stringResource(R.string.sig_clear))
        }
    }
}
