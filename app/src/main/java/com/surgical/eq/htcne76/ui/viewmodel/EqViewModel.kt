package com.surgical.eq.htcne76.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surgical.eq.htcne76.data.EqBand
import com.surgical.eq.htcne76.data.EqDataStore
import com.surgical.eq.htcne76.data.NoiseControlConfig
import com.surgical.eq.htcne76.data.Preset
import com.surgical.eq.htcne76.data.PresetRepository
import com.surgical.eq.htcne76.service.AudioEqService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EqUiState(
    val isMasterEnabled: Boolean = true,
    val bands: List<EqBand> = emptyList(),
    val bassBoost: Int = 600,
    val virtualizer: Int = 150,
    val loudnessDb: Float = 3.5f,
    val preAmpDb: Float = 0.0f,
    val selectedPresetId: String = PresetRepository.PRESET_HTC_NE76,
    val allPresets: List<Preset> = emptyList(),
    val noiseControlConfig: NoiseControlConfig = NoiseControlConfig(),
    val ambientDb: Float = 32.0f,
    val isMicActive: Boolean = false
)

class EqViewModel(
    private val eqDataStore: EqDataStore
) : ViewModel() {

    private val _ambientDb = MutableStateFlow(32.0f)
    val ambientDb: StateFlow<Float> = _ambientDb.asStateFlow()

    init {
        // Observar sonómetro de decibelios del motor de ruido en segundo plano
        viewModelScope.launch {
            while (true) {
                AudioEqService.serviceInstance?.noiseControlEngine?.ambientDbFlow?.collect { db ->
                    _ambientDb.value = db
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    val uiState: StateFlow<EqUiState> = combine(
        eqDataStore.masterEqEnabledFlow,
        eqDataStore.eqBandsFlow,
        eqDataStore.bassBoostFlow,
        eqDataStore.virtualizerFlow,
        eqDataStore.loudnessDbFlow,
        eqDataStore.preAmpDbFlow,
        eqDataStore.selectedPresetIdFlow,
        eqDataStore.customPresetsFlow,
        eqDataStore.noiseControlConfigFlow,
        ambientDb
    ) { values ->
        val masterEnabled = values[0] as Boolean
        @Suppress("UNCHECKED_CAST")
        val bands = values[1] as List<EqBand>
        val bassBoost = values[2] as Int
        val virtualizer = values[3] as Int
        val loudnessDb = values[4] as Float
        val preAmpDb = values[5] as Float
        val selectedPresetId = values[6] as String
        @Suppress("UNCHECKED_CAST")
        val customPresets = values[7] as List<Preset>
        val noiseConfig = values[8] as NoiseControlConfig
        val currentDb = values[9] as Float

        val allPresets = PresetRepository.getDefaultPresets() + customPresets

        EqUiState(
            isMasterEnabled = masterEnabled,
            bands = bands,
            bassBoost = bassBoost,
            virtualizer = virtualizer,
            loudnessDb = loudnessDb,
            preAmpDb = preAmpDb,
            selectedPresetId = selectedPresetId,
            allPresets = allPresets,
            noiseControlConfig = noiseConfig,
            ambientDb = currentDb,
            isMicActive = noiseConfig.phaseInversionEnabled || noiseConfig.smartMaskingEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EqUiState(
            bands = PresetRepository.getHtcNe76Preset().bands,
            allPresets = PresetRepository.getDefaultPresets()
        )
    )

    fun setMasterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            eqDataStore.setMasterEqEnabled(enabled)
        }
    }

    fun updateBand(index: Int, freqHz: Float, gainDb: Float, qFactor: Float) {
        val currentBands = uiState.value.bands.toMutableList()
        val existingIndex = currentBands.indexOfFirst { it.index == index }
        if (existingIndex != -1) {
            val updated = currentBands[existingIndex].copy(
                frequencyHz = freqHz,
                gainDb = gainDb,
                qFactor = qFactor
            )
            currentBands[existingIndex] = updated
            viewModelScope.launch {
                eqDataStore.saveEqBands(currentBands)
            }
        }
    }

    fun toggleBandEnabled(index: Int) {
        val currentBands = uiState.value.bands.toMutableList()
        val existingIndex = currentBands.indexOfFirst { it.index == index }
        if (existingIndex != -1) {
            val band = currentBands[existingIndex]
            currentBands[existingIndex] = band.copy(isEnabled = !band.isEnabled)
            viewModelScope.launch {
                eqDataStore.saveEqBands(currentBands)
            }
        }
    }

    fun setBassBoost(strength: Int) {
        viewModelScope.launch {
            eqDataStore.setBassBoost(strength)
        }
    }

    fun setVirtualizer(strength: Int) {
        viewModelScope.launch {
            eqDataStore.setVirtualizer(strength)
        }
    }

    fun setLoudnessDb(db: Float) {
        viewModelScope.launch {
            eqDataStore.setLoudnessDb(db)
        }
    }

    fun setPreAmpDb(db: Float) {
        viewModelScope.launch {
            eqDataStore.setPreAmpDb(db)
        }
    }

    fun selectPreset(preset: Preset) {
        viewModelScope.launch {
            eqDataStore.setSelectedPresetId(preset.id)
            eqDataStore.saveEqBands(preset.bands)
            eqDataStore.setBassBoost(preset.bassBoost)
            eqDataStore.setLoudnessDb(preset.loudnessDb)
            eqDataStore.setVirtualizer(preset.virtualizer)
            eqDataStore.setPreAmpDb(preset.preAmpDb)

            if (preset.isHissFilterActive) {
                val currentNoise = uiState.value.noiseControlConfig
                eqDataStore.updateNoiseControlConfig(currentNoise.copy(dspNoiseGateEnabled = true))
            }
        }
    }

    fun saveCustomPreset(name: String) {
        viewModelScope.launch {
            val current = uiState.value
            val newPreset = Preset(
                id = "custom_${System.currentTimeMillis()}",
                name = name,
                description = "Preset personalizado del usuario",
                isCustom = true,
                bands = current.bands,
                bassBoost = current.bassBoost,
                loudnessDb = current.loudnessDb,
                virtualizer = current.virtualizer,
                preAmpDb = current.preAmpDb
            )
            eqDataStore.saveCustomPreset(newPreset)
        }
    }

    fun deleteCustomPreset(id: String) {
        viewModelScope.launch {
            eqDataStore.deleteCustomPreset(id)
        }
    }

    fun updateNoiseControlConfig(config: NoiseControlConfig) {
        viewModelScope.launch {
            eqDataStore.updateNoiseControlConfig(config)
        }
    }
}
