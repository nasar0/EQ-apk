package com.surgical.eq.htcne76.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Representa una banda individual de ecualización quirúrgica.
 */
data class EqBand(
    val index: Int,
    val frequencyHz: Float,
    val gainDb: Float,
    val qFactor: Float = 1.41f,
    val isEnabled: Boolean = true
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("index", index)
            put("frequencyHz", frequencyHz.toDouble())
            put("gainDb", gainDb.toDouble())
            put("qFactor", qFactor.toDouble())
            put("isEnabled", isEnabled)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): EqBand {
            return EqBand(
                index = json.optInt("index", 0),
                frequencyHz = json.optDouble("frequencyHz", 1000.0).toFloat(),
                gainDb = json.optDouble("gainDb", 0.0).toFloat(),
                qFactor = json.optDouble("qFactor", 1.41).toFloat(),
                isEnabled = json.optBoolean("isEnabled", true)
            )
        }
    }
}

/**
 * Perfil / Preset de ecualización y procesamiento de audio.
 */
data class Preset(
    val id: String,
    val name: String,
    val description: String,
    val isCustom: Boolean = false,
    val bands: List<EqBand>,
    val bassBoost: Int,
    val loudnessDb: Float,
    val virtualizer: Int = 0,
    val preAmpDb: Float = 0.0f,
    val isHissFilterActive: Boolean = false,
    val isSleepMaskingReady: Boolean = false
) {
    fun toJson(): JSONObject {
        val bandsArray = JSONArray()
        bands.forEach { bandsArray.put(it.toJson()) }

        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("description", description)
            put("isCustom", isCustom)
            put("bands", bandsArray)
            put("bassBoost", bassBoost)
            put("loudnessDb", loudnessDb.toDouble())
            put("virtualizer", virtualizer)
            put("preAmpDb", preAmpDb.toDouble())
            put("isHissFilterActive", isHissFilterActive)
            put("isSleepMaskingReady", isSleepMaskingReady)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): Preset {
            val bandsList = mutableListOf<EqBand>()
            val bandsArray = json.optJSONArray("bands")
            if (bandsArray != null) {
                for (i in 0 until bandsArray.length()) {
                    val bandObj = bandsArray.optJSONObject(i)
                    if (bandObj != null) {
                        bandsList.add(EqBand.fromJson(bandObj))
                    }
                }
            }

            return Preset(
                id = json.optString("id", "custom_${System.currentTimeMillis()}"),
                name = json.optString("name", "Custom Preset"),
                description = json.optString("description", "Ajuste personalizado por el usuario"),
                isCustom = json.optBoolean("isCustom", true),
                bands = bandsList,
                bassBoost = json.optInt("bassBoost", 0),
                loudnessDb = json.optDouble("loudnessDb", 0.0).toFloat(),
                virtualizer = json.optInt("virtualizer", 0),
                preAmpDb = json.optDouble("preAmpDb", 0.0).toFloat(),
                isHissFilterActive = json.optBoolean("isHissFilterActive", false),
                isSleepMaskingReady = json.optBoolean("isSleepMaskingReady", false)
            )
        }
    }
}

/**
 * Configuración del módulo de cancelación y aislamiento de ruido (NoiseControlEngine)
 */
data class NoiseControlConfig(
    // Modo 1: ANC por inversión de fase
    val phaseInversionEnabled: Boolean = false,
    val phaseDelayMs: Float = 14.5f, // Milisegundos de desfase para compensar latencia Bluetooth
    val lowPassCutoffHz: Float = 450.0f, // Frecuencia de corte paso-bajo (< 600 Hz)
    val inversionGain: Float = 0.85f, // Ganancia de cancelación

    // Modo 2: Enmascaramiento psicoacústico adaptativo
    val smartMaskingEnabled: Boolean = false,
    val noiseType: String = "BROWN", // BROWN, PINK, HYBRID
    val ambientThresholdDb: Float = 48.0f, // Umbral dB para activar enmascaramiento
    val maskingMaxGain: Float = 0.35f, // Volumen máximo de la capa de ruido

    // Modo 3: Limpieza DSP de siseo y ruido eléctrico
    val dspNoiseGateEnabled: Boolean = false,
    val subHumCutoffHz: Float = 40.0f, // Filtro sub-hum (elimina 50/60Hz ruidos de masa)
    val noiseGateThresholdDb: Float = -55.0f, // Umbral para cortar el siseo en silencios
    val hissReductionDb: Float = 6.0f // Reducción en dB de altas frecuencias de hiss
)
