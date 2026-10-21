package com.surgical.eq.htcne76.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "surgical_eq_prefs")

class EqDataStore(private val context: Context) {

    companion object {
        // Master Switches
        val KEY_MASTER_EQ_ENABLED = booleanPreferencesKey("master_eq_enabled")
        val KEY_SELECTED_PRESET_ID = stringPreferencesKey("selected_preset_id")

        // EQ Bands (Serialized JSON)
        val KEY_EQ_BANDS_JSON = stringPreferencesKey("eq_bands_json")
        val KEY_CUSTOM_PRESETS_JSON = stringPreferencesKey("custom_presets_json")

        // DSP Effects
        val KEY_BASS_BOOST = intPreferencesKey("bass_boost")
        val KEY_VIRTUALIZER = intPreferencesKey("virtualizer")
        val KEY_LOUDNESS_DB = floatPreferencesKey("loudness_db")
        val KEY_PRE_AMP_DB = floatPreferencesKey("pre_amp_db")

        // Mode 1: Phase Inversion ANC
        val KEY_ANC_PHASE_ENABLED = booleanPreferencesKey("anc_phase_enabled")
        val KEY_ANC_PHASE_DELAY_MS = floatPreferencesKey("anc_phase_delay_ms")
        val KEY_ANC_LOW_PASS_CUTOFF = floatPreferencesKey("anc_low_pass_cutoff")
        val KEY_ANC_INVERSION_GAIN = floatPreferencesKey("anc_inversion_gain")

        // Mode 2: Smart Psychoacoustic Masking
        val KEY_SMART_MASKING_ENABLED = booleanPreferencesKey("smart_masking_enabled")
        val KEY_SMART_MASKING_TYPE = stringPreferencesKey("smart_masking_type")
        val KEY_SMART_MASKING_THRESHOLD_DB = floatPreferencesKey("smart_masking_threshold_db")
        val KEY_SMART_MASKING_MAX_GAIN = floatPreferencesKey("smart_masking_max_gain")

        // Mode 3: DSP Noise Gate & Sub-hum Filter
        val KEY_DSP_NOISE_GATE_ENABLED = booleanPreferencesKey("dsp_noise_gate_enabled")
        val KEY_SUB_HUM_CUTOFF_HZ = floatPreferencesKey("sub_hum_cutoff_hz")
        val KEY_NOISE_GATE_THRESHOLD_DB = floatPreferencesKey("noise_gate_threshold_db")
        val KEY_HISS_REDUCTION_DB = floatPreferencesKey("hiss_reduction_db")
    }

