package com.hero

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class TelemetryService : AccessibilityService() {

    companion object {
        // Ładujemy naszą skompilowaną bibliotekę z Rusta.
        // Nazwa musi pasować do "libname" z Cargo.toml (hero_core)
        init {
            System.loadLibrary("hero_core")
        }
    }

    // Deklaracja natywnej funkcji z Rusta (nasze JNI)
    external fun analyzeTouch(x: Int, y: Int, pressure: Float, velocity: Float): Boolean

    // To jest główna pętla nasłuchująca Androida
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // W pełnej wersji (przyznane uprawnienia Full Access) 
        // wyciągamy tutaj fizyczne dane z ekranu/ruchu.
        // Na potrzeby PoC załóżmy, że mamy strumień danych:
        val simulatedPressure = 1.6f
        val simulatedVelocity = 1200.0f

        // Przekazujemy dane do Rusta!
        val triggerGlitch = analyzeTouch(0, 0, simulatedPressure, simulatedVelocity)

        if (triggerGlitch) {
            Log.w("HERO_NODE_ZERO", "Rust zadecydował: Próg frustracji przekroczony!")
            // Tutaj w przyszłości wstrzykniemy nasz Terminal UI (SYSTEM_ALERT_WINDOW)
        }
    }

    override fun onInterrupt() {
        Log.e("HERO_NODE_ZERO", "Usługa została przerwana przez system.")
    }

    // =========================================================
    // CALLBACK DLA RUSTA: Tu uderza JNI, gdy "żaba się ugotuje"
    // =========================================================
    fun triggerAnomalyVibration() {
        Log.e("HERO_NODE_ZERO", "Sygnał z Rusta: Uruchamiam anomalię haptyczną!")
        
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        
        if (vibrator.hasVibrator()) {
            // Nasz asymetryczny wodotrysk: [start natychmiast, krótki strzał, cisza, długie rzężenie]
            val timings = longArrayOf(0, 15, 50, 250)
            val amplitudes = intArrayOf(0, 255, 0, 80)
            
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        }
    }
}