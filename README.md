# 🎚️ Surgical Audio EQ & ANC Lab (HTC NE76 Pro Edition)

[![Build and Auto-Release APK](https://github.com/nasar0/EQ-apk/actions/workflows/build-and-release.yml/badge.svg)](https://github.com/nasar0/EQ-apk/actions/workflows/build-and-release.yml)
[![Direct APK Download](https://img.shields.io/badge/Download-Latest%20APK-00F5D4?style=flat&logo=android)](https://github.com/nasar0/EQ-apk/releases/latest/download/Surgical-Audio-EQ-HTCNE76.apk)

Aplicación nativa de Android de ecualización global quirúrgica y cancelación/aislamiento de ruido por software, optimizada para los auriculares **HTC NE76**.

100% libre de anuncios, telemetría y librerías de pago.

---

## ⚡ Novedad: CI/CD y Actualizaciones Automáticas sin Descargar Manualmente
Este repositorio incluye un pipeline completo de **GitHub Actions** (`.github/workflows/build-and-release.yml`) que:
1. Cada vez que se hace un `git push` o commit en la rama `main`, **compila automáticamente el APK**.
2. Publica de forma autónoma una nueva **GitHub Release** con el APK compilado listo para instalar.
3. La aplicación cuenta con un módulo interno **OTA Auto-Updater** ([`AppUpdater.kt`](app/src/main/java/com/surgical/eq/htcne76/updater/AppUpdater.kt)) que detecta nuevas versiones al abrir la app o al pulsar el botón de comprobación.
4. Al pulsar **"Actualizar Ahora"**, la aplicación descarga e instala la nueva versión sobre sí misma **sin perder tus ajustes, frecuencias ni presets**.

---

## 🎯 Especificaciones de Audio DSP
- **Motor Principal:** `DynamicsProcessing` (Android API 28+) configurado en modo `VARIANT_FAVOR_FREQUENCY_RESOLUTION` con etapa de **Limitador (Limiter)** de 1 ms de ataque y ratio 10:1 para eliminar clipping y distorsión inter-sample.
- **Fallback Inteligente:** Soporte retrocompatible con `Equalizer`, `BassBoost`, `Virtualizer` y `LoudnessEnhancer`.
- **Integración Global:** Funciona en segundo plano con la pantalla apagada gracias a su **ForegroundService** nativo con tipo `mediaPlayback | microphone` y enlaza automáticamente con **Spotify, YouTube Music, Tidal, etc.**

### 🎛️ Control Híbrido Quirúrgico (Sliders + Input Numérico Directo)
Ningún parámetro se limita a una barra deslizante:
- **Ganancia (dB):** Slider ±15.0 dB + caja numérica para escribir valores con decimales exactos (`+5.5`, `-1.5`).
- **Frecuencia (Hz):** Slider de 20 Hz a 20.000 Hz + edición numérica directa con el teclado.
- **Factor Q:** Ajuste fino del ancho de campana (0.30 a 5.00).
- **Efectos:** BassBoost (0-1000), Loudness Enhancer (0-15 dB), Espacializador 3D (0-1000) y Ganancia Pre-Amp.

---

## 🛡️ Laboratorio de Cancelación y Aislamiento de Ruido (ANC Lab)
Dado que los HTC NE76 no cuentan con procesador de hardware ANC, implementa 3 modos combinables por software:

1. **Modo 1: ANC por Inversión de Fase (Baja Latencia):**
   - Captura de audio en tiempo real desde el micrófono (`AudioRecord` en baja latencia e hilo de prioridad máxima de audio).
   - Filtro paso-bajo Biquad (< 600 Hz).
   - Inversión de fase de 180° con ganancia regulable.
   - **Búfer circular de retardo (ms y muestras):** Permite sincronizar y compensar la latencia del códec Bluetooth hasta anular zumbidos continuos (motores, ventiladores, tráfico).
2. **Modo 2: Cancelación Psicoacústica Adaptativa (Smart Noise Masking):**
   - Sonómetro RMS ambiental en tiempo real con indicador dinámico en dB SPL.
   - Síntesis de **Ruido Marrón**, **Ruido Rosa** e **Híbrido** sin allocations de memoria.
   - Auto-regula la potencia de enmascaramiento acústico para dormir protegiendo frente a ronquidos o ruidos súbitos.
3. **Modo 3: Limpieza DSP Anti-Hiss & Sub-Hum:**
   - Filtro digital paso-alto a 40 Hz para eliminar ruidos de red y masa eléctrica.
   - Supresión de siseo Bluetooth (> 8 kHz) mediante el compresor multibanda de `DynamicsProcessing`.

---

## 🎧 Presets Predefinidos
- **HTC NE76 Pro Tuned:** 60Hz (+5.5dB), 150Hz (+3.5dB), 400Hz (-1.5dB para remover sonido encajonado), 1000Hz (+1.0dB), 3500Hz (+3.0dB), 12000Hz (+4.0dB), BassBoost 600, Loudness +3.5dB.
- **Aislamiento Total + Dormir:** Anti-siseo activo, corte de agudos y enmascaramiento cálido.
- **Bass Extreme:** Subgraves reforzados al máximo con protección de limitador para cero distorsión.
- **Guardado de Presets Personalizados:** Crea, nombra y administra tus propios perfiles.

---

## 🛠️ Compilación Manual
Requiere JDK 17 y Android SDK (API 34):
```bash
./gradlew assembleDebug
```
El archivo APK se genera en:
`app/build/outputs/apk/debug/app-debug.apk`
