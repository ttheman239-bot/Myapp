package com.example.myapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Minimal line chart for an equity curve starting at 1.0. */
@Composable
fun EquityChart(
    curve: List<Double>,
    modifier: Modifier = Modifier,
) {
    if (curve.size < 2) return
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val baseline = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val w = size.width
        val h = size.height
        val minV = curve.min()
        val maxV = curve.max()
        val range = (maxV - minV).takeIf { it > 0 } ?: 1.0
        val pad = 8f

        // Horizontal baseline at value = 1.0 if within range
        if (1.0 in minV..maxV) {
            val y = (h - pad) - ((1.0 - minV) / range * (h - 2 * pad)).toFloat()
            drawLine(
                color = grid,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.5f,
            )
        }

        // Draw the line
        val path = Path()
        curve.forEachIndexed { i, v ->
            val x = (i.toFloat() / (curve.size - 1).toFloat()) * w
            val y = (h - pad) - ((v - minV) / range * (h - 2 * pad)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = line,
            style = Stroke(width = 3f),
        )

        // Corner baseline markers
        drawLine(
            color = baseline.copy(alpha = 0.3f),
            start = Offset(0f, h - pad),
            end = Offset(w, h - pad),
            strokeWidth = 1f,
        )
    }
}
