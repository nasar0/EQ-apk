package com.surgical.eq.htcne76.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.surgical.eq.htcne76.ui.components.FrequencyCurveCanvas
import com.surgical.eq.htcne76.ui.components.HybridSliderField
import com.surgical.eq.htcne76.ui.components.PresetBar
import com.surgical.eq.htcne76.ui.components.SavePresetDialog
import com.surgical.eq.htcne76.ui.components.VerticalFader
import com.surgical.eq.htcne76.ui.theme.AcidPink
import com.surgical.eq.htcne76.ui.theme.CyberAmber
import com.surgical.eq.htcne76.ui.theme.DarkBorder
import com.surgical.eq.htcne76.ui.theme.DarkSurface
import com.surgical.eq.htcne76.ui.theme.DarkSurfaceVariant
import com.surgical.eq.htcne76.ui.theme.DarkTextPrimary
import com.surgical.eq.htcne76.ui.theme.DarkTextSecondary
import com.surgical.eq.htcne76.ui.theme.ElectricBlue
import com.surgical.eq.htcne76.ui.theme.EmeraldActive
import com.surgical.eq.htcne76.ui.theme.NeonCyan
import com.surgical.eq.htcne76.ui.theme.OledBlack
import com.surgical.eq.htcne76.ui.viewmodel.EqUiState
import com.surgical.eq.htcne76.ui.viewmodel.EqViewModel

