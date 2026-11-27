package com.example.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.Base64
import java.io.ByteArrayOutputStream

/** Signatures are stored as a small black-on-white PNG, Base64 encoded, inside the job card. */
object SignatureCodec {
    private const val TARGET_WIDTH = 600f

    /** Each stroke is a list of (x, y) points in the pad's pixel space. */
    fun encode(strokes: List<List<Pair<Float, Float>>>, width: Int, height: Int): String {
        if (strokes.isEmpty() || width <= 0 || height <= 0) return ""
        val scale = TARGET_WIDTH / width
        val bitmap = Bitmap.createBitmap(TARGET_WIDTH.toInt(), (height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 3.5f * scale
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        strokes.forEach { stroke ->
            if (stroke.isEmpty()) return@forEach
            val path = Path()
            path.moveTo(stroke[0].first * scale, stroke[0].second * scale)
            if (stroke.size == 1) path.lineTo(stroke[0].first * scale + 0.1f, stroke[0].second * scale)
            for (i in 1 until stroke.size) path.lineTo(stroke[i].first * scale, stroke[i].second * scale)
            canvas.drawPath(path, paint)
        }
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    fun decode(data: String): Bitmap? {
        if (data.isBlank()) return null
        return try {
            val bytes = Base64.decode(data, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
