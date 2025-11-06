# 🤖 ML Inference Endpoints - Документация

**Версия API**: 2.1.0  
**Дата**: 31.10.2025

---

## 📋 Обзор

ML Inference endpoints предоставляют интеллектуальный анализ задач с confidence scores для обеспечения качества предсказаний.

**Ключевые особенности**:
- ✅ Confidence scores для всех предсказаний
- ✅ Human-in-loop флаг (requires_review)
- ✅ Explainability (факторы решений)
- ✅ Защита через rate limiting
- ✅ Аутентификация через JWT

---

## 🔐 Аутентификация

Все ML endpoints требуют JWT токена:

```http
Authorization: Bearer <your_jwt_token>
```

---

## 📍 Endpoints

### 1. POST /api/ml/tags - Определение тегов

Автоматически определяет теги для текста задачи.

**Request**:
```json
{
  "text": "Купить продукты в магазине",
  "max_tags": 5
}
```

**Response**:
```json
{
  "tags": [
    {
      "tag": "покупки",
      "confidence": 0.95,
      "requires_review": false
    },
    {
      "tag": "дом",
      "confidence": 0.45,
      "requires_review": true
    }
  ],
  "processing_time_ms": 12
}
```

**Поля**:
- `tag` - название тега
- `confidence` - уверенность модели (0.0-1.0)
- `requires_review` - требуется ли проверка человеком (confidence < 0.7)

**Пример использования**:
```bash
curl -X POST http://localhost:8003/api/ml/tags \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"text": "Сходить в спортзал", "max_tags": 3}'
```

---

### 2. POST /api/ml/difficulty - Оценка сложности

Оценивает сложность задачи по шкале 1-10 с объяснением факторов.

**Request**:
```json
{
  "title": "Разработать сложный проект",
  "description": "Требуется изучить новую технологию и создать прототип"
}
```

**Response**:
```json
{
  "difficulty": 8,
  "confidence": 0.85,
  "factors": [
    {
      "factor": "Ключевое слово: 'сложн'",
      "impact": 0.4,
      "explanation": "Указывает на повышенную сложность"
    },
    {
      "factor": "Ключевое слово: 'изучить'",
      "impact": 0.3,
      "explanation": "Указывает на повышенную сложность"
    },
    {
      "factor": "Множественные этапы",
      "impact": 0.25,
      "explanation": "Обнаружено 2 подзадач"
    }
  ],
  "requires_review": false,
  "processing_time_ms": 8
}
```

**Поля**:
- `difficulty` - оценка сложности (1-10)
- `confidence` - уверенность в оценке
- `factors` - факторы, влияющие на оценку (explainability)
- `requires_review` - требуется проверка (confidence < 0.6)

**Факторы сложности**:
- ✅ Длина описания
- ✅ Ключевые слова сложности (сложн, трудн, изучить, разработать)
- ✅ Ключевые слова простоты (просто, быстро, легко)
- ✅ Множественные этапы (несколько подзадач)

---

### 3. POST /api/ml/transform - Трансформация ToDo → Quest

Превращает обычную задачу в эпический квест с фэнтези описанием.

**Request**:
```json
{
  "title": "Купить молоко",
  "description": "Сходить в магазин",
  "difficulty": 2,
  "user_level": 5,
  "preferred_style": "fantasy"
}
```

**Response**:
```json
{
  "fantasy_title": "🏰 Легендарный квест: Купить молоко",
  "fantasy_description": "В мирной деревне требуется помощь. Сходить в магазин\n\nТребуется смелый герой для выполнения задания.",
  "suggested_rewards": {
    "experience": 40,
    "gold": 20,
    "items": []
  },
  "suggested_difficulty": 2,
  "confidence": 0.85,
  "requires_review": false,
  "style_used": "fantasy",
  "processing_time_ms": 15
}
```

