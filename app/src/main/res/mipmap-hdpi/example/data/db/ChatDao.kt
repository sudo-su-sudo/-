package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): ChatSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    @Update
    suspend fun updateSession(session: ChatSession)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Long)

    // --- Custom Agentic Tools ---
    @Query("SELECT * FROM custom_tools ORDER BY id DESC")
    fun getAllTools(): Flow<List<CustomTool>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTool(tool: CustomTool): Long

    @Query("DELETE FROM custom_tools WHERE id = :toolId")
    suspend fun deleteToolById(toolId: Long)

    // --- Dynamic User Settings ---
    @Query("SELECT * FROM dynamic_settings ORDER BY id ASC")
    fun getAllDynamicSettings(): Flow<List<DynamicSetting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDynamicSetting(setting: DynamicSetting): Long

    @Update
    suspend fun updateDynamicSetting(setting: DynamicSetting)

    @Query("DELETE FROM dynamic_settings WHERE id = :settingId")
    suspend fun deleteDynamicSettingById(settingId: Long)

    // --- Aggregated Custom Models ---
    @Query("SELECT * FROM aggregated_models ORDER BY id ASC")
    fun getAllAggregatedModels(): Flow<List<AggregatedModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAggregatedModel(model: AggregatedModel): Long

    @Query("DELETE FROM aggregated_models WHERE id = :modelId")
    suspend fun deleteAggregatedModel(modelId: Long)
}
