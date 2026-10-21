package com.surgical.eq.htcne76.dsp

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import com.surgical.eq.htcne76.data.EqBand

/**
 * Motor maestro de ecualización quirúrgica de alta compatibilidad.
 *
 * Utiliza prioridad máxima (1000) y modo híbrido simultáneo:
 * 1. DynamicsProcessing (API 28+) para resolución quirúrgica y limiter anti-clipping.
 * 2. Legacy AudioFX (Equalizer + BassBoost + Virtualizer + LoudnessEnhancer)
 *    para garantizar que en ROMs de Samsung, Xiaomi, Motorola, etc. donde la sesión 0
 *    de DynamicsProcessing pueda ser ignorada por el HAL, el sonido sea 100% audible y potente.
 * 3. Ancla de audio en silencio (Silent Audio Anchor) para forzar a AudioFlinger a mantener
 *    activa la cadena de efectos global 24/7.
 */
class GlobalEqEngine {

    companion object {
        private const val TAG = "GlobalEqEngine"
        private const val GLOBAL_SESSION_ID = 0
        private const val EFFECT_PRIORITY = 1000 // Prioridad alta para superar efectos del sistema
    }

    private var isEnabled = true
    private var activeSessionId = GLOBAL_SESSION_ID

    // Motor Primario (API 28+)
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var isDynamicsProcessingActive = false

    // Motores Universales (AudioFX)
    private var legacyEqualizer: Equalizer? = null
    private var legacyBassBoost: BassBoost? = null
    private var legacyVirtualizer: Virtualizer? = null
    private var legacyLoudnessEnhancer: LoudnessEnhancer? = null

    // Ancla de audio silenciosa para mantener vivo AudioFlinger
    private var silentAnchorTrack: AudioTrack? = null
    private var isAnchorRunning = false

    // Estado en memoria
    private var currentBands: List<EqBand> = emptyList()
    private var currentBassBoost: Int = 600
    private var currentVirtualizer: Int = 150
    private var currentLoudnessDb: Float = 3.5f
    private var currentPreAmpDb: Float = 0.0f
    private var isHissFilterActive: Boolean = false

    // Registro de sesiones de reproductores externos
    private val playerSessions = mutableSetOf<Int>()
    private val sessionDynamics = mutableMapOf<Int, DynamicsProcessing>()
    private val sessionEqualizers = mutableMapOf<Int, Equalizer>()
    private val sessionBassBoosts = mutableMapOf<Int, BassBoost>()
    private val sessionLoudness = mutableMapOf<Int, LoudnessEnhancer>()

    fun init(sessionId: Int = GLOBAL_SESSION_ID) {
        activeSessionId = sessionId
        release()

        // 1. Iniciar ancla de audio silenciosa para forzar a Android a activar el pipeline global
        startSilentAudioAnchor()

        // 2. Iniciar DynamicsProcessing con alta prioridad
        initDynamicsProcessing(sessionId)

        // 3. Iniciar siempre Legacy AudioFX en paralelo para máxima compatibilidad y audibilidad
        initLegacyAudioFx(sessionId)

        // 4. Aplicar configuraciones guardadas
        if (currentBands.isNotEmpty()) {
            applyBands(currentBands)
        }
        applyBassBoost(currentBassBoost)
        applyVirtualizer(currentVirtualizer)
        applyLoudnessDb(currentLoudnessDb)
        applyPreAmpDb(currentPreAmpDb)
    }

