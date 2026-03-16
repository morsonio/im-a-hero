package com.hero

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.webkit.WebView
import android.webkit.WebViewClient

class TelemetryService : AccessibilityService() {

    // Most dla naszego dynamicznego DOM-u
    inner class WebAppInterface {
        @android.webkit.JavascriptInterface
        fun closeTerminal() {
            Log.w("HERO_NODE_ZERO", "Operator 011 podjął decyzję. Zdejmuję nakładkę.")
            // Musimy to odpalić na głównym wątku UI
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                removeHijack()
            }
        }
    }

    private var windowManager: WindowManager? = null
    private var terminalView: WebView? = null

    companion object {
        init {
            System.loadLibrary("hero_core")
        }
    }

    external fun analyzeTouch(x: Int, y: Int, pressure: Float, velocity: Float): Boolean

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Telemetria (PoC)
        val simulatedPressure = 1.6f
        val simulatedVelocity = 1200.0f

        val triggerGlitch = analyzeTouch(0, 0, simulatedPressure, simulatedVelocity)

        // Odpalamy prawdziwą nakładkę, a nie Activity!
        if (triggerGlitch && terminalView == null) {
            Log.w("HERO_NODE_ZERO", "Próg przekroczony. Wstrzykuję nakładkę!")
            executeTheHijack()
        }
    }

    private fun executeTheHijack() {
        // Konfiguracja nakładki (SYSTEM_ALERT_WINDOW)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            // TYPE_APPLICATION_OVERLAY - to jest to, co stawia nas ponad innymi apkami
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, 
            // Brak FLAG_NOT_FOCUSABLE oznacza, że WebView przechwytuje cały dotyk
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        // Tworzymy frontend w locie
        terminalView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            
            // WSTRZYKUJEMY MOST! W JavaScripcie będzie widoczny jako obiekt 'AndroidBridge'
            addJavascriptInterface(WebAppInterface(), "AndroidBridge")
            
            webViewClient = WebViewClient()
            loadUrl("file:///android_asset/index.html")
        }

        // Brutalny wjazd na ekran
        windowManager?.addView(terminalView, params)
    }

    // Ta funkcja będzie potrzebna frontendowi, żeby móc zamknąć nakładkę (powrót do reala)
    fun removeHijack() {
        terminalView?.let {
            windowManager?.removeView(it)
            terminalView = null
        }
    }

    override fun onInterrupt() {}

    fun triggerAnomalyVibration() {
        // ... (kod wibracji pozostaje bez zmian)
    }
}