package com.deepsky.pet.perception

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class BatteryMonitor(
    private val context: Context,
    private val onBatteryEvent: (String, Int) -> Unit
) {
    private val receiver = object : BroadcastReceiver() {
        private var lastPlugged = -1
        private var lastLevel = -1

        override fun onReceive(ctx: Context?, intent: Intent?) {
            intent ?: return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val pct = (level * 100 / scale)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

            if (plugged != lastPlugged) {
                lastPlugged = plugged
                if (plugged > 0) onBatteryEvent("charging", pct)
                else onBatteryEvent("unplugged", pct)
            }
            if (pct != lastLevel && pct <= 15) {
                onBatteryEvent("low_battery", pct)
            }
            lastLevel = pct
        }
    }

    fun start() {
        val filter = IntentFilter().apply { addAction(Intent.ACTION_BATTERY_CHANGED) }
        context.registerReceiver(receiver, filter)
    }

    fun stop() {
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
    }
}
