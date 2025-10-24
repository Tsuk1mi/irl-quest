package com.irlquest.app.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun StatsChart(
    data: List<DayData>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        if (data.isNotEmpty()) {
            val maxValue = data.maxOf { it.value }
            if (maxValue > 0) {
                val width = size.width
                val height = size.height
                val stepWidth = width / (data.size - 1)

                // Рисуем линию графика
                val path = Path()
                data.forEachIndexed { index, dayData ->
                    val x = index * stepWidth
                    val y = height - (dayData.value / maxValue * height)

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                // Рисуем линию
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Рисуем точки
                data.forEachIndexed { index, dayData ->
                    val x = index * stepWidth
                    val y = height - (dayData.value / maxValue * height)

                    drawCircle(
                        color = lineColor,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
