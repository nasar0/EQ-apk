package com.surgical.eq.htcne76.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.surgical.eq.htcne76.ui.theme.CyberAmber
import com.surgical.eq.htcne76.ui.theme.DarkBorder
import com.surgical.eq.htcne76.ui.theme.DarkSurfaceVariant
import com.surgical.eq.htcne76.ui.theme.DarkTextPrimary
import com.surgical.eq.htcne76.ui.theme.DarkTextSecondary
import com.surgical.eq.htcne76.ui.theme.EmeraldActive
import com.surgical.eq.htcne76.ui.theme.WarningRed
import java.util.Locale

@Composable
fun DecibelMeterView(
    ambientDb: Float,
    isMicActive: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = ((ambientDb - 20f) / 70f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 150),
        label = "meterProgress"
    )

    val meterColor by animateColorAsState(
        targetValue = when {
            ambientDb < 45f -> EmeraldActive
            ambientDb < 65f -> CyberAmber
            else -> WarningRed
        },
        label = "meterColor"
    )

    val statusDescription = when {
        !isMicActive -> "Micrófono en reposo • Pulsa para calibrar"
        ambientDb < 40f -> "Silencioso • Óptimo para descanso"
        ambientDb < 55f -> "Ruido moderado • Conversación / Hogar"
        ambientDb < 70f -> "Ruido alto • Motores / Tráfico (ANC activo)"
        else -> "Ruido crítico • Máximo aislamiento activado"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SONÓMETRO AMBIENTE EN TIEMPO REAL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = DarkTextSecondary,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusDescription,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isMicActive) meterColor else DarkTextSecondary
                    )
                )
            }

            // Lectura digital grande
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format(Locale.US, "%.1f", ambientDb),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMicActive) meterColor else DarkTextSecondary
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "dB SPL",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = DarkTextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Barra de vúmetro de decibelios con segmentos degradados
        LinearProgressIndicator(
            progress = { animatedProgress },
            color = meterColor,
            trackColor = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .border(0.5.dp, DarkBorder, RoundedCornerShape(4.dp))
        )
    }
}