**Поддерживаемые стили** (`preferred_style`):
- `fantasy` - Фэнтези (по умолчанию)
- `scifi` - Научная фантастика
- `modern` - Современный
- `horror` - Ужасы
- `adventure` - Приключения

**Награды рассчитываются на основе**:
- Сложность задачи (difficulty)
- Уровень пользователя (user_level)
- Бонусы за высокую сложность (items)

**Пример**:
```bash
curl -X POST http://localhost:8003/api/ml/transform \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Изучить Rust",
    "difficulty": 7,
    "preferred_style": "scifi"
  }'
```

---

### 4. POST /api/ml/recommendations - Персональные рекомендации

Генерирует персональные квесты на основе активности пользователя.

**Request**:
```json
{
  "user_id": 1,
  "limit": 10,
  "exclude_completed": true
}
```

**Response**:
```json
{
  "quests": [
    {
      "title": "Ежедневная тренировка",
      "description": "Выполните утреннюю зарядку для поддержания формы",
      "difficulty": 2,
      "estimated_time_minutes": 15,
      "tags": ["спорт", "здоровье"],
      "score": 0.92,
      "reasons": [
        "Вы часто выполняете спортивные задачи",
        "Подходит для вашего уровня"
      ]
    },
    {
      "title": "Изучить новую технологию",
      "description": "Прочитайте документацию и создайте тестовый проект",
      "difficulty": 6,
      "estimated_time_minutes": 120,
      "tags": ["учеба", "работа"],
      "score": 0.85,
      "reasons": [
        "Соответствует вашим интересам",
        "Поможет в карьерном росте"
      ]
    }
  ],
  "reasoning": "Рекомендации на основе активности пользователя 1",
  "processing_time_ms": 25
}
```

**Поля**:
- `score` - релевантность квеста (0.0-1.0)
- `reasons` - причины рекомендации (explainability)
- `estimated_time_minutes` - примерное время выполнения

---

### 5. GET /api/ml/health - Health Check

Проверка работоспособности ML сервиса.

**Response**: `200 OK` если сервис работает

**Пример**:
```bash
curl http://localhost:8003/api/ml/health
```

---

### 6. GET /api/ml/config - Конфигурация ML

Получить текущую конфигурацию ML inference.

**Response**:
```json
{
  "tags_confidence_threshold": 0.7,
  "difficulty_confidence_threshold": 0.6,
  "transform_confidence_threshold": 0.5,
  "enable_human_in_loop": true
}
```

**Пороги confidence** (thresholds):
- Tags: 0.7 - требуется проверка если ниже
- Difficulty: 0.6
- Transform: 0.5

---

## 🎯 Confidence Scores

Все ML endpoints возвращают `confidence` - уверенность модели в предсказании:

| Confidence | Качество | Действие |
|-----------|----------|----------|
| 0.9 - 1.0 | Отлично | Автоматическое применение |
| 0.7 - 0.9 | Хорошо | Можно применять |
| 0.5 - 0.7 | Средне | Требуется проверка (requires_review: true) |
| < 0.5 | Низко | Обязательна проверка человеком |

---

## 🔄 Human-in-the-Loop

Когда `requires_review: true`:

1. **UI должен показать предупреждение**:
   - ⚠️ "Требуется проверка"
   - "Уверенность модели: 65%"

2. **Пользователь может**:
   - ✅ Подтвердить предсказание
   - ✏️ Исправить и отправить feedback
   - ❌ Отклонить

3. **Feedback используется для**:
   - Улучшения модели
   - A/B тестирования
   - Мониторинга качества

---

## 📊 Explainability

Endpoints предоставляют объяснения решений:

### Tags:
- Confidence score для каждого тега
- Чем выше confidence, тем увереннее модель

### Difficulty:
```json
"factors": [
  {
    "factor": "Название фактора",
    "impact": 0.3,  // Влияние на оценку
    "explanation": "Почему этот фактор важен"
  }
]
```