    val masterEqEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_MASTER_EQ_ENABLED] ?: true
    }

    val selectedPresetIdFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SELECTED_PRESET_ID] ?: PresetRepository.PRESET_HTC_NE76
    }

    val eqBandsFlow: Flow<List<EqBand>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[KEY_EQ_BANDS_JSON]
        if (jsonStr.isNullOrEmpty()) {
            PresetRepository.getHtcNe76Preset().bands
        } else {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<EqBand>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i)
                    if (obj != null) {
                        list.add(EqBand.fromJson(obj))
                    }
                }
                if (list.isNotEmpty()) list else PresetRepository.getHtcNe76Preset().bands
            } catch (e: Exception) {
                PresetRepository.getHtcNe76Preset().bands
            }
        }
    }

    val customPresetsFlow: Flow<List<Preset>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[KEY_CUSTOM_PRESETS_JSON]
        if (jsonStr.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<Preset>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i)
                    if (obj != null) {
                        list.add(Preset.fromJson(obj))
                    }
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    val bassBoostFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_BASS_BOOST] ?: 600
    }

    val virtualizerFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_VIRTUALIZER] ?: 150
    }

    val loudnessDbFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_LOUDNESS_DB] ?: 3.5f
    }

    val preAmpDbFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_PRE_AMP_DB] ?: 0.0f
    }

    val noiseControlConfigFlow: Flow<NoiseControlConfig> = context.dataStore.data.map { preferences ->
        NoiseControlConfig(
            phaseInversionEnabled = preferences[KEY_ANC_PHASE_ENABLED] ?: false,
            phaseDelayMs = preferences[KEY_ANC_PHASE_DELAY_MS] ?: 14.5f,
            lowPassCutoffHz = preferences[KEY_ANC_LOW_PASS_CUTOFF] ?: 450.0f,
            inversionGain = preferences[KEY_ANC_INVERSION_GAIN] ?: 0.85f,

            smartMaskingEnabled = preferences[KEY_SMART_MASKING_ENABLED] ?: false,
            noiseType = preferences[KEY_SMART_MASKING_TYPE] ?: "BROWN",
            ambientThresholdDb = preferences[KEY_SMART_MASKING_THRESHOLD_DB] ?: 48.0f,
            maskingMaxGain = preferences[KEY_SMART_MASKING_MAX_GAIN] ?: 0.35f,

            dspNoiseGateEnabled = preferences[KEY_DSP_NOISE_GATE_ENABLED] ?: false,
            subHumCutoffHz = preferences[KEY_SUB_HUM_CUTOFF_HZ] ?: 40.0f,
            noiseGateThresholdDb = preferences[KEY_NOISE_GATE_THRESHOLD_DB] ?: -55.0f,
            hissReductionDb = preferences[KEY_HISS_REDUCTION_DB] ?: 6.0f
        )
    }

    suspend fun setMasterEqEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MASTER_EQ_ENABLED] = enabled
        }
    }

    suspend fun setSelectedPresetId(presetId: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_PRESET_ID] = presetId
        }
    }

    suspend fun saveEqBands(bands: List<EqBand>) {
        val array = JSONArray()
        bands.forEach { array.put(it.toJson()) }
        context.dataStore.edit { preferences ->
            preferences[KEY_EQ_BANDS_JSON] = array.toString()
        }
    }

    suspend fun saveCustomPreset(newPreset: Preset) {
        context.dataStore.edit { preferences ->
            val jsonStr = preferences[KEY_CUSTOM_PRESETS_JSON]
            val currentList = mutableListOf<Preset>()
            if (!jsonStr.isNullOrEmpty()) {
                try {
                    val array = JSONArray(jsonStr)
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i)
                        if (obj != null) currentList.add(Preset.fromJson(obj))
                    }
                } catch (_: Exception) {}
            }
            // Reemplazar si ya existe o agregar nuevo
            currentList.removeAll { it.id == newPreset.id }
            currentList.add(newPreset)

            val array = JSONArray()
            currentList.forEach { array.put(it.toJson()) }
            preferences[KEY_CUSTOM_PRESETS_JSON] = array.toString()
            preferences[KEY_SELECTED_PRESET_ID] = newPreset.id
        }
    }

    suspend fun deleteCustomPreset(presetId: String) {
        context.dataStore.edit { preferences ->
            val jsonStr = preferences[KEY_CUSTOM_PRESETS_JSON]
            if (!jsonStr.isNullOrEmpty()) {
                val currentList = mutableListOf<Preset>()
                try {
                    val array = JSONArray(jsonStr)
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i)
                        if (obj != null) currentList.add(Preset.fromJson(obj))
                    }
                } catch (_: Exception) {}
                currentList.removeAll { it.id == presetId }
                val array = JSONArray()
                currentList.forEach { array.put(it.toJson()) }
                preferences[KEY_CUSTOM_PRESETS_JSON] = array.toString()
            }
            if (preferences[KEY_SELECTED_PRESET_ID] == presetId) {
                preferences[KEY_SELECTED_PRESET_ID] = PresetRepository.PRESET_HTC_NE76
            }
        }
    }

    suspend fun setBassBoost(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BASS_BOOST] = value.coerceIn(0, 1000)
        }
    }

    suspend fun setVirtualizer(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VIRTUALIZER] = value.coerceIn(0, 1000)
        }
    }

    suspend fun setLoudnessDb(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LOUDNESS_DB] = value.coerceIn(0.0f, 15.0f)
        }
    }

    suspend fun setPreAmpDb(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PRE_AMP_DB] = value.coerceIn(-15.0f, 15.0f)
        }
    }

    suspend fun updateNoiseControlConfig(config: NoiseControlConfig) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ANC_PHASE_ENABLED] = config.phaseInversionEnabled
            preferences[KEY_ANC_PHASE_DELAY_MS] = config.phaseDelayMs
            preferences[KEY_ANC_LOW_PASS_CUTOFF] = config.lowPassCutoffHz
            preferences[KEY_ANC_INVERSION_GAIN] = config.inversionGain

            preferences[KEY_SMART_MASKING_ENABLED] = config.smartMaskingEnabled
            preferences[KEY_SMART_MASKING_TYPE] = config.noiseType
            preferences[KEY_SMART_MASKING_THRESHOLD_DB] = config.ambientThresholdDb
            preferences[KEY_SMART_MASKING_MAX_GAIN] = config.maskingMaxGain

            preferences[KEY_DSP_NOISE_GATE_ENABLED] = config.dspNoiseGateEnabled
            preferences[KEY_SUB_HUM_CUTOFF_HZ] = config.subHumCutoffHz
            preferences[KEY_NOISE_GATE_THRESHOLD_DB] = config.noiseGateThresholdDb
            preferences[KEY_HISS_REDUCTION_DB] = config.hissReductionDb
        }
    }
}
