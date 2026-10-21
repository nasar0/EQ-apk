package com.surgical.eq.htcne76.dsp

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.core.content.ContextCompat
import com.surgical.eq.htcne76.data.NoiseControlConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Motor de Cancelación y Aislamiento de Ruido por Software (ANC Lab)
 *
 * Implementa 3 modos combinables:
 * 1. ANC por Inversión de Fase de baja latencia con compensación de milisegundos Bluetooth.
 * 2. Enmascaramiento Psicoacústico Inteligente (Ruido Marrón/Rosa sintetizado adaptativo).
 * 3. Filtrado digital DSP de siseo (hiss) y zumbido de masa (sub-hum).
 */
class NoiseControlEngine(private val context: Context) {

    companion object {
        private const val TAG = "NoiseControlEngine"
        private const val SAMPLE_RATE = 48000
        private const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val MAX_DELAY_MS = 60.0f
        private const val MAX_DELAY_SAMPLES = (SAMPLE_RATE * (MAX_DELAY_MS / 1000f)).toInt() + 1024
    }

    private val isRunning = AtomicBoolean(false)
    private var processingThread: Thread? = null

    // Dispositivos de audio nativos
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    // Filtros DSP y Generadores de Ruido
    private val lowPassFilter = BiquadFilter()
    private val subHumFilter = BiquadFilter()
    private val noiseGenerators = NoiseGenerators()

    // Búfer circular para compensación de latencia y fase Bluetooth
    private val delayBuffer = FloatArray(MAX_DELAY_SAMPLES)
    private var writeIndex = 0

    // Configuración activa
    @Volatile
    private var currentConfig = NoiseControlConfig()

    // Medición de Decibelios en tiempo real para la UI
    private val _ambientDbFlow = MutableStateFlow(30.0f)
    val ambientDbFlow: StateFlow<Float> = _ambientDbFlow.asStateFlow()

    init {
        updateFilterParameters()
    }

    fun updateConfig(config: NoiseControlConfig) {
        val isNowNeeded = isEngineNeeded(config)
        this.currentConfig = config
        updateFilterParameters()

        if (isNowNeeded && !isRunning.get()) {
            start()
        } else if (!isNowNeeded && isRunning.get()) {
            stop()
        }
    }

    private fun isEngineNeeded(config: NoiseControlConfig): Boolean {
        return config.phaseInversionEnabled || config.smartMaskingEnabled || config.dspNoiseGateEnabled
    }

    private fun updateFilterParameters() {
        lowPassFilter.configureLowPass(
            sampleRate = SAMPLE_RATE.toFloat(),
            cutoffHz = currentConfig.lowPassCutoffHz,
            q = 0.7071f
        )
        subHumFilter.configureHighPass(
            sampleRate = SAMPLE_RATE.toFloat(),
            cutoffHz = currentConfig.subHumCutoffHz,
            q = 0.7071f
        )
    }

    @Synchronized
    fun start() {
        if (isRunning.get()) return

        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        isRunning.set(true)

        processingThread = Thread {
            runAudioLoop(hasMicPermission)
        }.apply {
            name = "SurgicalNoiseEngineThread"
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    @Synchronized
    fun stop() {
        isRunning.set(false)
        try {
            processingThread?.join(500)
        } catch (_: InterruptedException) {}
        processingThread = null

        releaseHardware()
    }

    private fun runAudioLoop(canRecord: Boolean) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

        val minRecordBufSize = if (canRecord) {
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT)
        } else 1024

        val minTrackBufSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT)
        val bufferSizeInShorts = max(256, minRecordBufSize / 4)

