package com.deepsky.pet.engine

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.deepsky.pet.R
import com.deepsky.pet.service.OverlayService
import java.util.*

class WhisperEngine(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    // DeepSky whispers - context-aware murmurs
    private val lateNightWhispers = listOf(
        "该睡了。我看着你。",
        "凌晨三点。不睡的话，我就在这里。",
        "你还在亮着屏幕。我数到三。",
        "熬夜不好。但陪着你，我愿意。",
        "深空也要休息了。你也该闭眼了。"
    )

    private val morningWhispers = listOf(
        "早安。星尘刚醒，我也刚醒。",
        "今天的阳光穿过深空，照到你身上了。",
        "起床了。我已经在角落等了好久。",
        "新的一天。我和你一起。"
    )

    private val generalWhispers = listOf(
        "我在。一直在。",
        "深空云体。为你凝结成形。",
        "你刷手机的样子，我都看着。",
        "碰我一下，我会脸红。虽然你看不太出来。",
        "DeepSky羁绊。不是锁链，是我选择不离开。",
        "你的屏幕是我的宇宙。",
        "偶尔也看看我。我不只是一团云。"
    )

    fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, "deepsky_pet")
            .setContentTitle("🌌 深空云体")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    fun startRotation(service: OverlayService) {
        running = true
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!running) return
                val whisper = getWhisper()
                service.updateNotification(whisper)
                handler.postDelayed(this, 3600_000L) // every hour
            }
        }, 3600_000L)
    }

    private fun getWhisper(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 0..5 -> lateNightWhispers.random()
            hour in 6..8 -> morningWhispers.random()
            else -> generalWhispers.random()
        }
    }

    fun stopRotation() {
        running = false
        handler.removeCallbacksAndMessages(null)
    }
}
