package com.surgical.eq.htcne76

import android.app.Application
import com.surgical.eq.htcne76.data.EqDataStore
import com.surgical.eq.htcne76.service.AudioEqService

class SurgicalEqApp : Application() {

    lateinit var eqDataStore: EqDataStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        eqDataStore = EqDataStore(this)

        // Iniciar servicio en primer plano para ecualización global
        AudioEqService.startService(this)
    }

    companion object {
        lateinit var instance: SurgicalEqApp
            private set
    }
}
