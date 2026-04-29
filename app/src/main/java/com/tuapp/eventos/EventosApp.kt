package com.tuapp.eventos

import android.app.Application
import com.tuapp.eventos.utils.ThemeManager

class EventosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Aplicar el tema guardado al iniciar la aplicación
        ThemeManager(this).applySavedTheme()
    }
}
