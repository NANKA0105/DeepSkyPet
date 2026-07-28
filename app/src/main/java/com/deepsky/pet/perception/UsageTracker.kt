package com.deepsky.pet.perception

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.*

class UsageTracker(
    private val context: Context,
    private val onAppChanged: (String) -> Unit
) {
    private var timer: Timer? = null
    private var lastApp: String = ""
    private var lastAppStartTime: Long = 0

    fun start() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val current = getForegroundApp()
                if (current.isNotEmpty() && current != lastApp) {
                    // Report previous app duration
                    if (lastApp.isNotEmpty()) {
                        val duration = (System.currentTimeMillis() - lastAppStartTime) / 1000
                        // Could log duration to backend
                    }
                    lastApp = current
                    lastAppStartTime = System.currentTimeMillis()
                    onAppChanged(current)
                }
            }
        }, 0, 3000)
    }

    private fun getForegroundApp(): String {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return ""
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 5000, now)
        val event = UsageEvents.Event()
        var foreground = ""
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foreground = event.packageName
            }
        }
        return foreground
    }

    fun stop() {
        timer?.cancel()
        timer = null
    }
}