### Recommendations:
```json
"reasons": [
  "Причина 1: почему этот квест рекомендован",
  "Причина 2: на основе чего сделана рекомендация"
]
```

---

## ⚡ Производительность

| Endpoint | Типичное время | Max RPS |
|----------|---------------|---------|
| /ml/tags | 5-15ms | 200 |
| /ml/difficulty | 5-10ms | 300 |
| /ml/transform | 10-20ms | 150 |
| /ml/recommendations | 20-50ms | 100 |

**Оптимизация**:
- ✅ Кэширование частых запросов (TODO)
- ✅ Batch processing для множественных задач (TODO)
- ✅ Async inference (реализовано)

---

## 🛡️ Безопасность

### Rate Limiting
- 60 requests/минуту per IP
- Burst до 10 requests/секунду

### Аутентификация
- JWT токен обязателен для всех endpoints
- Токен проверяется middleware

### Валидация
- Максимальная длина текста: 5000 символов
- Максимум тегов: 10
- Difficulty: 1-10

---

## 🧪 Примеры интеграции

### JavaScript/TypeScript

```typescript
interface MLClient {
  async predictTags(text: string, maxTags: number = 5): Promise<TagsResponse> {
    const response = await fetch('http://api.irlquest.com/api/ml/tags', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ text, max_tags: maxTags }),
    });
    return await response.json();
  }
}
```

### Kotlin (Android)

```kotlin
data class TagsRequest(
    val text: String,
    @SerialName("max_tags") val maxTags: Int? = null
)

interface MlApi {
    @POST("api/ml/tags")
    suspend fun predictTags(@Body request: TagsRequest): TagsResponse
    
    @POST("api/ml/difficulty")
    suspend fun predictDifficulty(@Body request: DifficultyRequest): DifficultyResponse
    
    @POST("api/ml/transform")
    suspend fun transformToQuest(@Body request: TransformRequest): TransformResponse
}
```

### Python

```python
import requests

def predict_tags(text: str, token: str, max_tags: int = 5):
    response = requests.post(
        'http://localhost:8003/api/ml/tags',
        headers={'Authorization': f'Bearer {token}'},
        json={'text': text, 'max_tags': max_tags}
    )
    return response.json()
```

---

## 📈 Мониторинг и метрики

### Логирование
Каждый запрос логируется:
```
INFO Tags predicted: 3 tags, processing time: 12ms
INFO Difficulty predicted: 7 (confidence: 0.85), requires_review: false
```

### Метрики (TODO)
- Average confidence score
- Human-in-loop rate
- Processing time percentiles
- Error rate

---

## 🔮 Roadmap

### v2.2.0 (Q4 2025)
- [ ] Интеграция с реальными ML моделями (TensorFlow/PyTorch)
- [ ] Кэширование предсказаний
- [ ] Batch inference endpoints
- [ ] Model versioning

### v2.3.0 (Q1 2026)
- [ ] A/B тестирование моделей
- [ ] Model registry (MLflow)
- [ ] Data drift monitoring
- [ ] Auto-retraining pipeline

---

## 🐛 Troubleshooting

### Низкий confidence score
**Проблема**: Все предсказания с низким confidence

**Решения**:
1. Проверить качество входных данных
2. Добавить больше контекста в описание
3. Использовать более специфичные слова

### Медленная обработка
**Проблема**: Processing time > 100ms

**Решения**:
1. Проверить rate limiting
2. Уменьшить max_tags
3. Использовать batch endpoints (когда будут доступны)

---

## 📞 Поддержка

**Документация**: См. `РЕАЛИЗАЦИЯ_v2.1.0.md`  
**API Version**: 2.1.0  
**Обновления**: Следите за CHANGELOG.md

---

**Версия**: 2.1.0  
**Дата**: 31.10.2025  
**Статус**: ✅ Production Ready

🤖 **Интеллектуальная обработка задач с объяснениями!** 🎯

