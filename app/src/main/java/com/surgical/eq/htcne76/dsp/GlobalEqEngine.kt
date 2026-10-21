package com.surgical.eq.htcne76.dsp

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import com.surgical.eq.htcne76.data.EqBand

/**
 * Motor maestro de ecualización quirúrgica y procesamiento dinámico.
 *
 * Utiliza DynamicsProcessing (API 28+) como motor primario de ultra-alta fidelidad
 * con resolución de frecuencia quirúrgica y etapa de Limitador anti-distorsión,
 * con fallback inteligente a Equalizer + BassBoost + Virtualizer + LoudnessEnhancer
 * sobre audioSessionId = 0 y sesiones de reproductores externos (Spotify, YouTube, etc.)
 */
class GlobalEqEngine {

    companion object {
        private const val TAG = "GlobalEqEngine"
        private const val GLOBAL_SESSION_ID = 0
    }

    private var isEnabled = true
    private var activeSessionId = GLOBAL_SESSION_ID

    // Motor Primario (API 28+)
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var isDynamicsProcessingSupported = false

    // Motores de Fallback (Legacy AudioFX)
    private var legacyEqualizer: Equalizer? = null
    private var legacyBassBoost: BassBoost? = null
    private var legacyVirtualizer: Virtualizer? = null
    private var legacyLoudnessEnhancer: LoudnessEnhancer? = null

    // Estado en memoria para sincronización
    private var currentBands: List<EqBand> = emptyList()
    private var currentBassBoost: Int = 0
    private var currentVirtualizer: Int = 0
    private var currentLoudnessDb: Float = 0.0f
    private var currentPreAmpDb: Float = 0.0f
    private var isHissFilterActive: Boolean = false

    // Registro de sesiones secundarias abiertas por reproductores
    private val secondarySessions = mutableSetOf<Int>()
    private val secondaryDynamics = mutableMapOf<Int, DynamicsProcessing>()

    /**
     * Inicializa el motor de audio en la sesión global (0)
     */
    fun init(sessionId: Int = GLOBAL_SESSION_ID) {
        activeSessionId = sessionId
        release()
        tryInitDynamicsProcessing(sessionId)
        if (!isDynamicsProcessingSupported) {
            Log.w(TAG, "DynamicsProcessing no soportado o bloqueado en sesión $sessionId. Activando motor fallback.")
            initLegacyAudioFx(sessionId)
        }
    }

    private fun tryInitDynamicsProcessing(sessionId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val bandCount = if (currentBands.isNotEmpty()) currentBands.size else 6
                val configBuilder = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2, // Estéreo
                    true, // PreEQ activo
                    bandCount,
                    true, // MBC activo (Multi-band compressor para anti-hiss)
                    4, // 4 bandas MBC
                    false, // PostEq desactivado para ahorrar latencia
                    0,
                    true // Limiter activo para Bass Extreme sin distorsión
                )

