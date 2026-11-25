package com.example.util

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlin.math.abs
import kotlin.math.roundToInt

data class ParsedPrice(val name: String, val price: Double)

data class PriceListResult(
    val items: List<ParsedPrice>,
    val skipped: Int,
    val pages: Int
)

/**
 * Reads supplier price lists laid out as a table: product name on the left, price in a right-aligned
 * column ("PRODUCTO  ... PRECIO"). It works from the position of every character on the page, so rows whose
 * name wraps onto two lines (price centred between them) are still matched to the right price.
 * Rows with "-" or no price are skipped.
 */
object PriceListPdfParser {

    private class Glyph(val ch: Char, val x0: Float, val x1: Float, val y: Float)

    private class Row(val y: Float, val price: Double?) {
        val parts = ArrayList<Pair<Float, String>>()
    }

    private class Collector : PDFTextStripper() {
        val pages = ArrayList<Pair<Float, MutableList<Glyph>>>()

        override fun startPage(page: PDPage) {
            pages.add(page.mediaBox.width to ArrayList())
            super.startPage(page)
        }

        override fun processTextPosition(text: TextPosition) {
            val s = text.unicode ?: return
            if (s.isBlank() || pages.isEmpty()) return
            val list = pages.last().second
            val x = text.xDirAdj
            val w = text.widthDirAdj
            val y = text.yDirAdj
            val step = w / s.length
            s.forEachIndexed { i, c ->
                if (!c.isWhitespace()) list.add(Glyph(c, x + i * step, x + (i + 1) * step, y))
            }
        }
    }

    private val ENDS_WITH_DECIMALS = Regex("""\.\d\d$""")
    private val PRICE_END = Regex("""(\d{1,3}(?:,\d{3})+\.\d{2}|\d{1,3}\.\d{2})$""")
    private val SPACES = Regex("""\s+""")
    private val NOISE_PREFIX = Regex("""^(\([A-Z]{1,12}\)|NR|\(N\)R)\s*""")

    fun parse(context: Context, uri: Uri): PriceListResult {
        PDFBoxResourceLoader.init(context.applicationContext)
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open file")
        return input.use { stream ->
            PDDocument.load(stream).use { doc ->
                val collector = Collector()
                collector.getText(doc)
                val items = ArrayList<ParsedPrice>()
                val skipped = IntArray(1)
                collector.pages.forEach { (width, glyphs) -> parsePage(width, glyphs, items, skipped) }
                val unique = items.distinctBy { it.name.lowercase() to it.price }
                PriceListResult(unique, skipped[0], collector.pages.size)
            }
        }
    }

    /** First word of the product name ("BATERIA", "PANTALLA"...) in title case. */
    fun categoryOf(name: String): String {
        var text = name.trim()
        while (true) {
            val stripped = NOISE_PREFIX.replace(text, "")
            if (stripped == text) break
            text = stripped
        }
        val word = Regex("""^[A-Za-zÁÉÍÓÚÑáéíóúñ]+""").find(text)?.value ?: return "Otros"
        return word.lowercase().replaceFirstChar { it.uppercase() }
    }

    private fun parsePage(width: Float, glyphs: List<Glyph>, out: MutableList<ParsedPrice>, skipped: IntArray) {
        if (glyphs.isEmpty()) return
        val sorted = glyphs.sortedWith(compareBy<Glyph>({ it.y }, { it.x0 }))
        val lines = ArrayList<Pair<Float, MutableList<Glyph>>>()
        for (g in sorted) {
            val last = lines.lastOrNull()
            if (last != null && abs(last.first - g.y) < 2.5f) last.second.add(g)
            else lines.add(g.y to mutableListOf(g))
        }
        lines.forEach { line -> line.second.sortBy { it.x0 } }

        // The price column is right aligned: find where it ends on this page.
        val edgeCounts = HashMap<Int, Int>()
        for ((_, cs) in lines) {
            val text = cs.joinToString("") { it.ch.toString() }
            if (ENDS_WITH_DECIMALS.containsMatchIn(text)) {
                edgeCounts.merge(cs.last().x1.roundToInt(), 1) { a, b -> a + b }
            }
        }
        val right = edgeCounts.maxByOrNull { it.value }?.key?.toFloat() ?: return
        val left = right - 0.091f * width

        val used = HashSet<Glyph>()
        val rows = ArrayList<Row>()
        for ((y, cs) in lines) {
            val last = cs.last()
            var priceRow: Row? = null
            if (abs(last.x1 - right) <= 1.5f && last.ch.isDigit()) {
                var k = cs.size - 1
                val token = StringBuilder()
                while (k >= 0 && cs[k].ch in "0123456789.," &&
                    (k == cs.size - 1 || cs[k + 1].x0 - cs[k].x1 < 2f)
                ) {
                    token.insert(0, cs[k].ch)
                    k--
                }
                val m = PRICE_END.find(token)
                if (m != null) {
                    val n = m.value.length
                    if (cs[cs.size - n].x0 > left - 0.05f * width) {
                        for (g in cs.subList(cs.size - n, cs.size)) used.add(g)
                        priceRow = Row(y, m.value.replace(",", "").toDouble())
                    }
                }
            }
            if (priceRow == null) {
                val dashes = cs.filter { it.ch == '-' && it.x0 > right - 0.067f * width && it.x1 <= right + 1f }
                if (dashes.isNotEmpty()) {
                    used.addAll(dashes)
                    priceRow = Row(y, null)
                }
            }
            if (priceRow != null) rows.add(priceRow)
        }
        if (rows.isEmpty()) return

        for ((y, cs) in lines) {
            val sb = StringBuilder()
            var prev: Glyph? = null
            for (g in cs) {
                if (g in used || g.x0 >= left) continue
                if (prev != null && g.x0 - prev.x1 > 1.2f) sb.append(' ')
                sb.append(g.ch)
                prev = g
            }
            val text = sb.toString().trim()
            if (text.isEmpty()) continue
            var best = 0
            var bestDist = Float.MAX_VALUE
            rows.forEachIndexed { i, r ->
                val d = abs(y - r.y)
                if (d < bestDist) { bestDist = d; best = i }
            }
            if (bestDist <= 11f) rows[best].parts.add(y to text)
        }

        for (row in rows) {
            val name = SPACES.replace(row.parts.sortedBy { it.first }.joinToString(" ") { it.second }, " ").trim()
            val price = row.price
            if (price != null && price > 0.0 && name.length >= 3) out.add(ParsedPrice(name, price)) else skipped[0]++
        }
    }
}
