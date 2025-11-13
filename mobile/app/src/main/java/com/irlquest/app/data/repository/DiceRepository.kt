package com.irlquest.app.data.repository

import com.irlquest.app.data.SharedRepositoryProvider
import com.irlquest.app.data.network.dto.DiceTypeInfo
import com.irlquest.app.data.network.dto.MultiRollRequest
import com.irlquest.app.data.network.dto.MultiRollResult
import com.irlquest.app.data.network.dto.RollDiceRequest
import com.irlquest.app.data.network.dto.RollDiceResponse
import com.irlquest.app.data.network.dto.SkillCheckRequest
import com.irlquest.app.data.network.dto.SkillCheckResult
import com.irlquest.app.data.network.dto.SkillInfo

class DiceRepository : BaseKmpRepository() {
    private val apiClient = SharedRepositoryProvider.apiClient

    suspend fun rollDice(diceType: String, modifier: Int? = null): RollDiceResponse {
        val token = requireToken()
        val request = RollDiceRequest(diceType = diceType, modifier = modifier)
        return apiClient.postWithBody(
            path = "/dice/roll",
            body = request,
            headers = authHeaders(token)
        )
    }

    suspend fun rollMultiDice(diceType: String, count: Int, modifier: Int? = null): MultiRollResult {
        val token = requireToken()
        val request = MultiRollRequest(diceType = diceType, count = count, modifier = modifier)
        return apiClient.postWithBody(
            path = "/dice/roll/multi",
            body = request,
            headers = authHeaders(token)
        )
    }

    suspend fun skillCheck(skill: String, difficulty: Int): SkillCheckResult {
        val token = requireToken()
        val request = SkillCheckRequest(skill = skill, difficulty = difficulty)
        return apiClient.postWithBody(
            path = "/dice/skill-check",
            body = request,
            headers = authHeaders(token)
        )
    }

    suspend fun getDiceTypes(): List<DiceTypeInfo> {
        val token = currentToken()
        return apiClient.get(
            path = "/dice/types",
            headers = authHeaders(token)
        )
    }

    suspend fun getSkills(): List<SkillInfo> {
        val token = currentToken()
        return apiClient.get(
            path = "/dice/skills",
            headers = authHeaders(token)
        )
    }
}


