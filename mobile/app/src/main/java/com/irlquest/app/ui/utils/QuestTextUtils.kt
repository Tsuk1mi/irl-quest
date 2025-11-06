package com.irlquest.app.ui.utils

/**
 * 📜 УТИЛИТЫ ДЛЯ ФЭНТЕЗИ-ТЕКСТОВ
 * 
 * Превращает обычные задачи в эпические квесты!
 */

/**
 * Превращает обычное название задачи в эпический квест
 */
fun String.toQuestTitle(): String {
    val lowerTitle = this.lowercase()
    
    return when {
        // Работа и карьера
        lowerTitle.contains("работ") || lowerTitle.contains("проект") -> 
            "⚔️ $this"
        lowerTitle.contains("встреч") || lowerTitle.contains("созвон") -> 
            "🤝 $this"
        lowerTitle.contains("отчёт") || lowerTitle.contains("отчет") || lowerTitle.contains("документ") -> 
            "📋 $this"
            
        // Обучение
        lowerTitle.contains("учи") || lowerTitle.contains("изучи") || lowerTitle.contains("прочита") -> 
            "📚 $this"
        lowerTitle.contains("курс") || lowerTitle.contains("урок") -> 
            "🎓 $this"
            
        // Дом и быт
        lowerTitle.contains("убор") || lowerTitle.contains("чист") -> 
            "🧹 $this"
        lowerTitle.contains("магазин") || lowerTitle.contains("купи") || lowerTitle.contains("продукт") -> 
            "🛒 $this"
        lowerTitle.contains("готов") || lowerTitle.contains("пригото") -> 
            "🍳 $this"
            
        // Спорт и здоровье
        lowerTitle.contains("трениро") || lowerTitle.contains("спорт") || lowerTitle.contains("бег") -> 
            "💪 $this"
        lowerTitle.contains("врач") || lowerTitle.contains("доктор") || lowerTitle.contains("здоров") -> 
            "🏥 $this"
            
        // Творчество
        lowerTitle.contains("рисо") || lowerTitle.contains("написа") || lowerTitle.contains("созда") -> 
            "🎨 $this"
            
        else -> "📍 $this"
    }
}

/**
 * Добавляет фэнтези-описание к задаче
 */
fun String.toQuestDescription(originalDescription: String?): String {
    if (!originalDescription.isNullOrBlank()) {
        return originalDescription
    }
    
    val lowerTitle = this.lowercase()
    
    return when {
        lowerTitle.contains("магазин") -> 
            "Отправься на рынок, чтобы добыть провизию для дома"
        lowerTitle.contains("работ") && lowerTitle.contains("проект") -> 
            "Выполни важное поручение Гильдии"
        lowerTitle.contains("убор") -> 
            "Наведи порядок в своих владениях"
        lowerTitle.contains("трениро") -> 
            "Укрепи свое тело и дух"
        lowerTitle.contains("учи") || lowerTitle.contains("изучи") -> 
            "Постигни новые знания и мудрость"
        lowerTitle.contains("встреч") -> 
            "Встреться с союзниками и спутниками"
        else -> 
            "Выполни это задание, чтобы продвинуться по пути героя"
    }
}

/**
 * Возвращает название сложности квеста
 */
fun Int.toDifficultyName(): String = when (this) {
    1 -> "Легкий"
    2 -> "Средний"
    3 -> "Сложный"
    4 -> "Героический"
    5 -> "Легендарный"
    else -> "Обычный"
}

/**
 * Возвращает описание сложности
 */
fun Int.toDifficultyDescription(): String = when (this) {
    1 -> "Простая задача для начинающего героя"
    2 -> "Потребуется некоторое усилие"
    3 -> "Серьезное испытание"
    4 -> "Достойное настоящего героя"
    5 -> "Лишь легенды справятся с этим"
    else -> "Обычное задание"
}

/**
 * Возвращает цитату NPC для мотивации
 */
fun getRandomMotivationalQuote(): String {
    val quotes = listOf(
        "\"Каждый великий герой начинал с малого\"",
        "\"Гильдия верит в тебя, искатель приключений\"",
        "\"Твой путь к славе только начинается\"",
        "\"Боги благосклонны к смелым\"",
        "\"Слава ждет тех, кто не сдается\"",
        "\"Пусть фортуна будет с тобой\"",
        "\"Даже драконы когда-то были маленькими\"",
        "\"Каждый квест приближает тебя к легенде\"",
        "\"Твоя решимость впечатляет, странник\"",
        "\"Таверна всегда рада видеть настоящего героя\""
    )
    return quotes.random()
}

/**
 * Возвращает поздравление при завершении квеста
 */
fun getQuestCompletionMessage(difficulty: Int): String = when (difficulty) {
    1 -> "Отличное начало, искатель!"
    2 -> "Достойная работа, герой!"
    3 -> "Впечатляющий подвиг!"
    4 -> "Героическое свершение!"
    5 -> "ЛЕГЕНДАРНО! Баллады будут петь о тебе!"
    else -> "Квест завершен!"
}

/**
 * Форматирует число опыта для отображения
 */
fun Int.formatXP(): String = when {
    this >= 1000000 -> "${this / 1000000}М XP"
    this >= 1000 -> "${this / 1000}К XP"
    else -> "$this XP"
}

/**
 * Форматирует золото для отображения
 */
fun Int.formatGold(): String = when {
    this >= 1000000 -> "${this / 1000000}М 💰"
    this >= 1000 -> "${this / 1000}К 💰"
    else -> "$this 💰"
}

/**
 * Возвращает название ранга по уровню
 */
fun Int.toRankName(): String = when {
    this < 5 -> "Новичок"
    this < 10 -> "Искатель Приключений"
    this < 20 -> "Опытный Герой"
    this < 30 -> "Чемпион"
    this < 50 -> "Мастер"
    this < 75 -> "Легенда"
    else -> "Бессмертный"
}

/**
 * Возвращает эмодзи ранга
 */
fun Int.toRankEmoji(): String = when {
    this < 5 -> "🌱"
    this < 10 -> "⚔️"
    this < 20 -> "🛡️"
    this < 30 -> "🏆"
    this < 50 -> "👑"
    this < 75 -> "⭐"
    else -> "🔥"
}

