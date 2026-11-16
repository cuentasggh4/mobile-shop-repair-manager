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
        drawReceiptContent(pdfDocument, job, profile)

        FileOutputStream(outFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        return outFile
    }

    private fun drawReceiptContent(
        pdfDocument: PdfDocument,
        job: JobCardEntity,
        profile: ShopProfileEntity
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
                    "${profile.shopName.uppercase()} • JOB CARD #${job.jobNumber} (Cont.)",
                    MARGIN_X,
                    y,
                    contPaint
                )
                canvas.drawText("Page $pageNumber", rightEdge, y, pageRightPaint)
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
            canvas.drawText("JOB CARD RECEIPT", MARGIN_X, y, jobTitlePaint)
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
                add("Customer" to job.customerName)
                add("Phone" to job.customerPhone)
                add("Status" to job.stage.title)
            }
            val infoPairsRight = buildList {
                add("Device" to "${job.deviceBrand} ${job.deviceModel}".trim())
                if (job.deviceColor.isNotBlank()) add("Color" to job.deviceColor)
                if (job.imeiOrSerial.isNotBlank()) add("IMEI / SN" to job.imeiOrSerial)
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
            canvas.drawText("REPORTED PROBLEM / COMPLAINT", MARGIN_X, y, sectionHeaderPaint)
            y += 6f
            drawPaginatedText(
                text = job.issueDescription.ifBlank { "No issue description provided" },
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
            canvas.drawText("12-POINT INTAKE CONDITION CHECKLIST", MARGIN_X, y, sectionHeaderPaint)
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
                    val statusLabel = entry.value.label
                    statusTextPaint.color = when (entry.value) {
                        ConditionStatus.WORKING -> emeraldColor
                        ConditionStatus.DEFECTIVE -> Color.parseColor("#DC2626")
                        ConditionStatus.UNTESTED -> mutedText
                    }
                    canvas.drawText(
                        truncateText("${entry.key}:", chkItemPaint, chkColWidth - 36f),
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

            // 6. Billing Breakdown Box
            val currency = profile.currencySymbol
            val totalCharge = if (job.finalCost > 0.0) job.finalCost else job.estimatedCost
            val billingRows = mutableListOf(
                "Repair Estimate / Final Charge" to "$currency${String.format(Locale.US, "%.2f", totalCharge)}",
                "Advance Paid" to "-$currency${String.format(Locale.US, "%.2f", job.advancePaid)}"
            )
            if (job.discount > 0.0) {
                billingRows.add("Discount" to "-$currency${String.format(Locale.US, "%.2f", job.discount)}")
            }
            val billBoxHeight = 14f + (billingRows.size * 15f) + 28f
            ensureSpace(billBoxHeight + 24f)

            canvas.drawText("BILLING & PAYMENT SUMMARY", MARGIN_X, y, sectionHeaderPaint)
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

            val balanceLabel = if (job.isPaid) "PAID IN FULL (${job.paymentMode})" else "NET BALANCE DUE"
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
                canvas.drawText("DELIVERY & HANDOVER RECORD", MARGIN_X, y, sectionHeaderPaint)
                y += 12f
                val recipientStr = if (!job.pickedUpByOriginalCustomer && job.recipientName.isNotBlank()) {
                    "${job.recipientName} (${job.recipientPhone}) - Representative"
                } else {
                    "${job.customerName} (${job.customerPhone}) - Original Customer"
                }
                val deliveredAtStr = dateFormat.format(Date(job.deliveredAt))
                canvas.drawText("Picked Up By: $recipientStr", MARGIN_X, y, bodyPaint)
                y += 13f
                canvas.drawText("Delivered On: $deliveredAtStr", MARGIN_X, y, smallMutedPaint)
                y += 12f
                canvas.drawLine(MARGIN_X, y, rightEdge, y, dividerPaint)
                y += 12f
            }

            // 8. Terms & Conditions (Paginated cleanly)
            if (profile.termsAndConditions.isNotBlank()) {
                ensureSpace(32f)
                canvas.drawText("TERMS & CONDITIONS", MARGIN_X, y, sectionHeaderPaint)
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
            ensureSpace(54f)
            val sigLineY = PAGE_HEIGHT - 44f
            val sigWidth = 140f
            canvas.drawLine(MARGIN_X, sigLineY, MARGIN_X + sigWidth, sigLineY, strongDividerPaint)
            canvas.drawText("Customer Signature", MARGIN_X + 24f, sigLineY + 13f, labelPaint)

            canvas.drawLine(rightEdge - sigWidth, sigLineY, rightEdge, sigLineY, strongDividerPaint)
            canvas.drawText("Technician Signature", rightEdge - sigWidth + 20f, sigLineY + 13f, labelPaint)
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
