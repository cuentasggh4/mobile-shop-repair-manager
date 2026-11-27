package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.R
import com.example.data.database.Converters
import com.example.data.entity.JobCardEntity
import com.example.data.entity.ShopProfileEntity
import com.example.data.model.ConditionStatus
import com.example.data.model.RepairStage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_X = 36f

    fun generateJobReceiptPdf(
        context: Context,
        job: JobCardEntity,
        profile: ShopProfileEntity
    ): File {
        val receiptsDir = File(context.cacheDir, "receipts")
        if (!receiptsDir.exists()) {
            receiptsDir.mkdirs()
        }

        val safeJobNumber = job.jobNumber.replace("[^A-Za-z0-9_-]".toRegex(), "_")
        val outFile = File(receiptsDir, "JobCard_${safeJobNumber}.pdf")

        val pdfDocument = PdfDocument()
        drawReceiptContent(pdfDocument, job, profile, LanguageManager.wrap(context))

        FileOutputStream(outFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        return outFile
    }

    private fun drawReceiptContent(
        pdfDocument: PdfDocument,
        job: JobCardEntity,
        profile: ShopProfileEntity,
        lc: Context
    ) {
        val contentWidth = PAGE_WIDTH - (MARGIN_X * 2)
        val rightEdge = PAGE_WIDTH - MARGIN_X
        val maxContentY = PAGE_HEIGHT - 74f

        val primaryNavy = Color.parseColor("#1E3A8A")
        val emeraldColor = Color.parseColor("#059669")
        val amberColor = Color.parseColor("#D97706")
        val darkText = Color.parseColor("#0F172A")
        val mutedText = Color.parseColor("#475569")
        val borderGray = Color.parseColor("#CBD5E1")
        val lightSurface = Color.parseColor("#F8FAFC")

        val shopTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryNavy
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedText
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        val sectionHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryNavy
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedText
            textSize = 9.5f
        }
        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkText
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkText
            textSize = 9.5f
        }
        val smallMutedPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedText
            textSize = 8.2f
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderGray
            strokeWidth = 1f
        }
        val strongDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkText
            strokeWidth = 1.4f
        }
        val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lightSurface
            style = Paint.Style.FILL
        }
        val cardStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderGray
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }

        var pageNumber = 0
        var currentPage: PdfDocument.Page? = null
        lateinit var canvas: Canvas
        var y = 38f

        fun startNewPage(isContinuation: Boolean) {
            currentPage?.let { pdfDocument.finishPage(it) }
            pageNumber += 1
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            currentPage = page
            canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            y = 38f

            if (isContinuation) {
                val contPaint = TextPaint(sectionHeaderPaint).apply { textSize = 9.5f }
                val pageRightPaint = TextPaint(smallMutedPaint).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText(
                    "${profile.shopName.uppercase()} • #${job.jobNumber} (${lc.getString(R.string.rc_continued)})",
                    MARGIN_X,
                    y,
                    contPaint
                )
                canvas.drawText(lc.getString(R.string.rc_page, pageNumber), rightEdge, y, pageRightPaint)
                y += 8f
                canvas.drawLine(MARGIN_X, y, rightEdge, y, dividerPaint)
                y += 16f
            }
        }

        fun ensureSpace(requiredHeight: Float) {
            if (y + requiredHeight > maxContentY) {
                startNewPage(isContinuation = true)
            }
        }

        fun drawPaginatedText(
            text: String,
            paint: TextPaint,
            x: Float,
            maxWidth: Int
        ) {
            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, paint, maxWidth.coerceAtLeast(100))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1.0f)
                .setIncludePad(false)
                .build()

            for (lineIndex in 0 until layout.lineCount) {
                val lineHeight = (layout.getLineBottom(lineIndex) - layout.getLineTop(lineIndex)).toFloat()
                ensureSpace(lineHeight + 2f)
                val baseline = y - layout.getLineAscent(lineIndex)
                val lineText = text.substring(layout.getLineStart(lineIndex), layout.getLineEnd(lineIndex)).trimEnd('\n')
                canvas.drawText(lineText, x, baseline, paint)
                y += lineHeight
            }
        }

        startNewPage(isContinuation = false)

        try {
            // 1. Shop Header
            val centerX = PAGE_WIDTH / 2f
            canvas.drawText(profile.shopName.uppercase(), centerX, y, shopTitlePaint)
            y += 14f
            if (profile.tagline.isNotBlank()) {
                canvas.drawText(profile.tagline, centerX, y, subtitlePaint)
                y += 12f
            }
            val contactLine = listOf(profile.address, "Tel: ${profile.contactPhone}")
                .filter { it.isNotBlank() }
                .joinToString("  •  ")
            canvas.drawText(contactLine, centerX, y, subtitlePaint)
            y += 12f

            canvas.drawLine(MARGIN_X, y, rightEdge, y, strongDividerPaint)
            y += 16f

            // 2. Job Number & Barcode Strip
            val jobTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkText
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val jobNumberPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryNavy
                textSize = 15f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            canvas.drawText(lc.getString(R.string.rc_receipt_title), MARGIN_X, y, jobTitlePaint)
            canvas.drawText("#${job.jobNumber}", MARGIN_X, y + 17f, jobNumberPaint)

            val barWidths = listOf(3f, 1.5f, 2.5f, 4f, 1.5f, 3f, 2f, 1.5f, 4f, 2f, 1.5f, 3f, 2.5f, 4f, 1.5f)
            val totalBarWidth = barWidths.sum() + (barWidths.size - 1) * 2f
            var bx = rightEdge - totalBarWidth
            val barPaint = Paint().apply { color = Color.BLACK }
            for (bw in barWidths) {
                canvas.drawRect(bx, y - 10f, bx + bw, y + 10f, barPaint)
                bx += bw + 2f
            }
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val createdDateText = dateFormat.format(Date(job.createdAt))
            val dateRightPaint = TextPaint(smallMutedPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(createdDateText, rightEdge, y + 21f, dateRightPaint)

            y += 32f
            canvas.drawLine(MARGIN_X, y, rightEdge, y, dividerPaint)
            y += 14f

            // 3. Customer & Device Two-Column Summary Box
            val infoPairsLeft = buildList {
                add(lc.getString(R.string.rc_customer) to job.customerName)
                add(lc.getString(R.string.rc_phone) to job.customerPhone)
                add(lc.getString(R.string.rc_status) to lc.getString(Labels.stageRes(job.stage)))
            }
            val infoPairsRight = buildList {
                add(lc.getString(R.string.rc_device) to "${job.deviceBrand} ${job.deviceModel}".trim())
                if (job.deviceColor.isNotBlank()) add(lc.getString(R.string.rc_color) to job.deviceColor)
                if (job.imeiOrSerial.isNotBlank()) add(lc.getString(R.string.rc_imei) to job.imeiOrSerial)
            }

            val maxRows = maxOf(infoPairsLeft.size, infoPairsRight.size)
            val infoBoxHeight = 14f + (maxRows * 16f) + 6f
            ensureSpace(infoBoxHeight + 14f)

            val boxTop = y
            val colGap = 14f
            val colWidth = (contentWidth - colGap) / 2f
            val leftColX = MARGIN_X + 10f
            val rightColX = MARGIN_X + colWidth + colGap + 10f

            var leftY = y + 15f
            var rightY = y + 15f

            val infoRect = RectF(MARGIN_X, boxTop, rightEdge, boxTop + infoBoxHeight)
            canvas.drawRoundRect(infoRect, 6f, 6f, cardFillPaint)
            canvas.drawRoundRect(infoRect, 6f, 6f, cardStrokePaint)

            for ((lbl, v) in infoPairsLeft) {
                canvas.drawText("$lbl:", leftColX, leftY, labelPaint)
                canvas.drawText(truncateText(v, valuePaint, colWidth - 68f), leftColX + 58f, leftY, valuePaint)
                leftY += 16f
            }
            for ((lbl, v) in infoPairsRight) {
                canvas.drawText("$lbl:", rightColX, rightY, labelPaint)
                canvas.drawText(truncateText(v, valuePaint, colWidth - 68f), rightColX + 58f, rightY, valuePaint)
                rightY += 16f
            }

            y = boxTop + infoBoxHeight + 14f

            // 4. Reported Issue Section
            ensureSpace(32f)
            canvas.drawText(lc.getString(R.string.rc_reported_problem), MARGIN_X, y, sectionHeaderPaint)
            y += 6f
            drawPaginatedText(
                text = job.issueDescription.ifBlank { lc.getString(R.string.rc_no_issue) },
                paint = bodyPaint,
                x = MARGIN_X,
                maxWidth = contentWidth.toInt()
            )
            y += 12f
            ensureSpace(16f)
            canvas.drawLine(MARGIN_X, y, rightEdge, y, dividerPaint)
            y += 14f

            // 5. 12-Point Intake Checklist (3 columns x 4 rows)
            ensureSpace(30f)
            canvas.drawText(lc.getString(R.string.rc_checklist_title), MARGIN_X, y, sectionHeaderPaint)
            y += 12f
            val checklistMap = Converters.deserializeChecklist(job.checklistJson).toStatusMap().entries.toList()
            val chkColWidth = contentWidth / 3f
            val statusTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val chkItemPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkText
                textSize = 8.5f
            }

            checklistMap.chunked(3).forEach { rowItems ->
                ensureSpace(16f)
                rowItems.forEachIndexed { colIndex, entry ->
                    val itemX = MARGIN_X + (colIndex * chkColWidth)
                    val statusLabel = lc.getString(Labels.conditionRes(entry.value))
                    statusTextPaint.color = when (entry.value) {
                        ConditionStatus.WORKING -> emeraldColor
                        ConditionStatus.DEFECTIVE -> Color.parseColor("#DC2626")
                        ConditionStatus.UNTESTED -> mutedText
                    }
                    canvas.drawText(
                        truncateText("${Labels.checklistRes(entry.key)?.let { lc.getString(it) } ?: entry.key}:", chkItemPaint, chkColWidth - 36f),
                        itemX,
                        y,
                        chkItemPaint
                    )
                    canvas.drawText(statusLabel, itemX + chkColWidth - 34f, y, statusTextPaint)
                }
                y += 14f
            }

            y += 4f
            ensureSpace(16f)
            canvas.drawLine(MARGIN_X, y, rightEdge, y, dividerPaint)
            y += 14f

            // 5b. Damage diagram recorded at reception
            val damageMarks = DamageMarks.parse(job.damageMarks)
            if (damageMarks.isNotEmpty()) {
                ensureSpace(150f)
                canvas.drawText(lc.getString(R.string.rc_damage_title), MARGIN_X, y, sectionHeaderPaint)
                y += 10f
                val phoneW = 52f
                val phoneH = 104f
                val phoneGap = 40f
                val startX = MARGIN_X + 10f
                val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = darkText
                    style = Paint.Style.STROKE
                    strokeWidth = 1.2f
                }
                val phoneFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#F3F4F6")
                    style = Paint.Style.FILL
                }
                val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                    strokeCap = Paint.Cap.ROUND
                }
                listOf(false, true).forEachIndexed { index, back ->
                    val left = startX + index * (phoneW + phoneGap)
                    val rect = RectF(left, y, left + phoneW, y + phoneH)
                    canvas.drawRoundRect(rect, 8f, 8f, phoneFill)
                    canvas.drawRoundRect(rect, 8f, 8f, outlinePaint)
                    if (back) {
                        canvas.drawCircle(left + 10f, y + 10f, 3f, outlinePaint)
                        canvas.drawCircle(left + 10f, y + 20f, 3f, outlinePaint)
                    } else {
                        canvas.drawLine(left + phoneW / 2f - 7f, y + 5f, left + phoneW / 2f + 7f, y + 5f, outlinePaint)
                    }
                    canvas.drawText(
                        lc.getString(if (back) R.string.dmg_back else R.string.dmg_front),
                        left,
                        y + phoneH + 11f,
                        labelPaint
                    )
                    damageMarks.filter { it.back == back }.forEach { m ->
                        val cx = left + m.x * phoneW
                        val cy = y + m.y * phoneH
                        markPaint.color = DamageMarks.colorFor(m.type)
                        when (m.type) {
                            DamageMarks.CRACK -> {
                                canvas.drawLine(cx - 3.5f, cy - 3.5f, cx + 3.5f, cy + 3.5f, markPaint)
                                canvas.drawLine(cx - 3.5f, cy + 3.5f, cx + 3.5f, cy - 3.5f, markPaint)
                            }
                            DamageMarks.DENT -> {
                                markPaint.style = Paint.Style.FILL
                                canvas.drawCircle(cx, cy, 3.5f, markPaint)
                                markPaint.style = Paint.Style.STROKE
                            }
                            else -> canvas.drawLine(cx - 3.5f, cy + 3.5f, cx + 3.5f, cy - 3.5f, markPaint)
                        }
                    }
                }
                // legend
                val legendX = startX + 2 * (phoneW + phoneGap)
                listOf(
                    DamageMarks.SCRATCH to R.string.dmg_scratch,
                    DamageMarks.CRACK to R.string.dmg_crack,
                    DamageMarks.DENT to R.string.dmg_dent
                ).forEachIndexed { i, (type, res) ->
                    val ly = y + 20f + i * 16f
                    val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = DamageMarks.colorFor(type) }
                    canvas.drawCircle(legendX, ly - 3f, 3.5f, dot)
                    canvas.drawText(lc.getString(res), legendX + 10f, ly, labelPaint)
                }
                y += phoneH + 18f
                ensureSpace(16f)
                canvas.drawLine(MARGIN_X, y, rightEdge, y, dividerPaint)
                y += 14f
            }

            // 6. Billing Breakdown Box
            val currency = profile.currencySymbol
            val totalCharge = if (job.finalCost > 0.0) job.finalCost else job.estimatedCost
            val billingRows = mutableListOf(
                lc.getString(R.string.rc_estimate_charge) to "$currency${String.format(Locale.US, "%.2f", totalCharge)}",
                lc.getString(R.string.rc_advance_paid) to "-$currency${String.format(Locale.US, "%.2f", job.advancePaid)}"
            )
            if (job.discount > 0.0) {
                billingRows.add(lc.getString(R.string.rc_discount) to "-$currency${String.format(Locale.US, "%.2f", job.discount)}")
            }
            val billBoxHeight = 14f + (billingRows.size * 15f) + 28f
            ensureSpace(billBoxHeight + 24f)

            canvas.drawText(lc.getString(R.string.rc_billing_summary), MARGIN_X, y, sectionHeaderPaint)
            y += 8f

            val billBoxTop = y
            val billRect = RectF(MARGIN_X, billBoxTop, rightEdge, billBoxTop + billBoxHeight)
            canvas.drawRoundRect(billRect, 6f, 6f, cardFillPaint)
            canvas.drawRoundRect(billRect, 6f, 6f, cardStrokePaint)

            var billY = billBoxTop + 16f
            val valueRightPaint = TextPaint(valuePaint).apply { textAlign = Paint.Align.RIGHT }
            for ((lbl, amt) in billingRows) {
                canvas.drawText(lbl, MARGIN_X + 10f, billY, labelPaint)
                canvas.drawText(amt, rightEdge - 10f, billY, valueRightPaint)
                billY += 15f
            }

            canvas.drawLine(MARGIN_X + 10f, billY - 6f, rightEdge - 10f, billY - 6f, dividerPaint)
            billY += 9f

            val balanceLabel = if (job.isPaid) lc.getString(R.string.rc_paid_in_full, job.paymentMode) else lc.getString(R.string.rc_net_balance_due)
            val balanceTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkText
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val balanceAmtPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (job.isPaid || job.balanceDue <= 0.0) emeraldColor else amberColor
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(balanceLabel, MARGIN_X + 10f, billY, balanceTextPaint)
            canvas.drawText("$currency${String.format(Locale.US, "%.2f", job.balanceDue)}", rightEdge - 10f, billY, balanceAmtPaint)

            y = billBoxTop + billBoxHeight + 12f

            // 7. Delivery & Handover Record (if Delivered)
            if (job.stage == RepairStage.DELIVERED && job.deliveredAt != null) {
                ensureSpace(58f)
                canvas.drawText(lc.getString(R.string.rc_delivery_record), MARGIN_X, y, sectionHeaderPaint)
                y += 12f
                val recipientStr = if (!job.pickedUpByOriginalCustomer && job.recipientName.isNotBlank()) {
                    "${job.recipientName} (${job.recipientPhone}) - ${lc.getString(R.string.rc_representative)}"
                } else {
                    "${job.customerName} (${job.customerPhone}) - ${lc.getString(R.string.rc_original_customer)}"
                }
                val deliveredAtStr = dateFormat.format(Date(job.deliveredAt))
                canvas.drawText("${lc.getString(R.string.rc_picked_up_by)}: $recipientStr", MARGIN_X, y, bodyPaint)
                y += 13f
                canvas.drawText("${lc.getString(R.string.rc_delivered_on)}: $deliveredAtStr", MARGIN_X, y, smallMutedPaint)
                y += 12f
                canvas.drawLine(MARGIN_X, y, rightEdge, y, dividerPaint)
                y += 12f
            }

            // 8. Terms & Conditions (Paginated cleanly)
            if (profile.termsAndConditions.isNotBlank()) {
                ensureSpace(32f)
                canvas.drawText(lc.getString(R.string.rc_terms), MARGIN_X, y, sectionHeaderPaint)
                y += 6f
                drawPaginatedText(
                    text = profile.termsAndConditions,
                    paint = smallMutedPaint,
                    x = MARGIN_X,
                    maxWidth = contentWidth.toInt()
                )
                y += 12f
            }

            // 9. Signatures Footer (Ensure non-overlapping placement at bottom of final page)
            val signatureBitmap = SignatureCodec.decode(job.signatureData)
            ensureSpace(if (signatureBitmap != null) 96f else 54f)
            val sigLineY = PAGE_HEIGHT - 44f
            val sigWidth = 140f
            if (signatureBitmap != null) {
                val scale = minOf(sigWidth / signatureBitmap.width, 40f / signatureBitmap.height)
                val w = signatureBitmap.width * scale
                val h = signatureBitmap.height * scale
                val dst = RectF(MARGIN_X + (sigWidth - w) / 2f, sigLineY - 3f - h, MARGIN_X + (sigWidth + w) / 2f, sigLineY - 3f)
                canvas.drawBitmap(signatureBitmap, null, dst, null)
            }
            canvas.drawLine(MARGIN_X, sigLineY, MARGIN_X + sigWidth, sigLineY, strongDividerPaint)
            canvas.drawText(lc.getString(R.string.rc_customer_signature), MARGIN_X + 24f, sigLineY + 13f, labelPaint)

            canvas.drawLine(rightEdge - sigWidth, sigLineY, rightEdge, sigLineY, strongDividerPaint)
            canvas.drawText(lc.getString(R.string.rc_technician_signature), rightEdge - sigWidth + 20f, sigLineY + 13f, labelPaint)
        } finally {
            currentPage?.let { pdfDocument.finishPage(it) }
        }
    }

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "...") > maxWidth) {
            end--
        }
        return if (end <= 1) "..." else text.substring(0, end) + "..."
    }
}
