package com.omega.devicemonitor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omega.devicemonitor.data.local.entity.SensorReadingEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TelemetrySparklineChart(
    readings: List<SensorReadingEntity>,
    lineColor: Color,
    unit: String,
    modifier: Modifier = Modifier,
    isFahrenheit: Boolean = false,
    sensorType: String = ""
) {
    if (readings.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No telemetry points recorded in selected period",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Process and sort readings chronologically (oldest to newest)
    val sorted = remember(readings) { readings.sortedBy { it.timestamp } }

    val values = remember(sorted, isFahrenheit, sensorType) {
        sorted.map { r ->
            if (sensorType.equals("TEMPERATURE", ignoreCase = true) && isFahrenheit && r.unit.contains("C")) {
                (r.value * 9.0 / 5.0) + 32.0
            } else {
                r.value
            }
        }
    }

    val displayUnit = remember(unit, isFahrenheit, sensorType) {
        if (sensorType.equals("TEMPERATURE", ignoreCase = true) && isFahrenheit && unit.contains("C")) "°F" else unit
    }

    val minValue = values.minOrNull() ?: 0.0
    val maxValue = values.maxOrNull() ?: 1.0
    val avgValue = values.average()
    val valueRange = if (maxValue == minValue) 1.0 else (maxValue - minValue)

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier) {
        // Selected or latest reading preview bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val targetIdx = selectedIndex ?: (sorted.size - 1)
            val currentVal = values[targetIdx]
            val currentTimestamp = sorted[targetIdx].timestamp
            val isManual = sorted[targetIdx].isManual

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(Locale.US, "%.1f %s", currentVal, displayUnit),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = lineColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isManual) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isManual) "MANUAL" else "HARDWARE",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isManual) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            val timeStr = remember(currentTimestamp) {
                val sdf = SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault())
                sdf.format(Date(currentTimestamp))
            }
            Text(
                text = timeStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Canvas Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            val avgLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(values) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val step = if (values.size > 1) width / (values.size - 1) else width
                            val tappedIdx = ((offset.x + step / 2) / step).toInt().coerceIn(0, values.size - 1)
                            selectedIndex = tappedIdx
                        }
                    }
            ) {
                val width = size.width
                val height = size.height

                // Draw horizontal grid lines (top, middle, bottom)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, 0f),
                    end = Offset(width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, height / 2),
                    end = Offset(width, height / 2),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, height),
                    end = Offset(width, height),
                    strokeWidth = 1.dp.toPx()
                )

                // Draw Average dotted line
                val avgY = (height - ((avgValue - minValue) / valueRange * (height * 0.8f) + height * 0.1f)).toFloat()
                drawLine(
                    color = avgLineColor,
                    start = Offset(0f, avgY),
                    end = Offset(width, avgY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )

                if (values.size == 1) {
                    // Single point representation
                    val singleY = height / 2
                    drawCircle(
                        color = lineColor,
                        radius = 6.dp.toPx(),
                        center = Offset(width / 2, singleY)
                    )
                    return@Canvas
                }

                // Compute point coordinates
                val stepX = width / (values.size - 1)
                val points = values.mapIndexed { index, value ->
                    val normalized = ((value - minValue) / valueRange).toFloat()
                    val y = height - (normalized * (height * 0.75f) + height * 0.12f)
                    Offset(index * stepX, y)
                }

                // Fill area path under curve
                val fillPath = Path().apply {
                    moveTo(points.first().x, height)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.35f),
                            lineColor.copy(alpha = 0.0f)
                        ),
                        startY = 0f,
                        endY = height
                    )
                )

                // Smooth line path
                val strokePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val midX = (prev.x + curr.x) / 2
                        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }
                }

                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Highlight selected index or last point
                val activeIdx = selectedIndex ?: (points.size - 1)
                val activePoint = points[activeIdx]

                // Glowing outer ring
                drawCircle(
                    color = lineColor.copy(alpha = 0.25f),
                    radius = 9.dp.toPx(),
                    center = activePoint
                )
                // Solid center dot
                drawCircle(
                    color = lineColor,
                    radius = 4.5.dp.toPx(),
                    center = activePoint
                )
                // Center core white
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = activePoint
                )
            }
        }

        // Min / Avg / Max statistics badge row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatLabel(label = "MIN", value = String.format(Locale.US, "%.1f %s", minValue, displayUnit))
            StatLabel(label = "AVG", value = String.format(Locale.US, "%.1f %s", avgValue, displayUnit))
            StatLabel(label = "MAX", value = String.format(Locale.US, "%.1f %s", maxValue, displayUnit))
            StatLabel(label = "POINTS", value = "${readings.size}")
        }
    }
}

@Composable
private fun StatLabel(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
