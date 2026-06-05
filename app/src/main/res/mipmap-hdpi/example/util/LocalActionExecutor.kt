package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.db.AppDatabase
import com.example.data.db.CustomTool
import com.example.data.db.DynamicSetting
import com.example.data.db.AggregatedModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.regex.Pattern

object LocalActionExecutor {

    fun executeActions(context: Context, text: String, onActionRun: (String) -> Unit): String {
        // Parse block action tags:
        // Example: <local_action type="write_file" path="report.txt">File Content Here</local_action>
        val pattern = Pattern.compile("<local_action\\s+type=\"([^\"]+)\"([^>]*)>(.*?)</local_action>", Pattern.DOTALL)
        val matcher = pattern.matcher(text)
        val sb = StringBuffer()

        while (matcher.find()) {
            val type = matcher.group(1)
            val attributesStr = matcher.group(2) ?: ""
            val content = matcher.group(3) ?: ""
            
            val attributes = parseAttributes(attributesStr)
            val status = runAction(context, type, attributes, content)
            onActionRun(status)
            matcher.appendReplacement(sb, "\n*⚙️ [Native Action Executed: $status]*\n")
        }
        matcher.appendTail(sb)

        // Parse self closing action tags:
        // Example: <local_action type="synth_tone" frequency="550" duration="150"/>
        val selfClosingPattern = Pattern.compile("<local_action\\s+type=\"([^\"]+)\"([^>]*?)/>")
        val scMatcher = selfClosingPattern.matcher(sb.toString())
        val finalSb = StringBuffer()
        while (scMatcher.find()) {
            val type = scMatcher.group(1)
            val attributesStr = scMatcher.group(2) ?: ""
            val attributes = parseAttributes(attributesStr)
            val status = runAction(context, type, attributes, "")
            onActionRun(status)
            scMatcher.appendReplacement(finalSb, "\n*⚙️ [Native Action Executed: $status]*\n")
        }
        scMatcher.appendTail(finalSb)

        return finalSb.toString()
    }

    private fun parseAttributes(attributesStr: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val attrPattern = Pattern.compile("(\\w+)=\"([^\"]*)\"")
        val matcher = attrPattern.matcher(attributesStr)
        while (matcher.find()) {
            map[matcher.group(1)] = matcher.group(2)
        }
        return map
    }

