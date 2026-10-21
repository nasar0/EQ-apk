package com.surgical.eq.htcne76

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.surgical.eq.htcne76.service.AudioEqService
import com.surgical.eq.htcne76.ui.screens.MainScreen
import com.surgical.eq.htcne76.ui.theme.SurgicalAudioEQTheme
import com.surgical.eq.htcne76.ui.viewmodel.EqViewModel
import com.surgical.eq.htcne76.ui.viewmodel.EqViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: EqViewModel by viewModels {
        EqViewModelFactory((application as SurgicalEqApp).eqDataStore)
    }

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Si el usuario concedió permisos, refrescar servicio
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            AudioEqService.startService(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Comprobar y solicitar permisos clave (Notificaciones + Micrófono para ANC)
        checkAndRequestPermissions()

        // Asegurar que el ForegroundService esté en marcha
        AudioEqService.startService(this)

        setContent {
            SurgicalAudioEQTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onResume() {
        super.onResume()
        // Asegurar que el servicio está activo
        AudioEqService.startService(this)
    }
}
