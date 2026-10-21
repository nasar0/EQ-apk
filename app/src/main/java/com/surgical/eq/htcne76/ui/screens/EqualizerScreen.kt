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
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Tarjeta Maestra: Switch Master + Insignia HTC NE76
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(14.dp))
                .border(1.dp, if (uiState.isMasterEnabled) NeonCyan else DarkBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(8.dp)
                                .background(
                                    if (uiState.isMasterEnabled) EmeraldActive else DarkTextSecondary,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isMasterEnabled) "DSP SURGICAL ACTIVO" else "DSP EN BYPASS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (uiState.isMasterEnabled) EmeraldActive else DarkTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "HTC NE76 Acoustic Tuning",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkTextPrimary
                    )
                }

                Switch(
                    checked = uiState.isMasterEnabled,
                    onCheckedChange = { viewModel.setMasterEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = DarkTextSecondary,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selector de Presets
            PresetBar(
                presets = uiState.allPresets,
                selectedPresetId = uiState.selectedPresetId,
                onPresetSelected = { viewModel.selectPreset(it) },
                onSaveNewPresetClicked = { showSaveDialog = true },
                onDeleteCustomPresetClicked = { viewModel.deleteCustomPreset(it) }
            )
        }

        // 2. Gráfico Interactivo de Respuesta en Frecuencia (Canvas)
        FrequencyCurveCanvas(
            bands = uiState.bands,
            isEnabled = uiState.isMasterEnabled
        )

        // 3. Bandas Quirúrgicas Paramétricas (Sliders + Cajas numéricas exactas)
        Text(
            text = "BANDAS PARAMÉTRICAS QUIRÚRGICAS",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.sp,
                color = NeonCyan
            )
        )

        val bandDescriptions = listOf(
            "SUB-GRAVES (Pegada profunda y retumbe limpio)",
            "GRAVES MEDIOS (Cuerpo de bombo y bajo)",
            "MEDIOS-GRAVES (Anti-encajonado y claridad vocal)",
            "MEDIOS (Presencia vocal y guitarras)",
            "AGUDOS / PRESENCIA (Detalle y articulación)",
            "AIRE / BRILLO SUPERIOR (Extensión cristalina sin fatiga)"
        )

        uiState.bands.forEachIndexed { index, band ->
            val description = bandDescriptions.getOrElse(index) { "BANDA ${index + 1}" }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BANDA ${index + 1}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = ElectricBlue
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 11.sp,
                                color = DarkTextSecondary
                            )
                        )
                    }

                    Switch(
                        checked = band.isEnabled,
                        onCheckedChange = { viewModel.toggleBandEnabled(band.index) },
                        enabled = uiState.isMasterEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = ElectricBlue,
                            uncheckedThumbColor = DarkTextSecondary,
                            uncheckedTrackColor = DarkSurfaceVariant
                        )
                    )
                }

                // Control Híbrido 1: Ganancia (dB) con decimales exactos
                HybridSliderField(
                    title = "Ganancia",
                    value = band.gainDb,
                    onValueChange = { newGain ->
                        viewModel.updateBand(band.index, band.frequencyHz, newGain, band.qFactor)
                    },
                    range = -15.0f..15.0f,
                    unit = "dB",
                    decimalPlaces = 1,
                    accentColor = NeonCyan,
                    enabled = uiState.isMasterEnabled && band.isEnabled
                )

                // Control Híbrido 2: Frecuencia Central (Hz) exacta
                HybridSliderField(
                    title = "Frecuencia Central",
                    value = band.frequencyHz,
                    onValueChange = { newFreq ->
                        viewModel.updateBand(band.index, newFreq, band.gainDb, band.qFactor)
                    },
                    range = 20.0f..20000.0f,
                    unit = "Hz",
                    decimalPlaces = 0,
                    accentColor = ElectricBlue,
                    enabled = uiState.isMasterEnabled && band.isEnabled
                )

                // Control Híbrido 3: Factor Q (Ancho de banda) exacto
                HybridSliderField(
                    title = "Factor Q (Ancho de Campana)",
                    value = band.qFactor,
                    onValueChange = { newQ ->
                        viewModel.updateBand(band.index, band.frequencyHz, band.gainDb, newQ)
                    },
                    range = 0.30f..5.00f,
                    unit = "Q",
                    decimalPlaces = 2,
                    accentColor = CyberAmber,
                    enabled = uiState.isMasterEnabled && band.isEnabled
                )
            }
        }

        // 4. Etapa DSP de Efectos, Dinámica y Limitador
        Text(
            text = "PROCESAMIENTO DINÁMICO & EFECTOS HTC NE76",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.sp,
                color = CyberAmber
            )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(12.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Bass Boost Híbrido
            HybridSliderField(
                title = "Bass Boost (Refuerzo de Graves)",
                value = uiState.bassBoost.toFloat(),
                onValueChange = { viewModel.setBassBoost(it.toInt()) },
                range = 0f..1000f,
                unit = "pts",
                decimalPlaces = 0,
                accentColor = NeonCyan,
                enabled = uiState.isMasterEnabled
            )

            // Loudness Enhancer Híbrido
            HybridSliderField(
                title = "Loudness Enhancer (Realce Acústico)",
                value = uiState.loudnessDb,
                onValueChange = { viewModel.setLoudnessDb(it) },
                range = 0.0f..15.0f,
                unit = "dB",
                decimalPlaces = 1,
                accentColor = ElectricBlue,
                enabled = uiState.isMasterEnabled
            )

            // Virtualizer / 3D Espacializador
            HybridSliderField(
                title = "Espacializador Virtual 3D",
                value = uiState.virtualizer.toFloat(),
                onValueChange = { viewModel.setVirtualizer(it.toInt()) },
                range = 0f..1000f,
                unit = "pts",
                decimalPlaces = 0,
                accentColor = AcidPink,
                enabled = uiState.isMasterEnabled
            )

            // Pre-Amp Gain / Headroom
            HybridSliderField(
                title = "Ganancia Pre-Amp (Headroom Limitador)",
                value = uiState.preAmpDb,
                onValueChange = { viewModel.setPreAmpDb(it) },
                range = -15.0f..15.0f,
                unit = "dB",
                decimalPlaces = 1,
                accentColor = CyberAmber,
                enabled = uiState.isMasterEnabled
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .background(EmeraldActive, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DynamicsProcessing Limiter Activo: Protección contra distorsión y clipping inter-sample en picos altos.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 11.sp,
                        color = DarkTextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
