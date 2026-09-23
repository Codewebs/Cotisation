package com.indiza.cotisation.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.indiza.cotisation.bank.IncomeStats
import com.indiza.cotisation.bank.SimpleTransaction

@Composable
fun GrowthLineChart(
    data: List<SimpleTransaction>,
    modifier: Modifier = Modifier,
    lineColor: Color = NeonCyan
) {
    if (data.isEmpty()) return

    // Calculer le solde cumulé
    var currentBalance = 0L
    val points = data.map {
        currentBalance += it.montant
        currentBalance
    }

    val maxVal = points.maxOrNull() ?: 1L
    val minVal = points.minOrNull() ?: 0L
    val range = (maxVal - minVal).coerceAtLeast(1L)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val spacing = width / (points.size - 1).coerceAtLeast(1)

        val path = Path().apply {
            points.forEachIndexed { index, value ->
                val x = index * spacing
                val y = height - ((value - minVal).toFloat() / range * height)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
fun IncomeDistributionChart(
    data: List<IncomeStats>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val total = data.sumOf { it.total }.coerceAtLeast(1L)
    val colors = listOf(NeonCyan, AfroOrange, Color(0xFF50FA7B), Color(0xFFA855F7), Color(0xFFFF5555))

    Canvas(modifier = modifier) {
        var startAngle = -90f
        data.forEachIndexed { index, stats ->
            val sweepAngle = (stats.total.toFloat() / total) * 360f
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                size = Size(size.width, size.height)
            )
            startAngle += sweepAngle
        }
    }
}
