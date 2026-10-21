package com.surgical.eq.htcne76.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.surgical.eq.htcne76.ui.components.DecibelMeterView
import com.surgical.eq.htcne76.ui.components.HybridSliderField
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
import com.surgical.eq.htcne76.ui.theme.NeonCyanDim
import com.surgical.eq.htcne76.ui.theme.OledBlack
import com.surgical.eq.htcne76.ui.viewmodel.EqUiState
import com.surgical.eq.htcne76.ui.viewmodel.EqViewModel
import kotlin.math.roundToInt

@Composable
fun NoiseControlScreen(
    viewModel: EqViewModel,
    uiState: EqUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
    }

    val config = uiState.noiseControlConfig

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner de permisos de micrófono si no están concedidos
        if (!hasMicPermission) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1400), RoundedCornerShape(12.dp))
                    .border(1.dp, CyberAmber, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "PERMISO DE MICRÓFONO REQUERIDO",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberAmber
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Para capturar el ruido exterior, calcular la inversión de fase y medir los decibelios en tiempo real, la app requiere acceso al micrófono.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberAmber,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Conceder Permiso")
                }
            }
        }

        // 1. Sonómetro de ruido en tiempo real
        DecibelMeterView(
            ambientDb = uiState.ambientDb,
            isMicActive = hasMicPermission && (config.phaseInversionEnabled || config.smartMaskingEnabled)
        )

        // 2. MODO 1: ANC Experimental por Inversión de Fase (Phase Inversion)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(14.dp))
                .border(
                    1.dp,
                    if (config.phaseInversionEnabled) NeonCyan else DarkBorder,
                    RoundedCornerShape(14.dp)
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    if (config.phaseInversionEnabled) EmeraldActive else DarkTextSecondary,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MODO 1: ANC POR INVERSIÓN DE FASE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (config.phaseInversionEnabled) NeonCyan else DarkTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Cancelación Acústica Destructiva",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkTextPrimary
                    )
                }

                Switch(
                    checked = config.phaseInversionEnabled,
                    onCheckedChange = { isEnabled ->
                        if (isEnabled && !hasMicPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.updateNoiseControlConfig(config.copy(phaseInversionEnabled = isEnabled))
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = DarkTextSecondary,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }

            // Explicación técnica para el usuario
            Text(
                text = "Captura el ruido por el micrófono, filtra frecuencias largas (< 600 Hz) e invierte la polaridad (180°). Ajusta el desfase milisegundo a milisegundo hasta anular el zumbido exterior con la latencia de tus HTC NE76.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.sp,
                    color = DarkTextSecondary
                )
            )

            // Control Híbrido: Compensación de Latencia y Desfase Bluetooth (ms y muestras)
            val samplesCount = ((config.phaseDelayMs * 48000f) / 1000f).roundToInt()
            HybridSliderField(
                title = "Compensación Latencia / Fase ($samplesCount muestras)",
                value = config.phaseDelayMs,
                onValueChange = { newMs ->
                    viewModel.updateNoiseControlConfig(config.copy(phaseDelayMs = newMs))
                },
                range = 0.0f..50.0f,
                unit = "ms",
                decimalPlaces = 1,
                accentColor = NeonCyan,
                enabled = config.phaseInversionEnabled
            )

            // Control Híbrido: Filtro Paso-Bajo (< 600 Hz)
            HybridSliderField(
                title = "Frecuencia de Corte ANC (< 600 Hz)",
                value = config.lowPassCutoffHz,
                onValueChange = { newHz ->
                    viewModel.updateNoiseControlConfig(config.copy(lowPassCutoffHz = newHz))
                },
                range = 50.0f..600.0f,
                unit = "Hz",
                decimalPlaces = 0,
                accentColor = ElectricBlue,
                enabled = config.phaseInversionEnabled
            )

            // Control Híbrido: Ganancia de Inversión
            HybridSliderField(
                title = "Ganancia de Onda Anti-Ruido",
                value = config.inversionGain,
                onValueChange = { newGain ->
                    viewModel.updateNoiseControlConfig(config.copy(inversionGain = newGain))
                },
                range = 0.0f..1.50f,
                unit = "x",
                decimalPlaces = 2,
                accentColor = CyberAmber,
                enabled = config.phaseInversionEnabled
            )
        }

        // 3. MODO 2: Enmascaramiento Psicoacústico Inteligente (Smart Noise Masking)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(14.dp))
                .border(
                    1.dp,
                    if (config.smartMaskingEnabled) ElectricBlue else DarkBorder,
                    RoundedCornerShape(14.dp)
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    if (config.smartMaskingEnabled) EmeraldActive else DarkTextSecondary,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MODO 2: ENMASCARAMIENTO PSICOACÚSTICO",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (config.smartMaskingEnabled) ElectricBlue else DarkTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Aislamiento Orgánico para Dormir",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkTextPrimary
                    )
                }

                Switch(
                    checked = config.smartMaskingEnabled,
                    onCheckedChange = { isEnabled ->
                        viewModel.updateNoiseControlConfig(config.copy(smartMaskingEnabled = isEnabled))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = ElectricBlue,
                        uncheckedThumbColor = DarkTextSecondary,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }

            Text(
                text = "Genera una capa sintetizada de frecuencias armónicas (Ruido Marrón o Rosa) que neutraliza la percepción cerebral de voces, ladridos o ruidos súbitos de la casa mientras duermes.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.sp,
                    color = DarkTextSecondary
                )
            )

            // Selector de Perfil de Ruido (Marrón, Rosa, Híbrido)
            Text(
                text = "Perfil Espectral de Ruido:",
                style = MaterialTheme.typography.labelSmall.copy(color = DarkTextSecondary)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("BROWN", "Marrón", "Graves/Ronquidos"),
                    Triple("PINK", "Rosa", "Voces/Conversación"),
                    Triple("HYBRID", "Híbrido", "Balance Máximo")
                ).forEach { (type, label, desc) ->
                    val isSelected = config.noiseType == type

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) NeonCyanDim else DarkSurfaceVariant,
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) ElectricBlue else DarkBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                viewModel.updateNoiseControlConfig(config.copy(noiseType = type))
                            }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isSelected) ElectricBlue else DarkTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = desc,
                            fontSize = 9.sp,
                            color = DarkTextSecondary
                        )
                    }
                }
            }

            // Control Híbrido: Umbral dB de activación inteligente
            HybridSliderField(
                title = "Umbral de Activación Ambiental",
                value = config.ambientThresholdDb,
                onValueChange = { newThreshold ->
                    viewModel.updateNoiseControlConfig(config.copy(ambientThresholdDb = newThreshold))
                },
                range = 30.0f..80.0f,
                unit = "dB",
                decimalPlaces = 1,
                accentColor = ElectricBlue,
                enabled = config.smartMaskingEnabled
            )

            // Control Híbrido: Volumen máximo de la capa de enmascaramiento
            HybridSliderField(
                title = "Volumen Máximo de Máscara Acústica",
                value = config.maskingMaxGain * 100f,
                onValueChange = { newPct ->
                    viewModel.updateNoiseControlConfig(config.copy(maskingMaxGain = newPct / 100f))
                },
                range = 5.0f..100.0f,
                unit = "%",
                decimalPlaces = 0,
                accentColor = AcidPink,
                enabled = config.smartMaskingEnabled
            )
        }

        // 4. MODO 3: Limpieza de Ruido en Audio Reproducido (DSP Noise Gate & Sub-hum Filter)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(14.dp))
                .border(
                    1.dp,
                    if (config.dspNoiseGateEnabled) CyberAmber else DarkBorder,
                    RoundedCornerShape(14.dp)
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    if (config.dspNoiseGateEnabled) EmeraldActive else DarkTextSecondary,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MODO 3: LIMPIEZA DSP & ANTI-SISEO",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (config.dspNoiseGateEnabled) CyberAmber else DarkTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Filtro Sub-hum & Noise Gate",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkTextPrimary
                    )
                }

                Switch(
                    checked = config.dspNoiseGateEnabled,
                    onCheckedChange = { isEnabled ->
                        viewModel.updateNoiseControlConfig(config.copy(dspNoiseGateEnabled = isEnabled))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = CyberAmber,
                        uncheckedThumbColor = DarkTextSecondary,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }

            Text(
                text = "Utiliza el Multi-Band Compressor (MBC) y filtro paso-alto para eliminar el 'hiss' (siseo eléctrico del DAC Bluetooth) y vibraciones de masa por debajo de 40 Hz.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.sp,
                    color = DarkTextSecondary
                )
            )

            // Control Híbrido: Filtro Sub-hum (Paso-Alto)
            HybridSliderField(
                title = "Filtro Paso-Alto Sub-hum (Corte Parásito)",
                value = config.subHumCutoffHz,
                onValueChange = { newHz ->
                    viewModel.updateNoiseControlConfig(config.copy(subHumCutoffHz = newHz))
                },
                range = 20.0f..100.0f,
                unit = "Hz",
                decimalPlaces = 0,
                accentColor = CyberAmber,
                enabled = config.dspNoiseGateEnabled
            )

            // Control Híbrido: Puerta de Ruido / Noise Gate Threshold
            HybridSliderField(
                title = "Umbral de Silencio (Noise Gate)",
                value = config.noiseGateThresholdDb,
                onValueChange = { newDb ->
                    viewModel.updateNoiseControlConfig(config.copy(noiseGateThresholdDb = newDb))
                },
                range = -80.0f..-30.0f,
                unit = "dB",
                decimalPlaces = 1,
                accentColor = AcidPink,
                enabled = config.dspNoiseGateEnabled
            )

            // Control Híbrido: Atenuación de Siseo (Hiss Reduction)
            HybridSliderField(
                title = "Atenuación de Siseo (> 8kHz)",
                value = config.hissReductionDb,
                onValueChange = { newDb ->
                    viewModel.updateNoiseControlConfig(config.copy(hissReductionDb = newDb))
                },
                range = 0.0f..15.0f,
                unit = "dB",
                decimalPlaces = 1,
                accentColor = NeonCyan,
                enabled = config.dspNoiseGateEnabled
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
