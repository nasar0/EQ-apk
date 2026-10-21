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
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!hasMicPermission) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1400), RoundedCornerShape(8.dp))
                    .border(0.7.dp, CyberAmber, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "PERMISO DE MICRÓFONO REQUERIDO",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberAmber
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Necesario para capturar audio exterior y calcular la onda anti-ruido invertida.",
                    fontSize = 9.sp,
                    color = DarkTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberAmber,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("Conceder Permiso", fontSize = 9.sp)
                }
            }
        }

        // 1. Sonómetro Compacto
        DecibelMeterView(
            ambientDb = uiState.ambientDb,
            isMicActive = hasMicPermission && (config.phaseInversionEnabled || config.smartMaskingEnabled)
        )

        // 2. MODO 1: ANC por Inversión de Fase
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(8.dp))
                .border(
                    0.7.dp,
                    if (config.phaseInversionEnabled) NeonCyan else DarkBorder,
                    RoundedCornerShape(8.dp)
                )
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(5.dp)
                            .background(
                                if (config.phaseInversionEnabled) EmeraldActive else DarkTextSecondary,
                                RoundedCornerShape(2.5.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "MODO 1: ANC INVERSIÓN DE FASE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (config.phaseInversionEnabled) NeonCyan else DarkTextSecondary
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
                    modifier = Modifier.height(22.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = DarkTextSecondary,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }

            Text(
                text = "Invierte la onda PCM exterior (< 600 Hz). Calibra los milisegundos de desfase para sincronizar con la latencia Bluetooth de tus HTC NE76.",
                fontSize = 8.5.sp,
                color = DarkTextSecondary
            )

            val samplesCount = ((config.phaseDelayMs * 48000f) / 1000f).roundToInt()
            HybridSliderField(
                title = "Compensación de Fase ($samplesCount muestras)",
                value = config.phaseDelayMs,
                onValueChange = { viewModel.updateNoiseControlConfig(config.copy(phaseDelayMs = it)) },
                range = 0.0f..50.0f,
                unit = "ms",
                decimalPlaces = 1,
                accentColor = NeonCyan,
                enabled = config.phaseInversionEnabled
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HybridSliderField(
                    title = "Corte ANC (<600Hz)",
                    value = config.lowPassCutoffHz,
                    onValueChange = { viewModel.updateNoiseControlConfig(config.copy(lowPassCutoffHz = it)) },
                    range = 50.0f..600.0f,
                    unit = "Hz",
                    decimalPlaces = 0,
                    accentColor = ElectricBlue,
                    enabled = config.phaseInversionEnabled,
                    modifier = Modifier.weight(1f)
                )

                HybridSliderField(
                    title = "Ganancia Anti-Ruido",
                    value = config.inversionGain,
                    onValueChange = { viewModel.updateNoiseControlConfig(config.copy(inversionGain = it)) },
                    range = 0.0f..1.50f,
                    unit = "x",
                    decimalPlaces = 2,
                    accentColor = CyberAmber,
                    enabled = config.phaseInversionEnabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. MODO 2: Enmascaramiento Psicoacústico Inteligente
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(8.dp))
                .border(
                    0.7.dp,
                    if (config.smartMaskingEnabled) ElectricBlue else DarkBorder,
                    RoundedCornerShape(8.dp)
                )
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(5.dp)
                            .background(
                                if (config.smartMaskingEnabled) EmeraldActive else DarkTextSecondary,
                                RoundedCornerShape(2.5.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "MODO 2: ENMASCARAMIENTO PSICOACÚSTICO",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (config.smartMaskingEnabled) ElectricBlue else DarkTextSecondary
                    )
                }

                Switch(
                    checked = config.smartMaskingEnabled,
                    onCheckedChange = { isEnabled ->
                        viewModel.updateNoiseControlConfig(config.copy(smartMaskingEnabled = isEnabled))
                    },
                    modifier = Modifier.height(22.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = ElectricBlue,
                        uncheckedThumbColor = DarkTextSecondary,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }

            // Selector compacto de Ruido
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple("BROWN", "Marrón", "Graves/Ronquidos"),
                    Triple("PINK", "Rosa", "Voces/Casa"),
                    Triple("HYBRID", "Híbrido", "Balance")
                ).forEach { (type, label, desc) ->
                    val isSelected = config.noiseType == type

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) NeonCyanDim else DarkSurfaceVariant,
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                0.7.dp,
                                if (isSelected) ElectricBlue else DarkBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { viewModel.updateNoiseControlConfig(config.copy(noiseType = type)) }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            color = if (isSelected) ElectricBlue else DarkTextPrimary
                        )
                        Text(
                            text = desc,
                            fontSize = 7.5.sp,
                            color = DarkTextSecondary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HybridSliderField(
                    title = "Umbral Activación",
                    value = config.ambientThresholdDb,
                    onValueChange = { viewModel.updateNoiseControlConfig(config.copy(ambientThresholdDb = it)) },
                    range = 30.0f..80.0f,
                    unit = "dB",
                    decimalPlaces = 1,
                    accentColor = ElectricBlue,
                    enabled = config.smartMaskingEnabled,
                    modifier = Modifier.weight(1f)
                )

                HybridSliderField(
                    title = "Volumen Máximo",
                    value = config.maskingMaxGain * 100f,
                    onValueChange = { viewModel.updateNoiseControlConfig(config.copy(maskingMaxGain = it / 100f)) },
                    range = 5.0f..100.0f,
                    unit = "%",
                    decimalPlaces = 0,
                    accentColor = AcidPink,
                    enabled = config.smartMaskingEnabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. MODO 3: Limpieza DSP & Anti-Hiss
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(8.dp))
                .border(
                    0.7.dp,
                    if (config.dspNoiseGateEnabled) CyberAmber else DarkBorder,
                    RoundedCornerShape(8.dp)
                )
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(5.dp)
                            .background(
                                if (config.dspNoiseGateEnabled) EmeraldActive else DarkTextSecondary,
                                RoundedCornerShape(2.5.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "MODO 3: LIMPIEZA DSP & ANTI-SISEO",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (config.dspNoiseGateEnabled) CyberAmber else DarkTextSecondary
                    )
                }

                Switch(
                    checked = config.dspNoiseGateEnabled,
                    onCheckedChange = { isEnabled ->
                        viewModel.updateNoiseControlConfig(config.copy(dspNoiseGateEnabled = isEnabled))
                    },
                    modifier = Modifier.height(22.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = CyberAmber,
                        uncheckedThumbColor = DarkTextSecondary,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HybridSliderField(
                    title = "Corte Sub-hum (<40Hz)",
                    value = config.subHumCutoffHz,
                    onValueChange = { viewModel.updateNoiseControlConfig(config.copy(subHumCutoffHz = it)) },
                    range = 20.0f..100.0f,
                    unit = "Hz",
                    decimalPlaces = 0,
                    accentColor = CyberAmber,
                    enabled = config.dspNoiseGateEnabled,
                    modifier = Modifier.weight(1f)
                )

                HybridSliderField(
                    title = "Atenuación Hiss (>8kHz)",
                    value = config.hissReductionDb,
                    onValueChange = { viewModel.updateNoiseControlConfig(config.copy(hissReductionDb = it)) },
                    range = 0.0f..15.0f,
                    unit = "dB",
                    decimalPlaces = 1,
                    accentColor = NeonCyan,
                    enabled = config.dspNoiseGateEnabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