    /**
     * Inicia una pista de audio en silencio inaudible para obligar al subsistema
     * AudioFlinger de Android a procesar la sesión global de efectos continuamente.
     */
    private fun startSilentAudioAnchor() {
        if (isAnchorRunning) return
        try {
            val sampleRate = 44100
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val buffer = ShortArray(minBuf) { 0 }

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()

            silentAnchorTrack = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            silentAnchorTrack?.play()
            isAnchorRunning = true

            // Escribir silencio en un hilo de fondo de bajo consumo
            Thread({
                while (isAnchorRunning && silentAnchorTrack != null) {
                    try {
                        silentAnchorTrack?.write(buffer, 0, buffer.size)
                        Thread.sleep(100)
                    } catch (_: Exception) {
                        break
                    }
                }
            }, "SurgicalSilentAnchor").apply {
                priority = Thread.MIN_PRIORITY
                isDaemon = true
                start()
            }
            Log.i(TAG, "Silent Audio Anchor iniciado con éxito")
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo iniciar silent anchor: ${e.message}")
        }
    }

    private fun initDynamicsProcessing(sessionId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val bandCount = if (currentBands.isNotEmpty()) currentBands.size else 6
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2, true, bandCount, true, 4, false, 0, true
                ).build()

                dynamicsProcessing = DynamicsProcessing(EFFECT_PRIORITY, sessionId, config).apply {
                    enabled = isEnabled
                }
                isDynamicsProcessingActive = true
                Log.i(TAG, "DynamicsProcessing activo con prioridad $EFFECT_PRIORITY en sesión $sessionId")
            } catch (e: Exception) {
                Log.e(TAG, "DynamicsProcessing no disponible en sesión $sessionId: ${e.message}")
                dynamicsProcessing = null
                isDynamicsProcessingActive = false
            }
        }
    }

    private fun initLegacyAudioFx(sessionId: Int) {
        try {
            legacyEqualizer = Equalizer(EFFECT_PRIORITY, sessionId).apply {
                enabled = isEnabled
            }
            Log.i(TAG, "Equalizer universal activo con ${legacyEqualizer?.numberOfBands} bandas")
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando Equalizer universal: ${e.message}")
        }

        try {
            legacyBassBoost = BassBoost(EFFECT_PRIORITY, sessionId).apply {
                enabled = isEnabled
                if (strengthSupported) setStrength(currentBassBoost.toShort())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando BassBoost universal: ${e.message}")
        }

        try {
            legacyVirtualizer = Virtualizer(EFFECT_PRIORITY, sessionId).apply {
                enabled = isEnabled
                if (strengthSupported) setStrength(currentVirtualizer.toShort())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando Virtualizer universal: ${e.message}")
        }

        try {
            legacyLoudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                enabled = isEnabled
                setTargetGain((currentLoudnessDb * 100).toInt())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando LoudnessEnhancer universal: ${e.message}")
        }
    }

    fun setEnabled(enabled: Boolean) {
        this.isEnabled = enabled

        try { dynamicsProcessing?.enabled = enabled } catch (_: Exception) {}
        try { legacyEqualizer?.enabled = enabled } catch (_: Exception) {}
        try { legacyBassBoost?.enabled = enabled } catch (_: Exception) {}
        try { legacyVirtualizer?.enabled = enabled } catch (_: Exception) {}
        try { legacyLoudnessEnhancer?.enabled = enabled } catch (_: Exception) {}

        sessionDynamics.values.forEach { try { it.enabled = enabled } catch (_: Exception) {} }
        sessionEqualizers.values.forEach { try { it.enabled = enabled } catch (_: Exception) {} }
        sessionBassBoosts.values.forEach { try { it.enabled = enabled } catch (_: Exception) {} }
        sessionLoudness.values.forEach { try { it.enabled = enabled } catch (_: Exception) {} }
    }

    fun applyBands(bands: List<EqBand>) {
        this.currentBands = bands

        // 1. Aplicar en DynamicsProcessing global
        dynamicsProcessing?.let { dp ->
            try {
                bands.forEachIndexed { index, band ->
                    val eqBand = DynamicsProcessing.EqBand(
                        band.isEnabled && isEnabled,
                        band.frequencyHz,
                        if (isEnabled) band.gainDb else 0.0f
                    )
                    dp.setPreEqBandAllChannelsTo(index, eqBand)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error aplicando bandas en DynamicsProcessing: ${e.message}")
            }
        }

        // 2. Aplicar simultáneamente en Equalizer universal
        applyBandsToEqualizer(legacyEqualizer, bands)

        // 3. Aplicar en todas las sesiones externas activas
        sessionDynamics.values.forEach { dp ->
            try {
                bands.forEachIndexed { index, band ->
                    val eqBand = DynamicsProcessing.EqBand(
                        band.isEnabled && isEnabled,
                        band.frequencyHz,
                        if (isEnabled) band.gainDb else 0.0f
                    )
                    dp.setPreEqBandAllChannelsTo(index, eqBand)
                }
            } catch (_: Exception) {}
        }

        sessionEqualizers.values.forEach { eq ->
            applyBandsToEqualizer(eq, bands)
        }
    }

    private fun applyBandsToEqualizer(eq: Equalizer?, bands: List<EqBand>) {
        if (eq == null) return
        try {
            val numBands = eq.numberOfBands.toInt()
            val minMb = eq.bandLevelRange[0].toInt()
            val maxMb = eq.bandLevelRange[1].toInt()

            for (i in 0 until numBands) {
                val centerFreqHz = eq.getCenterFreq(i.toShort()) / 1000f
                val closestBand = bands.minByOrNull { kotlin.math.abs(it.frequencyHz - centerFreqHz) }
                val targetGainMb = if (isEnabled && (closestBand?.isEnabled == true)) {
                    ((closestBand.gainDb) * 100).toInt()
                } else 0

                val clamped = targetGainMb.coerceIn(minMb, maxMb)
                eq.setBandLevel(i.toShort(), clamped.toShort())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando bandas en Equalizer: ${e.message}")
        }
    }

    fun applyBassBoost(strength: Int) {
        this.currentBassBoost = strength.coerceIn(0, 1000)
        val s = (if (isEnabled) currentBassBoost else 0).toShort()
        try {
            if (legacyBassBoost?.strengthSupported == true) legacyBassBoost?.setStrength(s)
        } catch (_: Exception) {}
        sessionBassBoosts.values.forEach {
            try { if (it.strengthSupported) it.setStrength(s) } catch (_: Exception) {}
        }
    }

    fun applyVirtualizer(strength: Int) {
        this.currentVirtualizer = strength.coerceIn(0, 1000)
        val s = (if (isEnabled) currentVirtualizer else 0).toShort()
        try {
            if (legacyVirtualizer?.strengthSupported == true) legacyVirtualizer?.setStrength(s)
        } catch (_: Exception) {}
    }

    fun applyLoudnessDb(gainDb: Float) {
        this.currentLoudnessDb = gainDb.coerceIn(0.0f, 15.0f)
        val mB = (if (isEnabled) currentLoudnessDb * 100 else 0f).toInt()
        try { legacyLoudnessEnhancer?.setTargetGain(mB) } catch (_: Exception) {}
        sessionLoudness.values.forEach {
            try { it.setTargetGain(mB) } catch (_: Exception) {}
        }
    }

    fun applyPreAmpDb(preAmpDb: Float) {
        this.currentPreAmpDb = preAmpDb.coerceIn(-15.0f, 15.0f)
        val effectiveGain = if (isEnabled) currentPreAmpDb else 0.0f

        dynamicsProcessing?.let { dp ->
            try {
                dp.setInputGainAllChannelsTo(effectiveGain)
                val limiter = DynamicsProcessing.Limiter(
                    true, true, 0, 1.0f, 50.0f, 10.0f, -0.5f, 0.0f
                )
                dp.setLimiterAllChannelsTo(limiter)
            } catch (_: Exception) {}
        }
        sessionDynamics.values.forEach { dp ->
            try {
                dp.setInputGainAllChannelsTo(effectiveGain)
            } catch (_: Exception) {}
        }
    }

    fun setHissFilterActive(active: Boolean) {
        this.isHissFilterActive = active
        dynamicsProcessing?.let { dp ->
            try {
                val mbcBand = DynamicsProcessing.MbcBand(
                    active && isEnabled, 8000.0f, 1.0f, 80.0f, 4.0f,
                    if (active) -45.0f else -10.0f, 0.0f, -2.0f, 0.0f, 0.0f,
                    if (active) -3.0f else 0.0f
                )
                dp.setMbcBandAllChannelsTo(3, mbcBand)
            } catch (_: Exception) {}
        }
    }

    fun onSessionOpened(sessionId: Int) {
        if (sessionId == GLOBAL_SESSION_ID || sessionId <= 0) return
        playerSessions.add(sessionId)
        Log.i(TAG, "Vinculando nueva sesión externa: $sessionId")

        // 1. DynamicsProcessing por sesión
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val bandCount = if (currentBands.isNotEmpty()) currentBands.size else 6
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2, true, bandCount, true, 4, false, 0, true
                ).build()
                val dp = DynamicsProcessing(EFFECT_PRIORITY, sessionId, config).apply {
                    enabled = isEnabled
                }
                sessionDynamics[sessionId] = dp
            } catch (_: Exception) {}
        }

        // 2. Legacy FX por sesión para garantizar compatibilidad total
        try {
            val eq = Equalizer(EFFECT_PRIORITY, sessionId).apply { enabled = isEnabled }
            sessionEqualizers[sessionId] = eq
            applyBandsToEqualizer(eq, currentBands)
        } catch (_: Exception) {}

        try {
            val bb = BassBoost(EFFECT_PRIORITY, sessionId).apply {
                enabled = isEnabled
                if (strengthSupported) setStrength(currentBassBoost.toShort())
            }
            sessionBassBoosts[sessionId] = bb
        } catch (_: Exception) {}

        try {
            val le = LoudnessEnhancer(sessionId).apply {
                enabled = isEnabled
                setTargetGain((currentLoudnessDb * 100).toInt())
            }
            sessionLoudness[sessionId] = le
        } catch (_: Exception) {}

        // Sincronizar parámetros
        applyBands(currentBands)
        applyPreAmpDb(currentPreAmpDb)
    }

    fun onSessionClosed(sessionId: Int) {
        playerSessions.remove(sessionId)
        sessionDynamics.remove(sessionId)?.let { try { it.release() } catch (_: Exception) {} }
        sessionEqualizers.remove(sessionId)?.let { try { it.release() } catch (_: Exception) {} }
        sessionBassBoosts.remove(sessionId)?.let { try { it.release() } catch (_: Exception) {} }
        sessionLoudness.remove(sessionId)?.let { try { it.release() } catch (_: Exception) {} }
    }

    fun release() {
        isAnchorRunning = false
        try { silentAnchorTrack?.stop(); silentAnchorTrack?.release() } catch (_: Exception) {}
        silentAnchorTrack = null

        try { dynamicsProcessing?.enabled = false; dynamicsProcessing?.release() } catch (_: Exception) {}
        dynamicsProcessing = null

        sessionDynamics.values.forEach { try { it.release() } catch (_: Exception) {} }
        sessionDynamics.clear()

        try { legacyEqualizer?.release() } catch (_: Exception) {}
        legacyEqualizer = null
        sessionEqualizers.values.forEach { try { it.release() } catch (_: Exception) {} }
        sessionEqualizers.clear()

        try { legacyBassBoost?.release() } catch (_: Exception) {}
        legacyBassBoost = null
        sessionBassBoosts.values.forEach { try { it.release() } catch (_: Exception) {} }
        sessionBassBoosts.clear()

        try { legacyVirtualizer?.release() } catch (_: Exception) {}
        legacyVirtualizer = null

        try { legacyLoudnessEnhancer?.release() } catch (_: Exception) {}
        legacyLoudnessEnhancer = null
        sessionLoudness.values.forEach { try { it.release() } catch (_: Exception) {} }
        sessionLoudness.clear()

        playerSessions.clear()
    }
}
