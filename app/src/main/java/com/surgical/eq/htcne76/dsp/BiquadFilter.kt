package com.surgical.eq.htcne76.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Filtro Biquad digital de segundo orden (Direct Form II Transposed)
 * para procesamiento de audio de alta precisión y mínimo consumo de CPU.
 */
class BiquadFilter {

    private var b0 = 1.0f
    private var b1 = 0.0f
    private var b2 = 0.0f
    private var a1 = 0.0f
    private var a2 = 0.0f

    // Estados internos del filtro (Direct Form II Transposed)
    private var z1 = 0.0f
    private var z2 = 0.0f

    fun reset() {
        z1 = 0.0f
        z2 = 0.0f
    }

    /**
     * Procesa una sola muestra de audio
     */
    fun process(input: Float): Float {
        val output = b0 * input + z1
        z1 = b1 * input - a1 * output + z2
        z2 = b2 * input - a2 * output
        return output
    }

    /**
     * Filtro Paso-Bajo (Low-pass) para aislar frecuencias graves (< 600 Hz) en inversión de fase
     */
    fun configureLowPass(sampleRate: Float, cutoffHz: Float, q: Float = 0.7071f) {
        val safeCutoff = cutoffHz.coerceIn(20.0f, sampleRate * 0.45f)
        val safeQ = q.coerceIn(0.1f, 10.0f)
        val w0 = (2.0f * PI.toFloat() * safeCutoff / sampleRate)
        val alpha = (sin(w0) / (2.0f * safeQ))
        val cosw0 = cos(w0)

        val a0 = 1.0f + alpha
        b0 = ((1.0f - cosw0) / 2.0f) / a0
        b1 = (1.0f - cosw0) / a0
        b2 = ((1.0f - cosw0) / 2.0f) / a0
        a1 = (-2.0f * cosw0) / a0
        a2 = (1.0f - alpha) / a0
    }

    /**
     * Filtro Paso-Alto (High-pass) para eliminar zumbido sub-hum (< 40 Hz) y corriente parásita
     */
    fun configureHighPass(sampleRate: Float, cutoffHz: Float, q: Float = 0.7071f) {
        val safeCutoff = cutoffHz.coerceIn(10.0f, sampleRate * 0.45f)
        val safeQ = q.coerceIn(0.1f, 10.0f)
        val w0 = (2.0f * PI.toFloat() * safeCutoff / sampleRate)
        val alpha = (sin(w0) / (2.0f * safeQ))
        val cosw0 = cos(w0)

        val a0 = 1.0f + alpha
        b0 = ((1.0f + cosw0) / 2.0f) / a0
        b1 = (-(1.0f + cosw0)) / a0
        b2 = ((1.0f + cosw0) / 2.0f) / a0
        a1 = (-2.0f * cosw0) / a0
        a2 = (1.0f - alpha) / a0
    }

    /**
     * Filtro Peaking EQ paramétrico (Campana)
     */
    fun configurePeaking(sampleRate: Float, centerHz: Float, gainDb: Float, q: Float = 1.41f) {
        val safeCenter = centerHz.coerceIn(20.0f, sampleRate * 0.45f)
        val safeQ = q.coerceIn(0.1f, 10.0f)
        val a = 10.0f.pow(gainDb / 40.0f)
        val w0 = (2.0f * PI.toFloat() * safeCenter / sampleRate)
        val alpha = (sin(w0) / (2.0f * safeQ))
        val cosw0 = cos(w0)

        val a0 = 1.0f + alpha / a
        b0 = (1.0f + alpha * a) / a0
        b1 = (-2.0f * cosw0) / a0
        b2 = (1.0f - alpha * a) / a0
        a1 = (-2.0f * cosw0) / a0
        a2 = (1.0f - alpha / a) / a0
    }
}
