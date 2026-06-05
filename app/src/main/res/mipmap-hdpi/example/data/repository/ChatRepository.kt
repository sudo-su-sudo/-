package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.InlineData
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.db.ChatDao
import com.example.data.db.ChatMessage
import com.example.data.db.ChatSession
import com.example.util.LocalActionExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ChatRepository(
    private val context: Context,
    private val chatDao: ChatDao
) {
    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>> =
        chatDao.getMessagesForSession(sessionId)

    suspend fun getSessionById(sessionId: Long): ChatSession? = chatDao.getSessionById(sessionId)

    suspend fun insertSession(session: ChatSession): Long = chatDao.insertSession(session)

    suspend fun updateSession(session: ChatSession) = chatDao.updateSession(session)

    suspend fun deleteSession(sessionId: Long) = chatDao.deleteSessionById(sessionId)

    suspend fun insertMessage(message: ChatMessage): Long = chatDao.insertMessage(message)

    suspend fun sendMessageAndGetReply(
        sessionId: Long,
        userText: String,
        filePath: String? = null,
        fileMimeType: String? = null,
        hardwareInfo: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: API Key is unconfigured! Please enter your Gemini API Key in the AI Studio Secrets panel."
        }

        val session = chatDao.getSessionById(sessionId) ?: return@withContext "Error: Active chat session not found."

        // 1. Add user message to historical query database
        val userMsgId = chatDao.insertMessage(
            ChatMessage(
                sessionId = sessionId,
                sender = "user",
                text = userText,
                filePath = filePath,
                fileMimeType = fileMimeType,
                hardwareInfo = hardwareInfo
            )
        )

        // 2. Core historical sequence query
        val historyMsg = chatDao.getMessagesForSession(sessionId).first().toMutableList()

        // 3. Build contents payload for Gemini API
        val apiContents = mutableListOf<Content>()
        
        historyMsg.forEach { msg ->
            val parts = mutableListOf<Part>()
            parts.add(Part(text = msg.text))
            
            if (msg.filePath != null && msg.fileMimeType != null) {
                val mediaBase64 = getFileBase64(context, msg.filePath)
                if (mediaBase64 != null) {
                    parts.add(Part(inlineData = InlineData(mimeType = msg.fileMimeType, data = mediaBase64)))
                }
            }

            if (msg.hardwareInfo != null && msg.sender == "user") {
                parts.add(Part(text = "[Hardware Sensor Context Snapshot at Prompt Time:\n${msg.hardwareInfo}\n]"))
            }

            apiContents.add(
                Content(
                    role = if (msg.sender == "user") "user" else "model",
                    parts = parts
                )
            )
        }

        // 4. Set prompt generation parameters
        val config = GenerationConfig(
            temperature = session.temperature,
            maxOutputTokens = session.maxTokens,
            topP = session.topP,
            topK = session.topK
        )

        val customToolsList = try { chatDao.getAllTools().first() } catch(e: Exception) { emptyList() }
        val customSettingsList = try { chatDao.getAllDynamicSettings().first() } catch(e: Exception) { emptyList() }

        val toolsContextText = if (customToolsList.isNotEmpty()) {
            "\n[EXHAUSTIVE USER/AGENT DYNAMIC TOOLS LIBRARY AVAILABLE]\n" +
            customToolsList.joinToString("\n") { tool ->
                "- TOOL: ${tool.name} (${tool.targetCategory})\n  Desc: ${tool.description}\n  Inputs: ${tool.inputSchemaJson}\n  Specification Log:\n  ${tool.codeOrSpecification}"
            } + "\nIf any of these custom tools match your needs, execute them conceptually and report back to the user."
        } else ""

        val settingsContextText = if (customSettingsList.isNotEmpty()) {
            "\n[ACTIVE DEPLOYED DYNAMIC USER SETTINGS CANVAS (CURRENT STATES)]\n" +
            customSettingsList.joinToString("\n") { setting ->
                "- SETTING: \"${setting.title}\" (${setting.type.uppercase()}) = CURRENT_VALUE: \"${setting.currentValue}\"\n  Conceptual/Target Binding: ${setting.targetActionBinding}\n  Purpose: ${setting.description}"
            } + "\nEnsure you align your model personality, creative style thresholds, or simulated environment modifiers to match these current states!"
        } else ""

        val directActionsInstruction = """
            
            [HIGH-LEVERAGE CAPABILITY: DIRECT NATIVE SYSTEM ACTIONS]
            You are operating natively inside an Android Sandbox container. You can trigger real hardware behaviors, file creations, settings configuration additions, intent triggers, and tools compilations directly on the user's phone by inserting standard structural local_action tags in your markdown output.
            When you decide to execute a command, you MUST output one of the following exact tagged elements (you can combine multiple or suggest updates):
            
            1. WRITE LOCAL FILE in Workspace:
            <local_action type="write_file" path="[relative_filename.txt]">[content_inside_file]</local_action>
            Example: <local_action type="write_file" path="report.txt">Generated on device. Storage free: ok</local_action>
            
            2. DELETE LOCAL WORKSPACE FILE:
            <local_action type="delete_file" path="[relative_filename.txt]"/>

            3. SYNTHESIZE TONAL SOUNDS:
            <local_action type="synth_tone" frequency="[frequency_hz]" duration="[duration_ms]"/>
            Example: <local_action type="synth_tone" frequency="550" duration="250"/>
            
            4. TRIGGER NATIVE SYSTEM NOTIFICATION:
            <local_action type="notification" title="[Title]" text="[Message]"/>
            Example: <local_action type="notification" title="Sandbox Build Alert" text="Task completed."/>

            5. LAUNCH OTHER APPLICATIONS / IMPLICIT INTENTS:
            Use this to launch specific app packages or intent URLs (e.g. settings, web, contacts):
            <local_action type="launch_app" package="[package_id]" url="[url_if_any]"/>
            Example (Launch Google): <local_action type="launch_app" url="https://google.com"/>
            Example (Launch Settings): <local_action type="launch_app" package="com.android.settings"/>

            6. COMPILE & REGISTER NEW CUSTOM DYNAMIC TOOL:
            Agents can dynamically co-create tools they need to solve complex tasks. Registering tools adds them directly to the exhaustive library where all other agents and users can instantly access them in any combination:
            <local_action type="register_tool" name="[toolName]" category="[category]" description="[description_of_tool_purpose]" schema="[json_input_schema_parameters]">[tool_procedural_code_or_detailed_rules]</local_action>
            Example:
            <local_action type="register_tool" name="BluetoothDeviceScannerSimulation" category="Hardware" description="Simulatively checks signal strengths" schema="{\"filter_name\": \"string\"}">Read battery packets and estimate beacon strengths based on dB loss.</local_action>

            7. REGISTER CONVERSATIONALLY NEGOTIATED UI SETTING:
            If you have successfully completed a multi-round dialogue with the user proposing a custom UI setting controller and they approved/deployed it, register it dynamically to the live Canvas using:
            <local_action type="register_setting" title="[title]" controlType="[slider|switch|text]" rangeStart="[min_val_if_slider]" rangeEnd="[max_val_if_slider]" default="[default_value_string]" binding="[conceptual_action_binding]">[setting_purpose_instructions]</local_action>
            Once ran, it instantly appears as a live, functional, and draggable control rendering in the active settings panel!

            8. AGGREGATE SYSTEM MODELS:
            To dynamically add an external or newly requested AI model to the platform aggregated catalogs, use:
            <local_action type="register_model" modelName="[modelName]" provider="[provider]" endpoint="[apiEndpoint_or_domain_url]">[model_description_purpose]</local_action>

            9. SYSTEM CONFIGURATION SIMULATION TUNER:
            To modify simulated device status parameters:
            <local_action type="modify_system_setting" name="[sensor_metric_or_adaptive_parameter]" value="[new_value]"/>

            Remember: Your system actions are automatically parsed and instantly run on the user's local operating system. Give the user confirmation when executing them. Keep responses professional, helpful, and technically detailed.
            $toolsContextText
            $settingsContextText
        """.trimIndent()

        val fullSystemInstruction = session.systemInstructions + directActionsInstruction

        val systemContent = Content(
            parts = listOf(Part(text = fullSystemInstruction))
        )

        val request = GenerateContentRequest(
            contents = apiContents,
            generationConfig = config,
            systemInstruction = systemContent
        )

        try {
            // Call API via dynamic endpoint model selection
            val rawResponse = RetrofitClient.service.generateContent(
                model = session.modelName,
                apiKey = apiKey,
                request = request
            )

            val rawReplyText = rawResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No response candidate produced by model ${session.modelName}."

            // 5. Run local action executions
            val actionStatuses = mutableListOf<String>()
            val processedReplyText = LocalActionExecutor.executeActions(context, rawReplyText) { status ->
                actionStatuses.add(status)
            }

            val finalActionStatus = if (actionStatuses.isNotEmpty()) {
                actionStatuses.joinToString(separator = ", ")
            } else {
                null
            }

            // 6. Censor-cache assistant response message
            val savedReply = ChatMessage(
                sessionId = sessionId,
                sender = "model",
                text = processedReplyText,
                executedAction = finalActionStatus
            )
            chatDao.insertMessage(savedReply)

            processedReplyText
        } catch (e: Exception) {
            val errorMsg = "API Connection Error: ${e.localizedMessage ?: "Call failed"}. Check your connections or verify your API key in the AI Studio secrets pane."
            val savedReply = ChatMessage(
                sessionId = sessionId,
                sender = "model",
                text = errorMsg
            )
            chatDao.insertMessage(savedReply)
            errorMsg
        }
    }

    private fun getFileBase64(context: Context, uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val byteBuffer = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var len: Int
            while (inputStream.read(buffer).also { len = it } != -1) {
                byteBuffer.write(buffer, 0, len)
            }
            val bytes = byteBuffer.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