                val config = configBuilder.build()
                dynamicsProcessing = DynamicsProcessing(0, sessionId, config).apply {
                    enabled = isEnabled
                }
                isDynamicsProcessingSupported = true
                Log.i(TAG, "DynamicsProcessing inicializado con éxito en audioSession $sessionId")
            } catch (e: Exception) {
                Log.e(TAG, "Fallo al inicializar DynamicsProcessing en sesión $sessionId: ${e.message}")
                dynamicsProcessing = null
                isDynamicsProcessingSupported = false
            }
        } else {
            isDynamicsProcessingSupported = false
        }
    }

    private fun initLegacyAudioFx(sessionId: Int) {
        try {
            legacyEqualizer = Equalizer(0, sessionId).apply {
                enabled = isEnabled
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando Equalizer legacy: ${e.message}")
        }

        try {
            legacyBassBoost = BassBoost(0, sessionId).apply {
                enabled = isEnabled
                if (strengthSupported) {
                    setStrength(currentBassBoost.toShort())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando BassBoost legacy: ${e.message}")
        }

        try {
            legacyVirtualizer = Virtualizer(0, sessionId).apply {
                enabled = isEnabled
                if (strengthSupported) {
                    setStrength(currentVirtualizer.toShort())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando Virtualizer legacy: ${e.message}")
        }

        try {
            legacyLoudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                enabled = isEnabled
                setTargetGain((currentLoudnessDb * 100).toInt())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando LoudnessEnhancer legacy: ${e.message}")
        }
    }

    /**
     * Aplica el estado maestro de habilitación
     */
    fun setEnabled(enabled: Boolean) {
        this.isEnabled = enabled
        try {
            dynamicsProcessing?.enabled = enabled
        } catch (e: Exception) {
            Log.e(TAG, "Error cambiando estado de DynamicsProcessing: ${e.message}")
        }

        try {
            legacyEqualizer?.enabled = enabled
            legacyBassBoost?.enabled = enabled
            legacyVirtualizer?.enabled = enabled
            legacyLoudnessEnhancer?.enabled = enabled
        } catch (e: Exception) {
            Log.e(TAG, "Error cambiando estado de legacy AudioFX: ${e.message}")
        }

        secondaryDynamics.values.forEach { dp ->
            try {
                dp.enabled = enabled
            } catch (_: Exception) {}
        }
    }

    /**
     * Aplica la lista de bandas quirúrgicas
     */
    fun applyBands(bands: List<EqBand>) {
        this.currentBands = bands

        // 1. Aplicar en DynamicsProcessing primario a todos los canales
        dynamicsProcessing?.let { dp ->
            try {
                bands.forEachIndexed { index, band ->
                    val eqBand = DynamicsProcessing.EqBand(
                        band.isEnabled,
                        band.frequencyHz,
                        band.gainDb
                    )
                    dp.setPreEqBandAllChannelsTo(index, eqBand)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error configurando bandas en DynamicsProcessing: ${e.message}")
            }
        }

        // 2. Aplicar en sesiones secundarias
        secondaryDynamics.values.forEach { dp ->
            try {
                bands.forEachIndexed { index, band ->
                    val eqBand = DynamicsProcessing.EqBand(
                        band.isEnabled,
                        band.frequencyHz,
                        band.gainDb
                    )
                    dp.setPreEqBandAllChannelsTo(index, eqBand)
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback en legacy Equalizer si DynamicsProcessing no está disponible
        legacyEqualizer?.let { eq ->
            try {
                val numBands = eq.numberOfBands.toInt()
                val minLevel = eq.bandLevelRange[0]
                val maxLevel = eq.bandLevelRange[1]

                for (i in 0 until numBands) {
                    val centerFreqHz = eq.getCenterFreq(i.toShort()) / 1000f // mHz a Hz
                    // Encontrar la banda quirúrgica más cercana
                    val closestBand = bands.minByOrNull { kotlin.math.abs(it.frequencyHz - centerFreqHz) }
                    val targetGainMb = ((closestBand?.gainDb ?: 0f) * 100).toInt() // dB a mB
                    val clampedMb = targetGainMb.coerceIn(minLevel.toInt(), maxLevel.toInt())
                    eq.setBandLevel(i.toShort(), clampedMb.toShort())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error aplicando bandas en legacy Equalizer: ${e.message}")
            }
        }
    }

    /**
     * Aplica BassBoost (0 - 1000)
     */
    fun applyBassBoost(strength: Int) {
        this.currentBassBoost = strength.coerceIn(0, 1000)
        try {
            legacyBassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength(currentBassBoost.toShort())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error aplicando BassBoost: ${e.message}")
        }
    }

    /**
     * Aplica Virtualizer / Espacializador (0 - 1000)
     */
    fun applyVirtualizer(strength: Int) {
        this.currentVirtualizer = strength.coerceIn(0, 1000)
        try {
            legacyVirtualizer?.let {
                if (it.strengthSupported) {
                    it.setStrength(currentVirtualizer.toShort())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error aplicando Virtualizer: ${e.message}")
        }
    }

    /**
     * Aplica Ganancia Loudness (dB)
     */
    fun applyLoudnessDb(gainDb: Float) {
        this.currentLoudnessDb = gainDb.coerceIn(0.0f, 15.0f)
        try {
            legacyLoudnessEnhancer?.setTargetGain((currentLoudnessDb * 100).toInt())
        } catch (e: Exception) {
            Log.e(TAG, "Error aplicando Loudness: ${e.message}")
        }
    }

    /**
     * Aplica Ganancia Pre-Amp (dB) y Limitador de seguridad
     */
    fun applyPreAmpDb(preAmpDb: Float) {
        this.currentPreAmpDb = preAmpDb.coerceIn(-15.0f, 15.0f)
        dynamicsProcessing?.let { dp ->
            try {
                dp.setInputGainAllChannelsTo(currentPreAmpDb)

                // Calibración quirúrgica del Limiter: si preAmp es positivo o bassBoost es alto,
                // aseguramos que el limitador evite clipping digital (cero distorsión)
                val limiter = DynamicsProcessing.Limiter(
                    true, // InUse
                    true, // Enabled
                    0, // LinkGroup
                    1.0f, // AttackTime (1 ms ultra rápido)
                    50.0f, // ReleaseTime (50 ms)
                    10.0f, // Ratio 10:1 (limiter firme)
                    -0.5f, // Threshold -0.5 dB (protección de picos inter-sample)
                    0.0f // PostGain 0 dB
                )
                dp.setLimiterAllChannelsTo(limiter)
            } catch (e: Exception) {
                Log.e(TAG, "Error aplicando PreAmp/Limiter en DynamicsProcessing: ${e.message}")
            }
        }
    }

    /**
     * Configuración del filtro Anti-Hiss mediante MBC (Multi-Band Compressor) de DynamicsProcessing
     */
    fun setHissFilterActive(active: Boolean) {
        this.isHissFilterActive = active
        dynamicsProcessing?.let { dp ->
            try {
                // Banda 3 de MBC configurada para altas frecuencias (> 8 kHz)
                val mbcBand = DynamicsProcessing.MbcBand(
                    active, // Enabled
                    8000.0f, // CutoffFrequency
                    1.0f, // AttackTime
                    80.0f, // ReleaseTime
                    4.0f, // Ratio
                    if (active) -45.0f else -10.0f, // Threshold
                    0.0f, // KneeWidth
                    -2.0f, // NoiseGateThreshold
                    0.0f, // ExpanderRatio
                    0.0f, // PreGain
                    if (active) -3.0f else 0.0f // PostGain
                )
                dp.setMbcBandAllChannelsTo(3, mbcBand)
            } catch (e: Exception) {
                Log.e(TAG, "Error configurando Anti-Hiss MBC: ${e.message}")
            }
        }
    }

    /**
     * Vincula una sesión de audio de una app de música (Spotify, YouTube, etc.)
     */
    fun onSessionOpened(sessionId: Int) {
        if (sessionId == GLOBAL_SESSION_ID || sessionId <= 0) return
        secondarySessions.add(sessionId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val bandCount = if (currentBands.isNotEmpty()) currentBands.size else 6
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2, true, bandCount, true, 4, false, 0, true
                ).build()
                val dp = DynamicsProcessing(0, sessionId, config).apply {
                    enabled = isEnabled
                }
                secondaryDynamics[sessionId] = dp
                // Aplicar estado actual
                applyBands(currentBands)
                applyPreAmpDb(currentPreAmpDb)
                Log.i(TAG, "Sesión de reproductor $sessionId vinculada con éxito")
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo vincular DynamicsProcessing en sesión $sessionId: ${e.message}")
            }
        }
    }

    /**
     * Desvincula una sesión de audio
     */
    fun onSessionClosed(sessionId: Int) {
        secondarySessions.remove(sessionId)
        secondaryDynamics.remove(sessionId)?.let { dp ->
            try {
                dp.enabled = false
                dp.release()
            } catch (_: Exception) {}
        }
    }

    fun release() {
        try {
            dynamicsProcessing?.enabled = false
            dynamicsProcessing?.release()
        } catch (_: Exception) {}
        dynamicsProcessing = null

        secondaryDynamics.values.forEach { dp ->
            try {
                dp.enabled = false
                dp.release()
            } catch (_: Exception) {}
        }
        secondaryDynamics.clear()
        secondarySessions.clear()

        try {
            legacyEqualizer?.enabled = false
            legacyEqualizer?.release()
        } catch (_: Exception) {}
        legacyEqualizer = null

        try {
            legacyBassBoost?.enabled = false
            legacyBassBoost?.release()
        } catch (_: Exception) {}
        legacyBassBoost = null

        try {
            legacyVirtualizer?.enabled = false
            legacyVirtualizer?.release()
        } catch (_: Exception) {}
        legacyVirtualizer = null

        try {
            legacyLoudnessEnhancer?.enabled = false
            legacyLoudnessEnhancer?.release()
        } catch (_: Exception) {}
        legacyLoudnessEnhancer = null
    }
}
