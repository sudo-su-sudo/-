package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val temperature: Float = 0.7f,
    val modelName: String = "gemini-3.5-flash",
    val systemInstructions: String = "You are a helpful explorer companion with direct access to physical hardware context and local action executions.",
    val maxTokens: Int = 2048,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val hardwareScannerEnabled: Boolean = false
)

@Entity(tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val sender: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val filePath: String? = null,
    val fileMimeType: String? = null,
    val hardwareInfo: String? = null,
    val executedAction: String? = null
)

@Entity(tableName = "custom_tools")
data class CustomTool(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val codeOrSpecification: String,
    val inputSchemaJson: String,
    val targetCategory: String = "Utility"
)

@Entity(tableName = "dynamic_settings")
data class DynamicSetting(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String, // "slider", "switch", "text"
    val currentValue: String,
    val rangeStart: Float = 0f,
    val rangeEnd: Float = 100f,
    val description: String,
    val targetActionBinding: String
)

@Entity(tableName = "aggregated_models")
data class AggregatedModel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modelName: String,
    val provider: String, // e.g. "Gemini Developer API", "Vertex AI Sandbox", "Custom Gateway"
    val endpointUrl: String,
    val description: String,
    val isCustom: Boolean = true
)

