package com.indiza.cotisation.tools

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.indiza.cotisation.tontine.TontineDetailUiState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generateAndShare(context: Context, state: TontineDetailUiState.Success) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        var y = 40f

        // Title
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("RAPPORT FINANCIER : ${state.tontineWithCaisses.tontine.libelle.uppercase()}", 40f, y, paint)
        
        y += 30f
        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = Color.GRAY
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        canvas.drawText("Généré le : ${dateFormat.format(Date())}", 40f, y, paint)

        y += 40f
        paint.color = Color.BLACK
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("RÉSUMÉ DE LA SITUATION", 40f, y, paint)

        y += 25f
        paint.textSize = 12f
        paint.isFakeBoldText = false
        val stats = state.financialSummary
        canvas.drawText("Balance Globale : ${stats.globalBalance} F", 50f, y, paint); y += 20f
        canvas.drawText("Prêts Actifs : ${stats.totalActiveLoans} F", 50f, y, paint); y += 20f
        canvas.drawText("Gains Prévus : ${stats.forecastGains} F", 50f, y, paint); y += 20f
        canvas.drawText("Montant Prêtable : ${stats.lendableAmount} F", 50f, y, paint)

        y += 40f
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("LISTE DES MEMBRES", 40f, y, paint)

        y += 25f
        paint.textSize = 10f
        paint.isFakeBoldText = false
        state.displayMembers.forEach { member ->
            if (y > 780) { // New page
                pdfDocument.finishPage(page)
                // New page logic would go here
            }
            canvas.drawText("${member.displayName.uppercase()} - ${member.role.displayName}", 50f, y, paint)
            y += 15f
        }

        pdfDocument.finishPage(page)

        // PAGE 2: CHARTS
        val page2Info = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        val page2 = pdfDocument.startPage(page2Info)
        val canvas2 = page2.canvas
        
        y = 40f
        paint.textSize = 18f
        paint.isFakeBoldText = true
        paint.color = Color.BLACK
        canvas2.drawText("ANALYSE VISUELLE", 40f, y, paint)

        y += 40f
        paint.textSize = 12f
        canvas2.drawText("Croissance de la Trésorerie", 40f, y, paint)
        
        y += 20f
        drawGrowthChartOnCanvas(canvas2, 40f, y, 515f, 200f, state.growthStats)
        
        y += 250f
        canvas2.drawText("Répartition des Revenus", 40f, y, paint)
        
        y += 40f
        drawIncomeDistributionOnCanvas(canvas2, 595f/2f, y + 100f, 100f, state.incomeStats)

        pdfDocument.finishPage(page2)

        // Save file
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        val file = File(reportsDir, "Rapport_${state.tontineWithCaisses.tontine.libelle.replace(" ", "_")}.pdf")

        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        // Share
        sharePdf(context, file)
    }

    private fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Partager le rapport"))
    }

    private fun drawGrowthChartOnCanvas(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, data: List<com.indiza.cotisation.bank.SimpleTransaction>) {
        if (data.isEmpty()) return
        val paint = Paint().apply {
            color = Color.parseColor("#22d3ee") // NeonCyan
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }

        var current = 0L
        val points = data.map { current += it.montant; current }
        val max = points.maxOrNull() ?: 1L
        val min = points.minOrNull() ?: 0L
        val range = (max - min).coerceAtLeast(1L)
        val spacing = width / (points.size - 1).coerceAtLeast(1)

        val path = android.graphics.Path()
        points.forEachIndexed { index, value ->
            val px = x + (index * spacing)
            val py = y + height - ((value - min).toFloat() / range * height)
            if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        canvas.drawPath(path, paint)
    }

    private fun drawIncomeDistributionOnCanvas(canvas: Canvas, cx: Float, cy: Float, radius: Float, data: List<com.indiza.cotisation.bank.IncomeStats>) {
        if (data.isEmpty()) return
        val colors = listOf("#22d3ee", "#fb923c", "#10b981", "#a855f7", "#ef4444")
        val paint = Paint().apply { style = Paint.Style.FILL }
        val total = data.sumOf { it.total }.coerceAtLeast(1L)
        
        var startAngle = -90f
        val rect = android.graphics.RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        data.forEachIndexed { index, stats ->
            val sweep = (stats.total.toFloat() / total) * 360f
            paint.color = Color.parseColor(colors[index % colors.size])
            canvas.drawArc(rect, startAngle, sweep, true, paint)
            startAngle += sweep
        }
    }
}
