-- Миграция для системы наград и экономики
-- Версия: 2.1.0
-- Дата: 31.10.2025

-- Таблица для хранения ежедневной статистики наград
CREATE TABLE IF NOT EXISTS daily_reward_stats (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    date DATE NOT NULL,
    total_experience INTEGER NOT NULL DEFAULT 0,
    total_gold INTEGER NOT NULL DEFAULT 0,
    total_items INTEGER NOT NULL DEFAULT 0,
    quests_completed INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id, date)
);

CREATE INDEX idx_daily_reward_stats_user_date ON daily_reward_stats(user_id, date);
CREATE INDEX idx_daily_reward_stats_date ON daily_reward_stats(date);

-- Таблица для стриков (последовательное выполнение)
CREATE TABLE IF NOT EXISTS user_streaks (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL UNIQUE,
    current_streak INTEGER NOT NULL DEFAULT 0,
    best_streak INTEGER NOT NULL DEFAULT 0,
    last_completion_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_streaks_user_id ON user_streaks(user_id);

-- Таблица для инвентаря предметов
CREATE TABLE IF NOT EXISTS user_inventory (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    item_id TEXT NOT NULL,
    item_name TEXT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    rarity TEXT NOT NULL, -- common, uncommon, rare, epic, legendary
    acquired_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_inventory_user_id ON user_inventory(user_id);
CREATE INDEX idx_user_inventory_item_id ON user_inventory(user_id, item_id);

-- Таблица для истории наград
CREATE TABLE IF NOT EXISTS reward_history (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    quest_id INTEGER,
    task_id INTEGER,
    experience_earned INTEGER NOT NULL,
    gold_earned INTEGER NOT NULL,
    items_json TEXT, -- JSON массив предметов
    modifiers_json TEXT, -- JSON модификаторов
    total_multiplier REAL NOT NULL,
    soft_cap_applied BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (quest_id) REFERENCES quests(id) ON DELETE SET NULL,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL
);

CREATE INDEX idx_reward_history_user_id ON reward_history(user_id);
CREATE INDEX idx_reward_history_created_at ON reward_history(created_at);
CREATE INDEX idx_reward_history_quest_id ON reward_history(quest_id);

-- Таблица для временных событий (бонусы)
CREATE TABLE IF NOT EXISTS reward_events (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    bonus_multiplier REAL NOT NULL DEFAULT 1.25, -- 25% бонус
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reward_events_active ON reward_events(is_active, start_date, end_date);

-- Добавить колонки к таблице users для характеристик
-- Already added in migration 006
-- ALTER TABLE users ADD COLUMN strength INTEGER DEFAULT 10;
-- ALTER TABLE users ADD COLUMN intelligence INTEGER DEFAULT 10;
-- ALTER TABLE users ADD COLUMN dexterity INTEGER DEFAULT 10;
-- ALTER TABLE users ADD COLUMN charisma INTEGER DEFAULT 10;
-- ALTER TABLE users ADD COLUMN luck INTEGER DEFAULT 10;

-- Таблица для отслеживания завершений квестов (анти-фарм)
CREATE TABLE IF NOT EXISTS quest_completions (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    quest_id INTEGER NOT NULL,
    times_completed INTEGER NOT NULL DEFAULT 1,
    first_completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (quest_id) REFERENCES quests(id) ON DELETE CASCADE,
    UNIQUE(user_id, quest_id)
);

CREATE INDEX idx_quest_completions_user_quest ON quest_completions(user_id, quest_id);

