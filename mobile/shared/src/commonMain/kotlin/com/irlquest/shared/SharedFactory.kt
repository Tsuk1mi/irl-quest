package com.irlquest.shared

import com.irlquest.shared.network.ApiClient
import com.irlquest.shared.network.createHttpClient
import com.irlquest.shared.repository.*

/**
 * Factory для создания репозиториев в shared модуле
 */
class SharedFactory(
    baseUrl: String
) {
    private val internalApiClient = ApiClient(baseUrl, createHttpClient())
    
    val apiClient: ApiClient get() = internalApiClient
    
    val authRepository: AuthRepository by lazy { AuthRepository(internalApiClient) }
    val questRepository: QuestRepository by lazy { QuestRepository(internalApiClient) }
    val taskRepository: TaskRepository by lazy { TaskRepository(internalApiClient) }
    val statsRepository: StatsRepository by lazy { StatsRepository(internalApiClient) }
    val characterRepository: CharacterRepository by lazy { CharacterRepository(internalApiClient) }
    val multiplayerRepository: MultiplayerRepository by lazy { MultiplayerRepository(internalApiClient) }
    val geolocationRepository: GeolocationRepository by lazy { GeolocationRepository(internalApiClient) }
    val auctionRepository: AuctionRepository by lazy { AuctionRepository(internalApiClient) }
    val mlRepository: MLRepository by lazy { MLRepository(internalApiClient) }
}

