package com.deepsky.pet.engine

import android.os.Handler
import android.os.Looper

class HeatSystem(
    private val mainHandler: Handler,
    private val onHeatChanged: (Int) -> Unit
) {
    private var heat = 0
    private val decayRunnable = object : Runnable {
        override fun run() {
            if (heat > 0) {
                heat = maxOf(0, heat - 1)
                onHeatChanged(heat)
                mainHandler.postDelayed(this, 30_000L) // decay every 30s
            }
        }
    }

    fun addHeat(amount: Int) {
        heat = minOf(100, heat + amount)
        onHeatChanged(heat)
        // Restart decay timer
        mainHandler.removeCallbacks(decayRunnable)
        mainHandler.postDelayed(decayRunnable, 30_000L)
    }

    fun getHeat(): Int = heat
}
