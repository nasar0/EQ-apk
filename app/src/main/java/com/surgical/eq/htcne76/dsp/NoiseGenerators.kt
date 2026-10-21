package com.surgical.eq.htcne76.dsp

import java.util.Random

/**
 * Generadores sintetizados de ruido psicoacústico en tiempo real sin allocations de memoria.
 */
class NoiseGenerators {

    private val random = Random()

    // Estados internos para Ruido Marrón (Brownian / Red Noise)
    private var brownState = 0.0f

    // Estados internos para Ruido Rosa (Algoritmo de Paul Kellet con 3 polos)
    private var b0 = 0.0f
    private var b1 = 0.0f
    private var b2 = 0.0f

    fun reset() {
        brownState = 0.0f
        b0 = 0.0f
        b1 = 0.0f
        b2 = 0.0f
    }

    /**
     * Genera una muestra de Ruido Blanco entre -1.0f y 1.0f
     */
    fun nextWhite(): Float {
        return random.nextFloat() * 2.0f - 1.0f
    }

    /**
     * Ruido Marrón (1/f^2): espectro profundo y relajante, excelente para bloquear
     * ruidos graves (motores, ronquidos, tráfico) y descansar.
     */
    fun nextBrown(): Float {
        val white = nextWhite()
        // Integrador con fuga (leaky integrator) para evitar deriva DC
        brownState = (brownState * 0.985f) + (white * 0.05f)
        return (brownState * 3.5f).coerceIn(-1.0f, 1.0f)
    }

    /**
     * Ruido Rosa (1/f): misma energía acústica por octava, óptimo para enmascarar
     * voces humanas, ladridos y ruidos del hogar.
     */
    fun nextPink(): Float {
        val white = nextWhite()
        b0 = 0.99765f * b0 + white * 0.0990460f
        b1 = 0.96300f * b1 + white * 0.1065835f
        b2 = 0.57000f * b2 + white * 0.3735835f
        val pink = b0 + b1 + b2 + white * 0.05362f
        return (pink * 0.15f).coerceIn(-1.0f, 1.0f)
    }

    /**
     * Ruido Híbrido: mezcla equilibrada de 70% Marrón + 30% Rosa para máxima cobertura.
     */
    fun nextHybrid(): Float {
        val brown = nextBrown()
        val pink = nextPink()
        return (brown * 0.70f + pink * 0.30f).coerceIn(-1.0f, 1.0f)
    }
}