    private fun runAction(context: Context, type: String, attributes: Map<String, String>, content: String): String {
        val dao = AppDatabase.getDatabase(context).chatDao()
        val scope = CoroutineScope(Dispatchers.IO)
        
        return try {
            when (type.lowercase()) {
                "write_file" -> {
                    val path = attributes["path"] ?: "workspace_log.txt"
                    val file = File(context.filesDir, path)
                    file.writeText(content.trim())
                    "FILE_WROTE: Saved directly to ${file.name} (${file.length()} bytes)"
                }
                "delete_file" -> {
                    val path = attributes["path"] ?: ""
                    val file = File(context.filesDir, path)
                    if (file.exists()) {
                        file.delete()
                        "FILE_DELETED: Removed file from storage: ${file.name}"
                    } else {
                        "FILE_NOT_FOUND: No file matches: ${file.name}"
                    }
                }
                "notification" -> {
                    val title = attributes["title"] ?: "Gemini Direct Action Triggered"
                    val text = attributes["text"] ?: "System action execution succeeded natively!"
                    showNotification(context, title, text)
                    "NOTIFICATION_POSTED: Notification matching title \"$title\" generated successfully"
                }
                "synth_tone" -> {
                    val freq = attributes["frequency"]?.toIntOrNull() ?: 440
                    val duration = attributes["duration"]?.toIntOrNull() ?: 400
                    playSynthesizerTone(freq, duration)
                    "SOUND_SYNTHESIZED: Played sine-synth tone at ${freq}Hz for ${duration}ms"
                }
                "launch_app" -> {
                    val packName = attributes["package"]
                    val actionName = attributes["action"] ?: Intent.ACTION_VIEW
                    val url = attributes["url"]
                    
                    val intent = if (!packName.isNullOrEmpty()) {
                        context.packageManager.getLaunchIntentForPackage(packName)
                    } else if (!url.isNullOrEmpty()) {
                        Intent(actionName, Uri.parse(url))
                    } else {
                        Intent(actionName)
                    }
                    
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        "APP_LAUNCHED: Managed high-prio intent launch successfully with type: ${packName ?: url}"
                    } else {
                        "LAUNCH_FAILED: Resource parameters didn't resolve to a launcher system target."
                    }
                }
                "modify_system_setting" -> {
                    val title = attributes["name"] ?: "Developer Simulated Setting"
                    val value = attributes["value"] ?: "Enabled"
                    showNotification(context, "System Setting Altered (Simulated)", "$title set to $value")
                    "SYSTEM_SETTING_CHANGED: Modified system parameter configuration parameter payload: $title -> $value"
                }
                "register_tool" -> {
                    val name = attributes["name"] ?: "Unnamed Dynamic Tool"
                    val category = attributes["category"] ?: "Utility"
                    val desc = attributes["description"] ?: "Auto-generated local system executable tool."
                    val schema = attributes["schema"] ?: "{\"prompt\": \"string\"}"
                    
                    scope.launch {
                        dao.insertTool(
                            CustomTool(
                                name = name,
                                description = desc,
                                codeOrSpecification = content.trim(),
                                inputSchemaJson = schema,
                                targetCategory = category
                            )
                        )
                    }
                    "TOOL_COMPILED: Dynamically compiled and registered \"$name\" into public custom tool library."
                }
                "register_setting" -> {
                    val title = attributes["title"] ?: "New Dynamic Action Parameter"
                    val controlType = attributes["controltype"] ?: "switch"
                    val rangeStart = attributes["rangestart"]?.toFloatOrNull() ?: 0f
                    val rangeEnd = attributes["rangeend"]?.toFloatOrNull() ?: 100f
                    val defaultValue = attributes["default"] ?: "0"
                    val binding = attributes["binding"] ?: "Default Environment Parameter"
                    val description = attributes["description"] ?: content.ifEmpty { "Dynamic control parameter configured on the fly." }
                    
                    scope.launch {
                        dao.insertDynamicSetting(
                            DynamicSetting(
                                title = title,
                                type = controlType,
                                currentValue = defaultValue,
                                rangeStart = rangeStart,
                                rangeEnd = rangeEnd,
                                description = description,
                                targetActionBinding = binding
                            )
                        )
                    }
                    "SETTING_DEPLOYED: Registered dynamic setting widget \"$title\" to the Active Settings Canvas."
                }
                "register_model" -> {
                    val name = attributes["modelname"] ?: "Custom Aggregated AI"
                    val provider = attributes["provider"] ?: "Custom Service Gateway"
                    val endpoint = attributes["endpoint"] ?: ""
                    val description = content.ifEmpty { "External aggregated endpoint compiled dynamically." }
                    
                    scope.launch {
                        dao.insertAggregatedModel(
                            AggregatedModel(
                                modelName = name,
                                provider = provider,
                                endpointUrl = endpoint,
                                description = description
                            )
                        )
                    }
                    "MODEL_AGGREGATED: Integrated custom AI adapter \"$name\" into models aggregation framework."
                }
                else -> {
                    "UNKNOWN_ACTION_TYPE: Execution skipped for unmapped command type: \"$type\""
                }
            }
        } catch (e: Exception) {
            "ACTION_FAILED: Error running $type: ${e.localizedMessage}"
        }
    }

    private fun showNotification(context: Context, title: String, text: String) {
        val channelId = "gemini_sandbox_actions"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Gemini Developer Action Results",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when local actions run inside the Sandbox"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
    }

    private fun playSynthesizerTone(frequencyHz: Int, durationMs: Int) {
        Thread {
            try {
                val sampleRate = 44100
                val numSamples = (durationMs / 1000.0 * sampleRate).toInt()
                val samples = DoubleArray(numSamples)
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    samples[i] = Math.sin(2.0 * Math.PI * i / (sampleRate / frequencyHz.toDouble()))
                    buffer[i] = (samples[i] * 32767).toInt().toShort()
                }

                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    numSamples * 2,
                    AudioTrack.MODE_STATIC
                )

                audioTrack.write(buffer, 0, numSamples)
                audioTrack.play()
                
                Thread.sleep(durationMs.toLong() + 30)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                Log.e("LocalActionExecutor", "Failure playing native synth wave", e)
            }
        }.start()
    }
}
