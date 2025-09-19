package com.irlquest.app.data.network

import com.irlquest.app.data.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Authentication
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<UserDto>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("auth/me")
    suspend fun getMe(): Response<UserDto>

    // Quests
    @GET("quests")
    suspend fun getQuests(): Response<List<QuestDto>>

    @POST("quests")
    suspend fun createQuest(@Body quest: CreateQuestRequest): Response<QuestDto>

    @GET("quests/{id}")
    suspend fun getQuest(@Path("id") id: Int): Response<QuestDto>

    @PUT("quests/{id}")
    suspend fun updateQuest(@Path("id") id: Int, @Body quest: UpdateQuestRequest): Response<QuestDto>

    @DELETE("quests/{id}")
    suspend fun deleteQuest(@Path("id") id: Int): Response<Unit>

    @POST("quests/{id}/complete")
    suspend fun completeQuest(@Path("id") id: Int): Response<QuestDto>

    // Tasks
    @GET("tasks")
    suspend fun getTasks(): Response<List<TaskDto>>

    @POST("tasks")
    suspend fun createTask(@Body task: CreateTaskRequest): Response<TaskDto>

    @GET("tasks/{id}")
    suspend fun getTask(@Path("id") id: Int): Response<TaskDto>

    @PUT("tasks/{id}")
    suspend fun updateTask(@Path("id") id: Int, @Body task: UpdateTaskRequest): Response<TaskDto>

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int): Response<Unit>

    @POST("tasks/{id}/complete")
    suspend fun completeTask(@Path("id") id: Int): Response<TaskDto>

    // RAG System - Главная фича для преобразования TODO в квесты
    @POST("rag/generate-quest")
    suspend fun generateQuest(@Body request: QuestGenerationRequest): Response<QuestGenerationResponse>

    @POST("rag/enhance-task") 
    suspend fun enhanceTask(@Body request: TaskEnhancementRequest): Response<TaskEnhancementResponse>

    // User Stats and Achievements
    @GET("users/me/stats")
    suspend fun getUserStats(): Response<UserStatsDto>

    @GET("users/me/achievements")
    suspend fun getUserAchievements(): Response<List<AchievementDto>>
}