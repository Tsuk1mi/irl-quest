package com.irlquest.app.data.network

import com.irlquest.app.data.network.dto.*
import retrofit2.http.*

interface ApiService {
    @POST("/api/v1/auth/register")
    suspend fun register(@Body req: UserCreateRequest): UserOutResponse

    @FormUrlEncoded
    @POST("/api/v1/auth/token")
    suspend fun token(@Field("username") username: String, @Field("password") password: String): TokenResponse

    @GET("/api/v1/auth/me")
    suspend fun me(): UserOutResponse

    @PUT("/api/v1/users/me")
    suspend fun updateMe(@Body req: UserUpdateRequest): UserOutResponse

    @GET("/api/v1/tasks/")
    suspend fun listTasks(): List<TaskDto>

    @POST("/api/v1/tasks/")
    suspend fun createTask(@Body req: TaskCreateRequest): TaskDto

    @PUT("/api/v1/tasks/{id}")
    suspend fun updateTask(@Path("id") id: Int, @Body req: TaskUpdateRequest): TaskDto

    @DELETE("/api/v1/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int)

    @GET("/api/v1/quests/")
    suspend fun listQuests(): List<QuestDto>

    @POST("/api/v1/quests/")
    suspend fun createQuest(@Body req: QuestCreateRequest): QuestDto
}
