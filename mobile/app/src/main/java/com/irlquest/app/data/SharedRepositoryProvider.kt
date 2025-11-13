package com.irlquest.app.data

import com.irlquest.shared.SharedFactory
import com.irlquest.app.BuildConfig

/**
 * Провайдер для shared репозиториев
 * Использует Kotlin Multiplatform shared модуль
 */
object SharedRepositoryProvider {
    private val sharedFactory = SharedFactory(
        baseUrl = BuildConfig.API_BASE_URL.removeSuffix("/api/v1/").removeSuffix("/")
    )
    
    val apiClient = sharedFactory.apiClient
    
    val authRepository = sharedFactory.authRepository
    val questRepository = sharedFactory.questRepository
    val taskRepository = sharedFactory.taskRepository
    val statsRepository = sharedFactory.statsRepository
    val characterRepository = sharedFactory.characterRepository
    val multiplayerRepository = sharedFactory.multiplayerRepository
    val geolocationRepository = sharedFactory.geolocationRepository
    val auctionRepository = sharedFactory.auctionRepository
    val mlRepository = sharedFactory.mlRepository
}

