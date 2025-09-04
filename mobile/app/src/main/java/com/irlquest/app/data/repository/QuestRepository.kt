MVPpackage com.irlquest.app.data.repository

import com.irlquest.app.data.network.RetrofitClient
import com.irlquest.app.data.network.dto.QuestCreateRequest
import com.irlquest.app.data.network.dto.QuestDto

class QuestRepository {
    private val api = RetrofitClient.apiService

    suspend fun listQuests(): List<QuestDto> {
        return api.listQuests()
    }

    suspend fun createQuest(title: String, description: String?, difficulty: Int = 1): QuestDto {
        return api.createQuest(QuestCreateRequest(title = title, description = description, difficulty = difficulty))
    }
}

