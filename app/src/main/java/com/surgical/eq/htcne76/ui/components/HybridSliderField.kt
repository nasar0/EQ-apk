package com.surgical.eq.htcne76.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.surgical.eq.htcne76.ui.theme.DarkBorder
import com.surgical.eq.htcne76.ui.theme.DarkSurfaceVariant
import com.surgical.eq.htcne76.ui.theme.DarkTextPrimary
import com.surgical.eq.htcne76.ui.theme.DarkTextSecondary
import com.surgical.eq.htcne76.ui.theme.NeonCyan
import java.util.Locale

@Composable
fun HybridSliderField(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    modifier: Modifier = Modifier,
    decimalPlaces: Int = 1,
    accentColor: Color = NeonCyan,
    enabled: Boolean = true
) {
    val focusManager = LocalFocusManager.current
    var isEditing by remember { mutableStateOf(false) }

    fun formatNumber(v: Float): String {
        return if (decimalPlaces == 0) {
            String.format(Locale.US, "%.0f", v)
        } else {
            String.format(Locale.US, "%.${decimalPlaces}f", v)
        }
    }

    var textState by remember(value) {
        mutableStateOf(if (!isEditing) formatNumber(value) else "")
    }

    LaunchedEffect(value) {
        if (!isEditing) {
            textState = formatNumber(value)
        }
    }

    fun submitText() {
        val sanitized = textState.replace(',', '.')
        val parsed = sanitized.toFloatOrNull()
        if (parsed != null) {
            val clamped = parsed.coerceIn(range.start, range.endInclusive)
            onValueChange(clamped)
            textState = formatNumber(clamped)
        } else {
            textState = formatNumber(value)
        }
        isEditing = false
        focusManager.clearFocus()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .border(0.7.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 10.5.sp,
                color = if (enabled) DarkTextPrimary else DarkTextSecondary
            )

            // Entrada numérica compacta
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.Black, RoundedCornerShape(5.dp))
                    .border(
                        0.7.dp,
                        if (isEditing) accentColor else DarkBorder,
                        RoundedCornerShape(5.dp)
                    )
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                BasicTextField(
                    value = if (isEditing) textState else formatNumber(value),
                    onValueChange = { input -> textState = input },
                    enabled = enabled,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) accentColor else DarkTextSecondary,
                        textAlign = TextAlign.End
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(accentColor),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (range.start < 0) KeyboardType.Text else KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitText() }
                    ),
                    modifier = Modifier
                        .width(48.dp)
                        .onFocusChanged { focusState ->
                            isEditing = focusState.isFocused
                            if (focusState.isFocused) {
                                textState = formatNumber(value)
                            } else {
                                submitText()
                            }
                        }
                )

                Spacer(modifier = Modifier.width(2.dp))

                Text(
                    text = unit,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = DarkTextSecondary
                )
            }
        }

        // Slider ultra-compacto
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = { newVal ->
                if (!isEditing) onValueChange(newVal)
            },
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = DarkBorder,
                disabledThumbColor = DarkTextSecondary,
                disabledActiveTrackColor = DarkBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
        )
    }
}
