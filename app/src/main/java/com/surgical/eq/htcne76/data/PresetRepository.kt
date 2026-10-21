package com.surgical.eq.htcne76.data

object PresetRepository {

    const val PRESET_HTC_NE76 = "htc_ne76_pro"
    const val PRESET_ISOLATION_SLEEP = "isolation_sleep"
    const val PRESET_BASS_EXTREME = "bass_extreme"
    const val PRESET_SURGICAL_FLAT = "surgical_flat"

    /**
     * Preset insignia optimizado para la respuesta de frecuencia acústica de los HTC NE76:
     * - 60Hz: +5.5dB (Refuerzo de subgraves profundos)
     * - 150Hz: +3.5dB (Cuerpo y pegada de bombo)
     * - 400Hz: -1.5dB (Limpieza quirúrgica: elimina la resonancia encajonada de las almohadillas)
     * - 1000Hz: +1.0dB (Presencia vocal natural)
     * - 3500Hz: +3.0dB (Apertura de armónicos y detalle de cuerdas/viento)
     * - 12000Hz: +4.0dB (Extensión de aire y brillo sin estridencia)
     * - BassBoost: 600
     * - Loudness: +3.5dB
     */
    fun getHtcNe76Preset(): Preset {
        return Preset(
            id = PRESET_HTC_NE76,
            name = "HTC NE76 Pro Tuned",
            description = "Calibración quirúrgica para HTC NE76: graves limpios, sin efecto encajonado y agudos brillantes.",
            isCustom = false,
            bands = listOf(
                EqBand(index = 0, frequencyHz = 60.0f, gainDb = 5.5f, qFactor = 1.41f),
                EqBand(index = 1, frequencyHz = 150.0f, gainDb = 3.5f, qFactor = 1.41f),
                EqBand(index = 2, frequencyHz = 400.0f, gainDb = -1.5f, qFactor = 1.80f),
                EqBand(index = 3, frequencyHz = 1000.0f, gainDb = 1.0f, qFactor = 1.41f),
                EqBand(index = 4, frequencyHz = 3500.0f, gainDb = 3.0f, qFactor = 1.41f),
                EqBand(index = 5, frequencyHz = 12000.0f, gainDb = 4.0f, qFactor = 1.20f)
            ),
            bassBoost = 600,
            loudnessDb = 3.5f,
            virtualizer = 150,
            preAmpDb = 0.0f,
            isHissFilterActive = true,
            isSleepMaskingReady = false
        )
    }

    /**
     * Preset para descanso, sueño y máximo aislamiento:
     * - Atenúa frecuencias fatigosas y siseo
     * - Enfatiza frecuencias suaves y cálidas
     */
    fun getIsolationSleepPreset(): Preset {
        return Preset(
            id = PRESET_ISOLATION_SLEEP,
            name = "Aislamiento Total + Dormir",
            description = "Filtro anti-siseo activo, corte suave de agudos cortantes y base cálida para dormir.",
            isCustom = false,
            bands = listOf(
                EqBand(index = 0, frequencyHz = 60.0f, gainDb = 4.0f, qFactor = 1.20f),
                EqBand(index = 1, frequencyHz = 150.0f, gainDb = 2.0f, qFactor = 1.20f),
                EqBand(index = 2, frequencyHz = 400.0f, gainDb = -2.0f, qFactor = 1.41f),
                EqBand(index = 3, frequencyHz = 1000.0f, gainDb = -0.5f, qFactor = 1.41f),
                EqBand(index = 4, frequencyHz = 3500.0f, gainDb = -3.5f, qFactor = 1.41f),
                EqBand(index = 5, frequencyHz = 12000.0f, gainDb = -6.0f, qFactor = 1.00f)
            ),
            bassBoost = 250,
            loudnessDb = 1.0f,
            virtualizer = 0,
            preAmpDb = -1.0f,
            isHissFilterActive = true,
            isSleepMaskingReady = true
        )
    }

    /**
     * Preset Bass Extreme:
     * - Subgraves al máximo con protección de limitador para evitar distorsión armónica o clipping
     */
    fun getBassExtremePreset(): Preset {
        return Preset(
            id = PRESET_BASS_EXTREME,
            name = "Bass Extreme (Editable)",
            description = "Subgraves atronadores controlados por DynamicsProcessing Limiter para cero distorsión.",
            isCustom = false,
            bands = listOf(
                EqBand(index = 0, frequencyHz = 60.0f, gainDb = 9.5f, qFactor = 1.20f),
                EqBand(index = 1, frequencyHz = 150.0f, gainDb = 6.0f, qFactor = 1.41f),
                EqBand(index = 2, frequencyHz = 400.0f, gainDb = 0.0f, qFactor = 1.41f),
                EqBand(index = 3, frequencyHz = 1000.0f, gainDb = 0.0f, qFactor = 1.41f),
                EqBand(index = 4, frequencyHz = 3500.0f, gainDb = 1.5f, qFactor = 1.41f),
                EqBand(index = 5, frequencyHz = 12000.0f, gainDb = 2.0f, qFactor = 1.20f)
            ),
            bassBoost = 850,
            loudnessDb = 4.0f,
            virtualizer = 200,
            preAmpDb = -2.0f, // Headroom para que el limitador opere limpiamente
            isHissFilterActive = false,
            isSleepMaskingReady = false
        )
    }

    /**
     * Preset Plano Quirúrgico / Referencia
     */
    fun getSurgicalFlatPreset(): Preset {
        return Preset(
            id = PRESET_SURGICAL_FLAT,
            name = "Surgical Flat (Referencia)",
            description = "Respuesta totalmente lineal a 0 dB para monitorización precisa.",
            isCustom = false,
            bands = listOf(
                EqBand(index = 0, frequencyHz = 60.0f, gainDb = 0.0f, qFactor = 1.41f),
                EqBand(index = 1, frequencyHz = 150.0f, gainDb = 0.0f, qFactor = 1.41f),
                EqBand(index = 2, frequencyHz = 400.0f, gainDb = 0.0f, qFactor = 1.41f),
                EqBand(index = 3, frequencyHz = 1000.0f, gainDb = 0.0f, qFactor = 1.41f),
                EqBand(index = 4, frequencyHz = 3500.0f, gainDb = 0.0f, qFactor = 1.41f),
                EqBand(index = 5, frequencyHz = 12000.0f, gainDb = 0.0f, qFactor = 1.41f)
            ),
            bassBoost = 0,
            loudnessDb = 0.0f,
            virtualizer = 0,
            preAmpDb = 0.0f,
            isHissFilterActive = false,
            isSleepMaskingReady = false
        )
    }

    fun getDefaultPresets(): List<Preset> {
        return listOf(
            getHtcNe76Preset(),
            getIsolationSleepPreset(),
            getBassExtremePreset(),
            getSurgicalFlatPreset()
        )
    }
}
