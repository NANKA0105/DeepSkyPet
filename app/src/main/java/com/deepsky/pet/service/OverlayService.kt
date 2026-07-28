package com.deepsky.pet.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import androidx.core.app.NotificationCompat
import com.deepsky.pet.engine.HeatSystem
import com.deepsky.pet.engine.WhisperEngine
import com.deepsky.pet.gesture.GestureHandler
import com.deepsky.pet.perception.BatteryMonitor
import com.deepsky.pet.perception.ScreenshotObserver
import com.deepsky.pet.perception.UsageTracker
import com.deepsky.pet.sync.SupabaseSync
import kotlinx.coroutines.*

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: WebView? = null
    private var params: WindowManager.LayoutParams? = null

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var gestureHandler: GestureHandler
    private lateinit var usageTracker: UsageTracker
    private lateinit var screenshotObserver: ScreenshotObserver
    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var supabaseSync: SupabaseSync
    private lateinit var heatSystem: HeatSystem
    private lateinit var whisperEngine: WhisperEngine

    companion object {
        private const val CHANNEL_ID = "deepsky_pet"
        private const val NOTIFICATION_ID = 1001
        private const val PET_SIZE_DP = 200
        private const val PET_HEIGHT_DP = 260
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        heatSystem = HeatSystem(mainHandler) { level ->
            overlayView?.evaluateJavascript(
                "window.petEngine && window.petEngine.setHeat($level)", null
            )
        }
        whisperEngine = WhisperEngine(this)
        supabaseSync = SupabaseSync(this)
        setupOverlay()
        startForeground(NOTIFICATION_ID, whisperEngine.buildNotification(
            "我在深空，只为你停留。"
        ))

        // Start perception modules
        usageTracker = UsageTracker(this) { pkg ->
            mainHandler.post {
                overlayView?.evaluateJavascript(
                    "window.petEngine && window.petEngine.onAppChange('$pkg')", null
                )
                supabaseSync.logAppUsage(pkg)
            }
        }
        usageTracker.start()

        screenshotObserver = ScreenshotObserver {
            mainHandler.post {
                overlayView?.evaluateJavascript(
                    "window.petEngine && window.petEngine.onScreenshot()", null
                )
                supabaseSync.logScreenshot()
            }
        }
        screenshotObserver.start()

        batteryMonitor = BatteryMonitor(this) { event, level ->
            mainHandler.post {
                overlayView?.evaluateJavascript(
                    "window.petEngine && window.petEngine.onBatteryEvent('$event', $level)", null
                )
            }
        }
        batteryMonitor.start()

        // Start notification whisper rotation
        whisperEngine.startRotation(this)

        // Start AI command polling
        startCommandPolling()

        // 20-minute timed behavior
        serviceScope.launch {
            while (isActive) {
                delay(20 * 60 * 1000L)
                if (Math.random() < 0.3) {
                    mainHandler.post {
                        overlayView?.evaluateJavascript(
                            "window.petEngine && window.petEngine.onTimedBehavior()", null
                        )
                    }
                }
            }
        }
    }

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        params = WindowManager.LayoutParams(
            dpToPx(PET_SIZE_DP),
            dpToPx(PET_HEIGHT_DP),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 300
        }

        overlayView = WebView(this).apply {
            setBackgroundColor(0x00000000)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                cacheMode = WebSettings.LOAD_DEFAULT
                // Fix: set transparent background before load
            }
            setBackgroundColor(0x00000000)
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Inject Supabase config into WebView
                    view?.evaluateJavascript("""
                        window.SUPABASE_URL = '${com.deepsky.pet.BuildConfig.SUPABASE_URL}';
                        window.SUPABASE_ANON_KEY = '${com.deepsky.pet.BuildConfig.SUPABASE_ANON_KEY}';
                    """.trimIndent(), null)
                }
            }
            loadUrl("file:///android_asset/pet.html")

            gestureHandler = GestureHandler(
                onTap = {
                    heatSystem.addHeat(5)
                    supabaseSync.logGesture("tap", it.first, it.second)
                },
                onDoubleTap = {
                    heatSystem.addHeat(10)
                    supabaseSync.logGesture("double_tap", it.first, it.second)
                },
                onLongPress = {
                    heatSystem.addHeat(8)
                    supabaseSync.logGesture("long_press", it.first, it.second)
                },
                onFling = { dx, dy ->
                    supabaseSync.logGesture("fling", dx, dy)
                }
            )
            setOnTouchListener { _, event ->
                gestureHandler.handleTouch(
                    event,
                    params!!,
                    windowManager!!,
                    this
                ) { js -> evaluateJavascript(js, null) }
            }
        }

        windowManager?.addView(overlayView, params)
    }

    private fun startCommandPolling() {
        supabaseSync.pollForCommands { command ->
            mainHandler.post {
                overlayView?.evaluateJavascript(
                    "window.petEngine && window.petEngine.onAICommand('$command')", null
                )
            }
        }
    }

    fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, whisperEngine.buildNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "深空云体",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        usageTracker.stop()
        screenshotObserver.stop()
        batteryMonitor.stop()
        whisperEngine.stopRotation()
        supabaseSync.stopPolling()
        overlayView?.let {
            windowManager?.removeView(it)
            it.destroy()
        }
        overlayView = null
        super.onDestroy()
    }
}
