package com.surgical.eq.htcne76.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.surgical.eq.htcne76.data.EqBand
import com.surgical.eq.htcne76.ui.theme.DarkBorder
import com.surgical.eq.htcne76.ui.theme.DarkSurface
import com.surgical.eq.htcne76.ui.theme.DarkSurfaceVariant
import com.surgical.eq.htcne76.ui.theme.DarkTextPrimary
import com.surgical.eq.htcne76.ui.theme.DarkTextSecondary
import com.surgical.eq.htcne76.ui.theme.ElectricBlue
import com.surgical.eq.htcne76.ui.theme.NeonCyan
import java.util.Locale

@Composable
fun VerticalFader(
    band: EqBand,
    isSelected: Boolean,
    onGainChanged: (Float) -> Unit,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    val minDb = -15.0f
    val maxDb = 15.0f
    val dbRange = maxDb - minDb

    val freqLabel = when {
        band.frequencyHz >= 1000f -> "${String.format(Locale.US, "%.1f", band.frequencyHz / 1000f).replace(".0", "")}k"
        else -> "${band.frequencyHz.toInt()}"
    }

    val gainLabel = if (band.gainDb > 0) {
        "+${String.format(Locale.US, "%.1f", band.gainDb)}"
    } else {
        String.format(Locale.US, "%.1f", band.gainDb)
    }

    Column(
        modifier = modifier
            .background(
                if (isSelected) DarkSurfaceVariant else DarkSurface,
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (isSelected) NeonCyan else DarkBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onSelected() }
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Frecuencia
        Text(
            text = freqLabel,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) NeonCyan else DarkTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Pista y Knob vertical táctil
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(130.dp)
                .pointerInput(band.gainDb, isEnabled) {
                    if (!isEnabled) return@pointerInput
                    detectTapGestures { offset ->
                        val ratio = 1.0f - (offset.y / size.height).coerceIn(0f, 1f)
                        val newGain = minDb + ratio * dbRange
                        onGainChanged(newGain.coerceIn(minDb, maxDb))
                        onSelected()
                    }
                }
                .pointerInput(band.gainDb, isEnabled) {
                    if (!isEnabled) return@pointerInput
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaRatio = -dragAmount.y / size.height
                        val deltaDb = deltaRatio * dbRange
                        val updated = (band.gainDb + deltaDb).coerceIn(minDb, maxDb)
                        onGainChanged(updated)
                        onSelected()
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                val width = size.width
                val height = size.height
                val midX = width / 2f
                val midY = height / 2f

                // 1. Pista de fondo (Track)
                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(midX - 2.5f, 0f),
                    size = Size(5f, height),
                    cornerRadius = CornerRadius(2.5f, 2.5f)
                )

                // 2. Línea central 0 dB
                drawLine(
                    color = DarkBorder,
                    start = Offset(midX - 8f, midY),
                    end = Offset(midX + 8f, midY),
                    strokeWidth = 1.5f
                )

                // 3. Posición del knob
                val normalizedRatio = ((band.gainDb - minDb) / dbRange).coerceIn(0f, 1f)
                val knobY = height - (normalizedRatio * height)

                // Barra iluminada desde el centro (0dB) hasta el knob
                val barTop = if (knobY < midY) knobY else midY
                val barHeight = kotlin.math.abs(knobY - midY)
                if (barHeight > 1f && isEnabled) {
                    drawRoundRect(
                        color = if (band.gainDb >= 0) NeonCyan.copy(alpha = 0.6f) else ElectricBlue.copy(alpha = 0.6f),
                        topLeft = Offset(midX - 2.5f, barTop),
                        size = Size(5f, barHeight),
                        cornerRadius = CornerRadius(2.5f, 2.5f)
                    )
                }

                // 4. Knob deslizador táctil
                val knobWidth = 26f
                val knobHeight = 14f
                val knobLeft = midX - knobWidth / 2f
                val knobTop = (knobY - knobHeight / 2f).coerceIn(0f, height - knobHeight)

                // Sombra/borde exterior del knob
                drawRoundRect(
                    color = if (isSelected) NeonCyan else DarkBorder,
                    topLeft = Offset(knobLeft, knobTop),
                    size = Size(knobWidth, knobHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Centro del knob
                drawRoundRect(
                    color = if (isEnabled) Color(0xFF1E1E28) else Color(0xFF121216),
                    topLeft = Offset(knobLeft + 1.5f, knobTop + 1.5f),
                    size = Size(knobWidth - 3f, knobHeight - 3f),
                    cornerRadius = CornerRadius(3f, 3f)
                )

                // Línea indicadora brillante en el centro del knob
                drawLine(
                    color = if (isEnabled) (if (isSelected) NeonCyan else Color.White) else DarkTextSecondary,
                    start = Offset(knobLeft + 4f, knobTop + knobHeight / 2f),
                    end = Offset(knobLeft + knobWidth - 4f, knobTop + knobHeight / 2f),
                    strokeWidth = 2f
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Lectura de dB
        Text(
            text = gainLabel,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                !isEnabled -> DarkTextSecondary
                band.gainDb > 0.05f -> NeonCyan
                band.gainDb < -0.05f -> ElectricBlue
                else -> DarkTextSecondary
            },
            textAlign = TextAlign.Center
        )
    }
}
