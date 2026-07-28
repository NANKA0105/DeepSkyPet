package com.deepsky.pet

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.deepsky.pet.service.OverlayService

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.status_text)
        val startBtn = findViewById<Button>(R.id.start_btn)
        val permissionBtn = findViewById<Button>(R.id.permission_btn)

        if (!Settings.canDrawOverlays(this)) {
            statusText.text = "需要悬浮窗权限"
            startBtn.isEnabled = false
            permissionBtn.setOnClickListener {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        } else {
            statusText.text = "✨ DeepSky 就绪"
            permissionBtn.isEnabled = false
            permissionBtn.text = "权限已授予 ✓"
        }

        startBtn.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                val intent = Intent(this, OverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                Toast.makeText(this, "狄深已降临 ✨", Toast.LENGTH_SHORT).show()
                moveTaskToBack(true)
            }
        }

        if (!hasUsageStatsPermission()) {
            findViewById<Button>(R.id.usage_btn).setOnClickListener {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        } else {
            findViewById<Button>(R.id.usage_btn).isEnabled = false
            findViewById<Button>(R.id.usage_btn).text = "使用统计已授权 ✓"
        }
    }

    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) {
            findViewById<TextView>(R.id.status_text).text = "✨ DeepSky 就绪"
            findViewById<Button>(R.id.start_btn).isEnabled = true
            findViewById<Button>(R.id.permission_btn).isEnabled = false
            findViewById<Button>(R.id.permission_btn).text = "权限已授予 ✓"
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val ts = System.currentTimeMillis()
        val usm = getSystemService(USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val stats = usm.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_DAILY,
            ts - 1000 * 86400,
            ts
        )
        return !stats.isNullOrEmpty()
    }
}
