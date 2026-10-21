package com.surgical.eq.htcne76.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.surgical.eq.htcne76.MainActivity
import com.surgical.eq.htcne76.R
import com.surgical.eq.htcne76.data.EqDataStore
import com.surgical.eq.htcne76.dsp.GlobalEqEngine
import com.surgical.eq.htcne76.dsp.NoiseControlEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AudioEqService : Service() {

    companion object {
        private const val TAG = "AudioEqService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "surgical_eq_service_channel"

        const val ACTION_START = "com.surgical.eq.ACTION_START"
        const val ACTION_STOP = "com.surgical.eq.ACTION_STOP"
        const val ACTION_TOGGLE_MASTER = "com.surgical.eq.ACTION_TOGGLE_MASTER"

        @Volatile
        var serviceInstance: AudioEqService? = null
            private set

        fun startService(context: Context) {
            val intent = Intent(context, AudioEqService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AudioEqService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    inner class LocalBinder : Binder() {
        val service: AudioEqService get() = this@AudioEqService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val globalEqEngine = GlobalEqEngine()
    lateinit var noiseControlEngine: NoiseControlEngine
        private set

    private lateinit var eqDataStore: EqDataStore
    private var isMasterEnabled = true
    private var currentPresetName = "HTC NE76 Pro Tuned"

    override fun onCreate() {
        super.onCreate()
        serviceInstance = this
        eqDataStore = EqDataStore(this)
        noiseControlEngine = NoiseControlEngine(this)

        createNotificationChannel()
        startInForeground()

        // Inicializar motor DSP en sesión global
        globalEqEngine.init(0)

        // Escuchar cambios de estado en DataStore para aplicar al vuelo
        observeDataStore()
    }

    private fun observeDataStore() {
        // Observar estado maestro y preset
        serviceScope.launch {
            combine(
                eqDataStore.masterEqEnabledFlow,
                eqDataStore.selectedPresetIdFlow
            ) { enabled, presetId ->
                Pair(enabled, presetId)
            }.collect { (enabled, presetId) ->
                isMasterEnabled = enabled
                currentPresetName = presetId
                globalEqEngine.setEnabled(enabled)
                updateNotification()
            }
        }

        // Observar bandas de ecualización
        serviceScope.launch {
            eqDataStore.eqBandsFlow.collect { bands ->
                globalEqEngine.applyBands(bands)
            }
        }

        // Observar efectos DSP (BassBoost, Virtualizer, Loudness, PreAmp)
        serviceScope.launch {
            eqDataStore.bassBoostFlow.collect { boost ->
                globalEqEngine.applyBassBoost(boost)
            }
        }

        serviceScope.launch {
            eqDataStore.virtualizerFlow.collect { virt ->
                globalEqEngine.applyVirtualizer(virt)
            }
        }

        serviceScope.launch {
            eqDataStore.loudnessDbFlow.collect { loudness ->
                globalEqEngine.applyLoudnessDb(loudness)
            }
        }

        serviceScope.launch {
            eqDataStore.preAmpDbFlow.collect { preAmp ->
                globalEqEngine.applyPreAmpDb(preAmp)
            }
        }

        // Observar configuración del laboratorio de cancelación de ruido
        serviceScope.launch {
            eqDataStore.noiseControlConfigFlow.collect { config ->
                noiseControlEngine.updateConfig(config)
                globalEqEngine.setHissFilterActive(config.dspNoiseGateEnabled)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_MASTER -> {
                serviceScope.launch {
                    eqDataStore.setMasterEqEnabled(!isMasterEnabled)
                }
            }
            ACTION_START -> {
                updateNotification()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun startInForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            }
            startForeground(NOTIFICATION_ID, notification, fgsType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, AudioEqService::class.java).apply {
            action = ACTION_TOGGLE_MASTER
        }
        val togglePendingIntent = PendingIntent.getService(
            this, 1, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AudioEqService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isMasterEnabled) {
            "Motor activo • Perfil: $currentPresetName"
        } else {
            "Motor en bypass (Desactivado)"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Surgical EQ & ANC")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(
                0,
                if (isMasterEnabled) "Desactivar EQ" else "Activar EQ",
                togglePendingIntent
            )
            .addAction(0, "Detener", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        serviceInstance = null
        globalEqEngine.release()
        noiseControlEngine.stop()
        Log.i(TAG, "AudioEqService detenido y recursos liberados.")
    }
}