@Composable
fun EqualizerScreen(
    viewModel: EqViewModel,
    uiState: EqUiState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var selectedBandIndex by remember { mutableIntStateOf(0) }

    if (showSaveDialog) {
        SavePresetDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                viewModel.saveCustomPreset(name)
                showSaveDialog = false
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Barra Maestra Compacta: Switch + Presets
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(8.dp))
                .border(0.7.dp, if (uiState.isMasterEnabled) NeonCyan else DarkBorder, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(6.dp)
                            .background(
                                if (uiState.isMasterEnabled) EmeraldActive else DarkTextSecondary,
                                RoundedCornerShape(3.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (uiState.isMasterEnabled) "DSP SURGICAL ACTIVO" else "BYPASS (APAGADO)",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = if (uiState.isMasterEnabled) EmeraldActive else DarkTextSecondary
                    )
                }

                Switch(
                    checked = uiState.isMasterEnabled,
                    onCheckedChange = { viewModel.setMasterEnabled(it) },
                    modifier = Modifier.height(24.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = DarkTextSecondary,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Presets
            PresetBar(
                presets = uiState.allPresets,
                selectedPresetId = uiState.selectedPresetId,
                onPresetSelected = { viewModel.selectPreset(it) },
                onSaveNewPresetClicked = { showSaveDialog = true },
                onDeleteCustomPresetClicked = { viewModel.deleteCustomPreset(it) }
            )
        }

        // 2. Gráfico Interactivo de Curva de Frecuencia
        FrequencyCurveCanvas(
            bands = uiState.bands,
            isEnabled = uiState.isMasterEnabled
        )

        // 3. CONSOLA DE FADERS ESTUDIO (6 Bandas en Paralelo)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(8.dp))
                .border(0.7.dp, DarkBorder, RoundedCornerShape(8.dp))
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONSOLA DE BANDAS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTextSecondary
                )
                Text(
                    text = "Toca un fader para ajuste fino",
                    fontSize = 8.5.sp,
                    color = DarkTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Faders verticales lado a lado (todos visibles sin scroll)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                uiState.bands.forEachIndexed { idx, band ->
                    VerticalFader(
                        band = band,
                        isSelected = selectedBandIndex == idx,
                        onGainChanged = { newGain ->
                            viewModel.updateBand(band.index, band.frequencyHz, newGain, band.qFactor)
                        },
                        onSelected = {
                            selectedBandIndex = idx
                        },
                        isEnabled = uiState.isMasterEnabled,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Panel de Ajuste Fino Numérico para la Banda Seleccionada
        val activeBand = uiState.bands.getOrNull(selectedBandIndex)
        if (activeBand != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(8.dp))
                    .border(0.7.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AJUSTE QUIRÚRGICO • BANDA ${selectedBandIndex + 1} (${activeBand.frequencyHz.toInt()} Hz)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )

                    Switch(
                        checked = activeBand.isEnabled,
                        onCheckedChange = { viewModel.toggleBandEnabled(activeBand.index) },
                        enabled = uiState.isMasterEnabled,
                        modifier = Modifier.height(20.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = ElectricBlue,
                            uncheckedThumbColor = DarkTextSecondary,
                            uncheckedTrackColor = DarkSurfaceVariant
                        )
                    )
                }

                // Control Ganancia exacta
                HybridSliderField(
                    title = "Ganancia de Banda",
                    value = activeBand.gainDb,
                    onValueChange = { viewModel.updateBand(activeBand.index, activeBand.frequencyHz, it, activeBand.qFactor) },
                    range = -15.0f..15.0f,
                    unit = "dB",
                    decimalPlaces = 1,
                    accentColor = NeonCyan,
                    enabled = uiState.isMasterEnabled && activeBand.isEnabled
                )

                // Control Frecuencia exacta
                HybridSliderField(
                    title = "Frecuencia Central Exacta",
                    value = activeBand.frequencyHz,
                    onValueChange = { viewModel.updateBand(activeBand.index, it, activeBand.gainDb, activeBand.qFactor) },
                    range = 20.0f..20000.0f,
                    unit = "Hz",
                    decimalPlaces = 0,
                    accentColor = ElectricBlue,
                    enabled = uiState.isMasterEnabled && activeBand.isEnabled
                )

                // Control Factor Q exacto
                HybridSliderField(
                    title = "Ancho de Campana (Factor Q)",
                    value = activeBand.qFactor,
                    onValueChange = { viewModel.updateBand(activeBand.index, activeBand.frequencyHz, activeBand.gainDb, it) },
                    range = 0.30f..5.00f,
                    unit = "Q",
                    decimalPlaces = 2,
                    accentColor = CyberAmber,
                    enabled = uiState.isMasterEnabled && activeBand.isEnabled
                )
            }
        }

        // 5. Rack de Efectos Dinámicos en Grid 2x2 Compacto
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(8.dp))
                .border(0.7.dp, DarkBorder, RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "RACK DE EFECTOS DSP",
                fontFamily = FontFamily.Monospace,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HybridSliderField(
                    title = "Bass Boost",
                    value = uiState.bassBoost.toFloat(),
                    onValueChange = { viewModel.setBassBoost(it.toInt()) },
                    range = 0f..1000f,
                    unit = "",
                    decimalPlaces = 0,
                    accentColor = NeonCyan,
                    enabled = uiState.isMasterEnabled,
                    modifier = Modifier.weight(1f)
                )

                HybridSliderField(
                    title = "Loudness",
                    value = uiState.loudnessDb,
                    onValueChange = { viewModel.setLoudnessDb(it) },
                    range = 0.0f..15.0f,
                    unit = "dB",
                    decimalPlaces = 1,
                    accentColor = ElectricBlue,
                    enabled = uiState.isMasterEnabled,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HybridSliderField(
                    title = "Espacializador 3D",
                    value = uiState.virtualizer.toFloat(),
                    onValueChange = { viewModel.setVirtualizer(it.toInt()) },
                    range = 0f..1000f,
                    unit = "",
                    decimalPlaces = 0,
                    accentColor = AcidPink,
                    enabled = uiState.isMasterEnabled,
                    modifier = Modifier.weight(1f)
                )

                HybridSliderField(
                    title = "Pre-Amp / Limiter",
                    value = uiState.preAmpDb,
                    onValueChange = { viewModel.setPreAmpDb(it) },
                    range = -15.0f..15.0f,
                    unit = "dB",
                    decimalPlaces = 1,
                    accentColor = CyberAmber,
                    enabled = uiState.isMasterEnabled,
                    modifier = Modifier.weight(1f)
                )
            }

            // Diagnóstico de motor de audio
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black, RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(5.dp)
                        .background(EmeraldActive, RoundedCornerShape(2.5.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Motor Híbrido: DynamicsProcessing + Universal AudioFX (Prioridad 1000)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = DarkTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
