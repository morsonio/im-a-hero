package com.im_a_hero.daemon

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient

class TelemetryService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    // --- PATIENT OBSERVATION VARIABLES ---
    private var isHijacked = false
    private var terminalView: WebView? = null

    // --- SENSOR VARIABLES ---
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var currentLux: Float = -1f
    // Tracking state
    private var currentTargetApp: String? = null
    private var sessionStartTime: Long = 0
    private var sessionScrolls: Int = 0

    // Configuration of The Upside Down
    // Add real package names here (e.g., "com.zhiliaoapp.musically" for TikTok)
    private val blacklistedApps =
            setOf(
                    "com.google.android.youtube",
                    "com.instagram.android",
                    "com.facebook.katana" // Add your own triggers here
            )

    // Strike thresholds (For testing: 30 seconds and 10 scrolls)
    private val THRESHOLD_TIME_MS: Long = 30 * 1000
    private val THRESHOLD_SCROLLS: Int = 10

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("HERO_NODE", "Demon uruchomiony. Kalibracja czujników...")

        // TUTEJ BYŁ BŁĄD! ZAPOMNIELIŚMY DAĆ MU MENEDŻERA OKIEN!
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Inicjalizacja czujnika światła
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        lightSensor?.let {
            sensorManager.registerListener(lightListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private val lightListener =
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_LIGHT) {
                        currentLux = event.values[0]
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
            }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || isHijacked) return

        val packageName = event.packageName?.toString() ?: return

        // 1. WINDOW STATE CHANGED (App switching detection)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (blacklistedApps.contains(packageName)) {
                // Entered a dark zone. Start patient observation if not already tracking.
                if (currentTargetApp != packageName) {
                    currentTargetApp = packageName
                    sessionStartTime = System.currentTimeMillis()
                    sessionScrolls = 0
                    Log.d("HERO_NODE", "Target acquired: $packageName. Observation started.")
                }
            } else {
                // Exited to a safe zone. Reset tracking.
                if (currentTargetApp != null) {
                    Log.d("HERO_NODE", "Target lost. Resetting observation.")
                    currentTargetApp = null
                    sessionStartTime = 0
                    sessionScrolls = 0
                }
            }
        }

        // 2. SCROLL DETECTION (Activity tracking inside the zone)
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            // Only count scrolls if we are currently tracking a target app
            if (currentTargetApp != null && currentTargetApp == packageName) {
                sessionScrolls++

                val timeSpentMs = System.currentTimeMillis() - sessionStartTime
                Log.d("HERO_NODE", "Tracking: $timeSpentMs ms elapsed, $sessionScrolls scrolls.")

                // 3. THE STRIKE CONDITION
                if (timeSpentMs > THRESHOLD_TIME_MS && sessionScrolls >= THRESHOLD_SCROLLS) {
                    Log.w("HERO_NODE", "DOPAMINE LOOP DETECTED. Executing strike.")
                    triggerTheHijack()
                }
            }
        }
    }

    private fun triggerTheHijack() {
        if (isHijacked) return
        isHijacked = true
        Log.w("HERO_NODE", "OVERRIDE TRIGGERED. Launching terminal overlay.")

        // 1. FIRE HAPTIC FEEDBACK (Physical shock to break the habit loop)
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            // Vibrate for 500ms for a strong, sudden impact
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        }

        // 2. ENABLE DEBUGGING (For Chrome inspect if needed)
        WebView.setWebContentsDebuggingEnabled(true)

        // 3. SET OVERLAY PARAMETERS (Zwrócona wersja dla klawiatury)
        val params =
                WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        PixelFormat.OPAQUE
                )
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

        // 4. INJECT WEBVIEW
        Handler(Looper.getMainLooper()).post {
            terminalView =
                    WebView(this).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        addJavascriptInterface(WebAppInterface(), "AndroidBridge")
                        webViewClient = WebViewClient()
                        loadUrl("file:///android_asset/index.html")
                    }
            windowManager.addView(terminalView, params)

            // Wymuszenie fokusu dla klawiatury
            terminalView?.requestFocus()
        }
    }

    // Ten interfejs jest widoczny w JS-ie Twojego terminala!
    // --- RCON BRIDGE: ROZBUDOWA ---
    // Dodajemy nowe metody, które JavaScript będzie mógł wywołać, żeby pobrać twarde dane
    inner class WebAppInterface {
        @JavascriptInterface
        fun closeTerminal() {
            Handler(Looper.getMainLooper()).post {
                if (terminalView != null) {
                    windowManager.removeView(terminalView)
                    terminalView = null
                    isHijacked = false
                }
            }
        }

        // --- NOWA BROŃ: KATAPULTA ---
        @JavascriptInterface
        fun forceHome() {
            val startMain = Intent(Intent.ACTION_MAIN)
            startMain.addCategory(Intent.CATEGORY_HOME)
            startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(startMain)
        }

        @JavascriptInterface
        fun getLuxLevel(): Float {
            return currentLux
        }

        @JavascriptInterface
        fun getTimeSpentSeconds(): Long {
            if (sessionStartTime == 0L) return 0
            return (System.currentTimeMillis() - sessionStartTime) / 1000
        }
    }

    override fun onInterrupt() {}
}
