package com.surgical.eq.htcne76.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.surgical.eq.htcne76.data.EqBand
import com.surgical.eq.htcne76.ui.theme.DarkBorder
import com.surgical.eq.htcne76.ui.theme.DarkTextSecondary
import com.surgical.eq.htcne76.ui.theme.ElectricBlue
import com.surgical.eq.htcne76.ui.theme.NeonCyan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10

@Composable
fun FrequencyCurveCanvas(
    bands: List<EqBand>,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    val minFreq = 20.0f
    val maxFreq = 20000.0f
    val logMin = log10(minFreq)
    val logMax = log10(maxFreq)
    val logSpan = logMax - logMin

    val minDb = -15.0f
    val maxDb = 15.0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(105.dp)
            .background(Color.Black, RoundedCornerShape(8.dp))
            .border(0.7.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val midY = height / 2f

            // 1. Línea 0 dB
            drawLine(
                color = DarkBorder.copy(alpha = 0.8f),
                start = Offset(0f, midY),
                end = Offset(width, midY),
                strokeWidth = 1f
            )

            // Líneas +10dB y -10dB
            val y10Db = midY - (10f / maxDb) * midY
            val yMinus10Db = midY + (10f / maxDb) * midY
            drawLine(
                color = DarkBorder.copy(alpha = 0.3f),
                start = Offset(0f, y10Db),
                end = Offset(width, y10Db),
                strokeWidth = 0.8f
            )
            drawLine(
                color = DarkBorder.copy(alpha = 0.3f),
                start = Offset(0f, yMinus10Db),
                end = Offset(width, yMinus10Db),
                strokeWidth = 0.8f
            )

            // Frecuencias guía: 100 Hz, 1 kHz, 10 kHz
            val gridFreqs = listOf(100f, 1000f, 10000f)
            gridFreqs.forEach { f ->
                val x = width * ((log10(f) - logMin) / logSpan)
                drawLine(
                    color = DarkBorder.copy(alpha = 0.3f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 0.8f
                )
            }

            // 2. Curva combinada
            val numSteps = 100
            val path = Path()
            val fillPath = Path()
            var isFirst = true

            for (step in 0..numSteps) {
                val ratio = step.toFloat() / numSteps
                val freq = minFreq * (maxFreq / minFreq).toDouble().pow(ratio.toDouble()).toFloat()
                val x = width * ratio

                var totalGainDb = 0.0f
                if (isEnabled) {
                    bands.filter { it.isEnabled }.forEach { b ->
                        val f0 = b.frequencyHz
                        val q = b.qFactor.coerceAtLeast(0.2f)
                        val logDiff = ln(freq / f0)
                        val bw = 1.0f / q
                        val bell = exp(-0.5f * (logDiff / bw) * (logDiff / bw))
                        totalGainDb += b.gainDb * bell
                    }
                }

                val clampedGain = totalGainDb.coerceIn(minDb, maxDb)
                val y = midY - (clampedGain / maxDb) * midY

                if (isFirst) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                    isFirst = false
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            fillPath.lineTo(width, height)
            fillPath.close()

            if (isEnabled) {
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
            }

            drawPath(
                path = path,
                color = if (isEnabled) NeonCyan else DarkTextSecondary.copy(alpha = 0.4f),
                style = Stroke(width = 2f)
            )

            // Puntos destacados
            if (isEnabled) {
                bands.forEach { b ->
                    val x = width * ((log10(b.frequencyHz.coerceIn(minFreq, maxFreq)) - logMin) / logSpan)
                    val y = midY - (b.gainDb.coerceIn(minDb, maxDb) / maxDb) * midY

                    drawCircle(
                        color = ElectricBlue.copy(alpha = 0.35f),
                        radius = 6f,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = if (b.isEnabled) NeonCyan else DarkTextSecondary,
                        radius = 3f,
                        center = Offset(x, y)
                    )
                }
            }
        }

        Text(
            text = "60Hz",
            color = DarkTextSecondary,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.BottomStart)
        )
        Text(
            text = "1kHz",
            color = DarkTextSecondary,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        Text(
            text = "16kHz",
            color = DarkTextSecondary,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

private fun Double.pow(exp: Double): Double = Math.pow(this, exp)