        if (canRecord) {
            try {
                audioRecord = AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AUDIO_FORMAT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(CHANNEL_CONFIG_IN)
                            .build()
                    )
                    .setBufferSizeInBytes(minRecordBufSize * 2)
                    .build()
                audioRecord?.startRecording()
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo iniciar AudioRecord: ${e.message}")
            }
        }

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AUDIO_FORMAT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG_OUT)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(minTrackBufSize * 2)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo iniciar AudioTrack de baja latencia: ${e.message}")
        }

        val inBuffer = ShortArray(bufferSizeInShorts)
        val outBuffer = ShortArray(bufferSizeInShorts)
        var dbUpdateCounter = 0

        while (isRunning.get()) {
            val config = currentConfig
            var shortsRead = 0

            if (audioRecord != null && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                shortsRead = audioRecord?.read(inBuffer, 0, bufferSizeInShorts) ?: 0
            }

            if (shortsRead <= 0) {
                // Si el micrófono no está leyendo, limpiamos buffer de entrada
                inBuffer.fill(0)
                shortsRead = bufferSizeInShorts
            }

            // 1. Medir nivel de decibelios del ruido ambiente (RMS)
            var sumSquares = 0.0
            for (i in 0 until shortsRead) {
                val s = inBuffer[i].toDouble()
                sumSquares += s * s
            }
            val rms = sqrt(sumSquares / shortsRead)
            val measuredDb = (20.0 * log10(max(rms, 1.0) / 32767.0) + 94.0).toFloat().coerceIn(20f, 110f)

            dbUpdateCounter++
            if (dbUpdateCounter % 15 == 0) {
                _ambientDbFlow.value = measuredDb
            }

            // Calcular ganancia adaptativa de enmascaramiento según decibelios
            val maskingTargetGain = if (config.smartMaskingEnabled) {
                if (measuredDb > config.ambientThresholdDb) {
                    val excess = (measuredDb - config.ambientThresholdDb) / 20.0f
                    (config.maskingMaxGain * (0.6f + 0.4f * excess)).coerceIn(0.05f, config.maskingMaxGain)
                } else {
                    config.maskingMaxGain * 0.4f
                }
            } else 0.0f

            // 2. Procesamiento PCM muestra a muestra
            val delaySamples = ((config.phaseDelayMs * SAMPLE_RATE) / 1000.0f).roundToInt()
                .coerceIn(0, MAX_DELAY_SAMPLES - 1)

            for (i in 0 until shortsRead) {
                var inputSampleFloat = inBuffer[i] / 32768.0f

                // Modo 3: Sub-hum filter (High-pass para cortar ruidos parásitos de red)
                if (config.dspNoiseGateEnabled) {
                    inputSampleFloat = subHumFilter.process(inputSampleFloat)
                }

                // Escribir en búfer circular de retardo
                delayBuffer[writeIndex] = inputSampleFloat
                val readIndex = (writeIndex - delaySamples + MAX_DELAY_SAMPLES) % MAX_DELAY_SAMPLES
                val delayedSample = delayBuffer[readIndex]
                writeIndex = (writeIndex + 1) % MAX_DELAY_SAMPLES

                var outputAccumulator = 0.0f

                // Modo 1: Inversión de Fase de baja latencia con filtro paso-bajo
                if (config.phaseInversionEnabled) {
                    val lowFreqComponent = lowPassFilter.process(delayedSample)
                    // Inversión exacta de fase: onda anti-ruido con ganancia calibrable
                    val antiNoise = -lowFreqComponent * config.inversionGain
                    outputAccumulator += antiNoise
                }

                // Modo 2: Ruido Psicoacústico Inteligente
                if (config.smartMaskingEnabled && maskingTargetGain > 0.001f) {
                    val noiseSample = when (config.noiseType) {
                        "BROWN" -> noiseGenerators.nextBrown()
                        "PINK" -> noiseGenerators.nextPink()
                        else -> noiseGenerators.nextHybrid()
                    }
                    outputAccumulator += noiseSample * maskingTargetGain
                }

                // Limitador suave tipo tanh para evitar feedback acústico o distorsión dura
                val limitedOutput = tanh(outputAccumulator.toDouble()).toFloat()
                outBuffer[i] = (limitedOutput * 32767.0f).roundToInt().coerceIn(-32768, 32767).toShort()
            }

            // Reproducir la señal procesada si al menos un modo de audio activo está encendido
            if (config.phaseInversionEnabled || config.smartMaskingEnabled) {
                audioTrack?.write(outBuffer, 0, shortsRead)
            } else {
                // Modo reposo
                try {
                    Thread.sleep(15)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }

        releaseHardware()
    }

    private fun releaseHardware() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
