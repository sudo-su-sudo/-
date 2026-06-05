package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.ChatMessage
import com.example.data.db.ChatSession
import com.example.data.repository.ChatRepository
import com.example.util.HardwareScanner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ChatRepository(application, database.chatDao())
    private val hardwareScanner = HardwareScanner(application)

    val sessions: StateFlow<List<ChatSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSession: StateFlow<ChatSession?> = _currentSessionId
        .flatMapLatest { id ->
            if (id == null) flowOf<ChatSession?>(null)
            else flow {
                emit(repository.getSessionById(id))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> = _currentSessionId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getMessagesForSession(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _promptInput = MutableStateFlow("")
    val promptInput: StateFlow<String> = _promptInput.asStateFlow()

    private val _selectedAttachmentUri = MutableStateFlow<String?>(null)
    val selectedAttachmentUri: StateFlow<String?> = _selectedAttachmentUri.asStateFlow()

    private val _selectedAttachmentMimeType = MutableStateFlow<String?>(null)
    val selectedAttachmentMimeType: StateFlow<String?> = _selectedAttachmentMimeType.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _actionLogs = MutableStateFlow<List<String>>(emptyList())
    val actionLogs: StateFlow<List<String>> = _actionLogs.asStateFlow()

    // --- Dynamic Elements Data Pipelines ---
    val allTools: StateFlow<List<com.example.data.db.CustomTool>> = database.chatDao().getAllTools()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDynamicSettings: StateFlow<List<com.example.data.db.DynamicSetting>> = database.chatDao().getAllDynamicSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAggregatedModels: StateFlow<List<com.example.data.db.AggregatedModel>> = database.chatDao().getAllAggregatedModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Conversational Settings State Machine ---
    data class SettingConversationalState(
        val currentStep: Int = 0, // 0: Idle, 1: Proposing design, 2: Finalizing, 3: Completed
        val lastModelReply: String = "",
        val proposedTitle: String = "",
        val proposedType: String = "", // switch, slider, text
        val proposedMin: Float = 0f,
        val proposedMax: Float = 100f,
        val proposedDefault: String = "",
        val proposedBinding: String = "",
        val proposedDescription: String = ""
    )

    private val _settingConversationState = MutableStateFlow(SettingConversationalState())
    val settingConversationState: StateFlow<SettingConversationalState> = _settingConversationState.asStateFlow()

    init {
        // Automatically select or create first session
        viewModelScope.launch {
            sessions.collect { list ->
                if (_currentSessionId.value == null && list.isNotEmpty()) {
                    _currentSessionId.value = list.first().id
                }
            }
        }
        
        // Let the scanner register on init
        hardwareScanner.registerSensors()
    }

    override fun onCleared() {
        super.onCleared()
        hardwareScanner.unregisterSensors()
    }

    fun selectSession(sessionId: Long) {
        _currentSessionId.value = sessionId
    }

    fun startNewSession() {
        viewModelScope.launch {
            val sessionCount = sessions.value.size
            val newSession = ChatSession(
                title = "Workspace Session #${sessionCount + 1}"
            )
            val id = repository.insertSession(newSession)
            _currentSessionId.value = id
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = sessions.value.firstOrNull { it.id != sessionId }?.id
            }
        }
    }

    fun updateSessionParameters(
        modelName: String,
        temperature: Float,
        systemInstructions: String,
        maxTokens: Int,
        topP: Float,
        topK: Int,
        hardwareScannerEnabled: Boolean
    ) {
        val current = currentSession.value ?: return
        viewModelScope.launch {
            val updated = current.copy(
                modelName = modelName,
                temperature = temperature,
                systemInstructions = systemInstructions,
                maxTokens = maxTokens,
                topP = topP,
                topK = topK,
                hardwareScannerEnabled = hardwareScannerEnabled
            )
            repository.updateSession(updated)
        }
    }

    fun updateSessionTitle(newTitle: String) {
        val current = currentSession.value ?: return
        viewModelScope.launch {
            repository.updateSession(current.copy(title = newTitle))
        }
    }

    fun setPromptInput(text: String) {
        _promptInput.value = text
    }

    fun setAttachment(uriString: String?, mimeType: String?) {
        _selectedAttachmentUri.value = uriString
        _selectedAttachmentMimeType.value = mimeType
    }

    fun clearAttachment() {
        _selectedAttachmentUri.value = null
        _selectedAttachmentMimeType.value = null
    }

    fun handleSharedData(text: String?, uriString: String?, mimeType: String?) {
        viewModelScope.launch {
            // Ensure we have a valid session to receive shared data
            if (_currentSessionId.value == null) {
                val list = sessions.value
                if (list.isNotEmpty()) {
                    _currentSessionId.value = list.first().id
                } else {
                    val id = repository.insertSession(ChatSession(title = "Shared Intake Payload"))
                    _currentSessionId.value = id
                }
            }
            
            if (text != null) {
                val initial = _promptInput.value
                _promptInput.value = if (initial.isEmpty()) text else "$initial\n$text"
            }
            if (uriString != null) {
                _selectedAttachmentUri.value = uriString
                _selectedAttachmentMimeType.value = mimeType ?: "application/octet-stream"
            }
        }
    }

    fun scanHardwareSnapshotNow(): String {
        return hardwareScanner.getScannerSummary()
    }

    fun sendMessage() {
        val sessionId = _currentSessionId.value ?: return
        val text = _promptInput.value.trim()
        val filePath = _selectedAttachmentUri.value
        val fileMimeType = _selectedAttachmentMimeType.value

        if (text.isEmpty() && filePath == null) return

        _promptInput.value = ""
        clearAttachment()
        _isThinking.value = true

        viewModelScope.launch {
            val hardwareSnapshot = if (currentSession.value?.hardwareScannerEnabled == true) {
                hardwareScanner.getScannerSummary()
            } else {
                null
            }

            repository.sendMessageAndGetReply(
                sessionId = sessionId,
                userText = text,
                filePath = filePath,
                fileMimeType = fileMimeType,
                hardwareInfo = hardwareSnapshot
            )

            // Gather and display logs of run actions
            val lastMsg = repository.getMessagesForSession(sessionId).first().lastOrNull { it.sender == "model" }
            lastMsg?.executedAction?.let { actionLog ->
                val updatedLogs = _actionLogs.value.toMutableList()
                updatedLogs.add(0, "[Sandbox Local Action Result]: $actionLog")
                _actionLogs.value = updatedLogs
            }

            _isThinking.value = false
        }
    }

    // --- Dynamic Settings / Custom Tools / Models Actions ---
    fun insertTool(tool: com.example.data.db.CustomTool) {
        viewModelScope.launch {
            database.chatDao().insertTool(tool)
        }
    }

    fun deleteTool(toolId: Long) {
        viewModelScope.launch {
            database.chatDao().deleteToolById(toolId)
        }
    }

    fun insertDynamicSetting(setting: com.example.data.db.DynamicSetting) {
        viewModelScope.launch {
            database.chatDao().insertDynamicSetting(setting)
        }
    }

    fun updateDynamicSettingValue(setting: com.example.data.db.DynamicSetting, value: String) {
        viewModelScope.launch {
            database.chatDao().updateDynamicSetting(setting.copy(currentValue = value))
        }
    }

    fun deleteDynamicSetting(settingId: Long) {
        viewModelScope.launch {
            database.chatDao().deleteDynamicSettingById(settingId)
        }
    }

    fun insertAggregatedModel(model: com.example.data.db.AggregatedModel) {
        viewModelScope.launch {
            database.chatDao().insertAggregatedModel(model)
        }
    }

    fun deleteAggregatedModel(modelId: Long) {
        viewModelScope.launch {
            database.chatDao().deleteAggregatedModel(modelId)
        }
    }

    // --- Conversational Multi-round Builder Dialog System ---
    fun submitCustomSettingRequest(request: String) {
        val cleaned = request.trim()
        if (cleaned.isEmpty()) return
        val currentState = _settingConversationState.value

        viewModelScope.launch {
            if (currentState.currentStep == 0) {
                // Round 1: Initiate proposal
                val parsedTitle = cleaned.split(" ").take(3).joinToString(" ").replaceFirstChar { it.uppercase() }
                val type = when {
                    cleaned.lowercase().contains("slider") || cleaned.lowercase().contains("range") || cleaned.lowercase().contains("level") -> "slider"
                    cleaned.lowercase().contains("text") || cleaned.lowercase().contains("string") -> "text"
                    else -> "switch"
                }
                val defValue = if (type == "slider") "50" else if (type == "switch") "false" else "Configuration Value"

                _settingConversationState.value = SettingConversationalState(
                    currentStep = 1,
                    proposedTitle = parsedTitle,
                    proposedType = type,
                    proposedMin = 0f,
                    proposedMax = 100f,
                    proposedDefault = defValue,
                    proposedBinding = "Dynamic Context Filter for $parsedTitle",
                    proposedDescription = cleaned,
                    lastModelReply = "I have proposed an active $type labeled \"$parsedTitle\" to control this behavior. (Conceptual target: $cleaned).\nWould you like to refine the title or change the configuration (e.g. modify slider boundaries, or change it to a switch)?"
                )
            } else if (currentState.currentStep == 1) {
                // Round 2: Refine configuration values
                var updatedType = currentState.proposedType
                var updatedMin = currentState.proposedMin
                var updatedMax = currentState.proposedMax
                var updatedTitle = currentState.proposedTitle

                if (cleaned.lowercase().contains("slider")) updatedType = "slider"
                if (cleaned.lowercase().contains("switch")) updatedType = "switch"
                if (cleaned.lowercase().contains("text")) updatedType = "text"

                // Fetch numeric values if provided
                val digits = "\\d+".toRegex().findAll(cleaned).map { it.value.toFloatOrNull() ?: 0f }.toList()
                if (digits.size >= 2) {
                    updatedMin = digits[0]
                    updatedMax = digits[1]
                } else if (digits.size == 1) {
                    updatedMax = digits[0]
                }

                val defValue = if (updatedType == "slider") "${(updatedMin + updatedMax) / 2}" else if (updatedType == "switch") "false" else "Custom Setting Entry"

                _settingConversationState.value = currentState.copy(
                    currentStep = 2,
                    proposedType = updatedType,
                    proposedMin = updatedMin,
                    proposedMax = updatedMax,
                    proposedDefault = defValue,
                    lastModelReply = "Negotiation Round 2 Complete!\nDesign schema refined: [TYPE: $updatedType] | [TITLE: \"$updatedTitle\"] | [RANGE: $updatedMin to $updatedMax]\nConceptual bind action set to: \"${currentState.proposedBinding}\". Shall we proceed to instantly compile and render this controller on your Settings space?"
                )
            }
        }
    }

    fun confirmAndDeploySetting() {
        val currentState = _settingConversationState.value
        if (currentState.currentStep >= 1) {
            viewModelScope.launch {
                insertDynamicSetting(
                    com.example.data.db.DynamicSetting(
                        title = currentState.proposedTitle,
                        type = currentState.proposedType,
                        currentValue = currentState.proposedDefault,
                        rangeStart = currentState.proposedMin,
                        rangeEnd = currentState.proposedMax,
                        description = currentState.proposedDescription,
                        targetActionBinding = currentState.proposedBinding
                    )
                )
                _settingConversationState.value = SettingConversationalState(
                    currentStep = 3,
                    lastModelReply = "Round 3 Success! Custom control \"${currentState.proposedTitle}\" compiles! It has been successfully loaded into the database and rendered live below."
                )
            }
        }
    }

    fun cancelSettingNegotiation() {
        _settingConversationState.value = SettingConversationalState()
    }
}
