package com.im_a_hero.daemon

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log

class TelemetryService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private var terminalView: WebView? = null
    private var isHijacked = false

    // Zmienne Radaru Frustracji
    private var actionCount = 0
    private var lastActionTime = 0L
    private val FRUSTRATION_LIMIT = 15 // Ile wściekłych akcji odpala BUM
    private val TIME_WINDOW = 3000L // W jakim czasie (3 sekundy)

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Log.i("HERO_NODE", "Demon nasłuchuje w cieniu...")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || isHijacked) return

        val eventType = event.eventType
        
        // Interesują nas tylko kliknięcia i scrolle
        if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED || eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            val currentTime = System.currentTimeMillis()

            // Jeśli minęło za dużo czasu od ostatniej akcji, resetujemy licznik
            if (currentTime - lastActionTime > TIME_WINDOW) {
                actionCount = 0
            }

            actionCount++
            lastActionTime = currentTime

            Log.d("HERO_NODE", "Poziom frustracji: $actionCount / $FRUSTRATION_LIMIT")

            // Próg przekroczony -> Odpalamy The Hijack!
            if (actionCount >= FRUSTRATION_LIMIT) {
                actionCount = 0 // Reset
                triggerTheHijack()
            }
        }
    }

    private fun triggerTheHijack() {
        if (isHijacked) return
        isHijacked = true
        Log.w("HERO_NODE", "BUM! Przejmuję ekran.")

        // Parametry nakładki (OVERLAY) rysowanej nad innymi aplikacjami
        WebView.setWebContentsDebuggingEnabled(true)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, // Wymaga uprawnień, ale daje pełną władzę
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, // Pozwala pisać na klawiaturze w terminalu
            PixelFormat.OPAQUE
        )

        // Tworzymy na żywo przeglądarkę i wstrzykujemy jej nasz kod
        Handler(Looper.getMainLooper()).post {
            terminalView = WebView(this).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                addJavascriptInterface(WebAppInterface(), "AndroidBridge")
                webViewClient = WebViewClient()
                loadUrl("file:///android_asset/index.html")
            }
            windowManager.addView(terminalView, params)
        }
    }

    // Ten interfejs jest widoczny w JS-ie Twojego terminala!
    inner class WebAppInterface {
        @JavascriptInterface
        fun closeTerminal() {
            Log.i("HERO_NODE", "Zagadka rozwiązana. Zdejmuję blokadę.")
            Handler(Looper.getMainLooper()).post {
                terminalView?.let {
                    windowManager.removeView(it)
                    terminalView = null
                    isHijacked = false
                }
            }
        }
    }

    override fun onInterrupt() {}
}