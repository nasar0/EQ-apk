package com.surgical.eq.htcne76.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.surgical.eq.htcne76.data.Preset
import com.surgical.eq.htcne76.ui.theme.DarkBorder
import com.surgical.eq.htcne76.ui.theme.DarkSurfaceVariant
import com.surgical.eq.htcne76.ui.theme.DarkTextPrimary
import com.surgical.eq.htcne76.ui.theme.DarkTextSecondary
import com.surgical.eq.htcne76.ui.theme.NeonCyan
import com.surgical.eq.htcne76.ui.theme.NeonCyanDim

@Composable
fun PresetBar(
    presets: List<Preset>,
    selectedPresetId: String,
    onPresetSelected: (Preset) -> Unit,
    onSaveNewPresetClicked: () -> Unit,
    onDeleteCustomPresetClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        presets.forEach { preset ->
            val isSelected = preset.id == selectedPresetId

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        if (isSelected) NeonCyanDim else DarkSurfaceVariant,
                        RoundedCornerShape(14.dp)
                    )
                    .border(
                        0.8.dp,
                        if (isSelected) NeonCyan else DarkBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onPresetSelected(preset) }
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = preset.name,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) NeonCyan else DarkTextPrimary
                )

                if (preset.isCustom) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Eliminar",
                        tint = DarkTextSecondary,
                        modifier = Modifier
                            .size(11.dp)
                            .clickable { onDeleteCustomPresetClicked(preset.id) }
                    )
                }
            }
        }

        // Botón Guardar Preset
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color.Black, RoundedCornerShape(14.dp))
                .border(0.8.dp, DarkBorder, RoundedCornerShape(14.dp))
                .clickable { onSaveNewPresetClicked() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Guardar",
                tint = NeonCyan,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "Guardar",
                fontSize = 9.5.sp,
                color = NeonCyan,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
