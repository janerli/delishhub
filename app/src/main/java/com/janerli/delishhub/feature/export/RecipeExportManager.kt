package com.janerli.delishhub.feature.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.janerli.delishhub.data.local.model.RecipeFull
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object RecipeExportManager {

    data class ExportResult(
        val file: File,
        val displayName: String
    )

    fun exportRecipeToPdf(
        context: Context,
        full: RecipeFull
    ): ExportResult {
        val safeTitle = full.recipe.title
            .trim()
            .ifBlank { "recipe" }
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .take(60)

        val ts = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val fileName = "DelishHub_${safeTitle}_$ts.pdf"

        val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outFile = File(outDir, fileName)

        val doc = PdfDocument()

        // A4 @ 72dpi
        val pageW = 595
        val pageH = 842
        val margin = 40

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val h2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        fun newPage(): Pair<PdfDocument.Page, Canvas> {
            val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, doc.pages.size + 1).create()
            val page = doc.startPage(pageInfo)
            return page to page.canvas
        }

        fun finishPage(page: PdfDocument.Page) {
            doc.finishPage(page)
        }

        fun drawWrappedText(
            canvas: Canvas,
            text: String,
            x: Float,
            yStart: Float,
            maxWidth: Int,
            paint: Paint,
            lineSpacing: Float = 1.25f
        ): Pair<Float, List<String>> {
            val lines = wrapText(text, paint, maxWidth)
            var y = yStart
            val lineH = paint.fontSpacing * lineSpacing
            for (line in lines) {
                canvas.drawText(line, x, y, paint)
                y += lineH
            }
            return y to lines
        }

        var (page, canvas) = newPage()
        var y = margin.toFloat()

        // --- Header ---
        canvas.drawText(full.recipe.title, margin.toFloat(), y, titlePaint)
        y += titlePaint.fontSpacing * 1.4f

        val meta = buildString {
            append("Время: ${full.recipe.cookTimeMin} мин")
            append(" • Сложность: ${full.recipe.difficulty}/5")
            if (full.recipe.isVegetarian) append(" • Вегетарианское")
            if (full.recipe.isPublic) append(" • Public")
        }
        canvas.drawText(meta, margin.toFloat(), y, subPaint)
        y += subPaint.fontSpacing * 1.4f

        if (full.tags.isNotEmpty()) {
            val tagsLine = "Теги: " + full.tags.sortedBy { it.name.lowercase() }.joinToString(", ") { it.name }
            val (nextY, _) = drawWrappedText(
                canvas = canvas,
                text = tagsLine,
                x = margin.toFloat(),
                yStart = y,
                maxWidth = pageW - margin * 2,
                paint = subPaint
            )
            y = nextY + 6f
        } else {
            y += 6f
        }

        // --- Image (optional) ---
        val bmp = loadBitmapFromRecipeImage(full.recipe.mainImageUrl)
        if (bmp != null) {
            val maxImgW = pageW - margin * 2
            val maxImgH = 220
            val scaled = scaleBitmapToFit(bmp, maxImgW, maxImgH)

            val left = margin
            val top = y.toInt()
            val rect = Rect(left, top, left + scaled.width, top + scaled.height)

            canvas.drawBitmap(scaled, null, rect, null)
            y = rect.bottom + 16f
            if (scaled !== bmp) scaled.recycle()
            bmp.recycle()
        }

        // --- Description (optional) ---
        val desc = full.recipe.description.trim()
        if (desc.isNotBlank()) {
            canvas.drawText("Описание", margin.toFloat(), y, h2Paint)
            y += h2Paint.fontSpacing * 1.2f

            val (nextY, _) = drawWrappedText(
                canvas = canvas,
                text = desc,
                x = margin.toFloat(),
                yStart = y,
                maxWidth = pageW - margin * 2,
                paint = textPaint
            )
            y = nextY + 14f
        }

        // --- Ingredients ---
        canvas.drawText("Ингредиенты", margin.toFloat(), y, h2Paint)
        y += h2Paint.fontSpacing * 1.2f

        val ingredients = full.ingredients.sortedBy { it.position }
        for (ing in ingredients) {
            val amountPart = buildString {
                val a = ing.amount
                val u = ing.unit
                if (a != null) append(a.toString().removeSuffix(".0"))
                if (!u.isNullOrBlank()) {
                    if (isNotEmpty()) append(" ")
                    append(u.trim())
                }
            }
            val line = "• ${ing.name}${if (amountPart.isNotBlank()) " — $amountPart" else ""}"

            // new page if needed
            if (y + textPaint.fontSpacing * 2 > pageH - margin) {
                finishPage(page)
                val p = newPage()
                page = p.first
                canvas = p.second
                y = margin.toFloat()
            }

            val (nextY, _) = drawWrappedText(
                canvas = canvas,
                text = line,
                x = margin.toFloat(),
                yStart = y,
                maxWidth = pageW - margin * 2,
                paint = textPaint
            )
            y = nextY
        }
        y += 14f

        // --- Steps ---
        canvas.drawText("Шаги приготовления", margin.toFloat(), y, h2Paint)
        y += h2Paint.fontSpacing * 1.2f

        val steps = full.steps.sortedBy { it.position }
        for ((idx, step) in steps.withIndex()) {
            val stepText = "${idx + 1}. ${step.text}"

            if (y + textPaint.fontSpacing * 3 > pageH - margin) {
                finishPage(page)
                val p = newPage()
                page = p.first
                canvas = p.second
                y = margin.toFloat()
            }

            val (nextY, _) = drawWrappedText(
                canvas = canvas,
                text = stepText,
                x = margin.toFloat(),
                yStart = y,
                maxWidth = pageW - margin * 2,
                paint = textPaint
            )
            y = nextY + 6f
        }

        // --- Footer ---
        val footer = "Экспортировано из DelishHub • " + SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val footerY = (pageH - margin / 2).toFloat()
        canvas.drawText(footer, margin.toFloat(), footerY, subPaint)

        finishPage(page)

        FileOutputStream(outFile).use { fos ->
            doc.writeTo(fos)
        }
        doc.close()

        return ExportResult(file = outFile, displayName = fileName)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        val words = text.replace("\n", " \n ").split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        val sb = StringBuilder()

        fun flush() {
            val s = sb.toString().trimEnd()
            if (s.isNotEmpty()) lines.add(s)
            sb.clear()
        }

        for (w in words) {
            if (w == "\n") {
                flush()
                lines.add("")
                continue
            }

            val candidate = if (sb.isEmpty()) w else sb.toString() + " " + w
            if (paint.measureText(candidate) <= maxWidth) {
                if (sb.isNotEmpty()) sb.append(' ')
                sb.append(w)
            } else {
                flush()
                // single long word: hard-split
                if (paint.measureText(w) <= maxWidth) {
                    sb.append(w)
                } else {
                    var chunk = ""
                    for (ch in w) {
                        val cand2 = chunk + ch
                        if (paint.measureText(cand2) <= maxWidth) chunk = cand2
                        else {
                            if (chunk.isNotEmpty()) lines.add(chunk)
                            chunk = ch.toString()
                        }
                    }
                    if (chunk.isNotEmpty()) sb.append(chunk)
                }
            }
        }
        flush()
        return lines
    }

    private fun loadBitmapFromRecipeImage(mainImageUrl: String?): Bitmap? {
        if (mainImageUrl.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(mainImageUrl)
            when (uri.scheme?.lowercase()) {
                "file" -> {
                    val path = uri.path ?: return null
                    BitmapFactory.decodeFile(path)
                }
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun scaleBitmapToFit(src: Bitmap, maxW: Int, maxH: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return src

        val scale = min(maxW.toFloat() / w.toFloat(), maxH.toFloat() / h.toFloat())
        val outW = max(1, (w * scale).toInt())
        val outH = max(1, (h * scale).toInt())

        if (outW == w && outH == h) return src
        return Bitmap.createScaledBitmap(src, outW, outH, true)
    }
}
