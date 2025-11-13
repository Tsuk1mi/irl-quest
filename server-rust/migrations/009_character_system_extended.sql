-- Расширенная система персонажей (классы, расы, характеристики)
-- Версия: 2.1.0
-- Дата: 31.10.2025

-- Добавить расу к пользователю
ALTER TABLE users ADD COLUMN IF NOT EXISTS race TEXT DEFAULT 'human';

-- Таблица доступных очков характеристик
CREATE TABLE IF NOT EXISTS user_stat_points (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL UNIQUE,
    available_points INTEGER NOT NULL DEFAULT 0,
    total_earned INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_stat_points_user_id ON user_stat_points(user_id);

-- Таблица прокачки (history)
CREATE TABLE IF NOT EXISTS stat_increase_history (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    stat_name TEXT NOT NULL, -- strength, intelligence, dexterity, charisma, luck
    old_value INTEGER NOT NULL,
    new_value INTEGER NOT NULL,
    source TEXT NOT NULL, -- 'level_up', 'item', 'event'
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_stat_increase_history_user_id ON stat_increase_history(user_id);
CREATE INDEX IF NOT EXISTS idx_stat_increase_history_created_at ON stat_increase_history(created_at);

-- Добавить колонку luck если её еще нет
ALTER TABLE users ADD COLUMN IF NOT EXISTS luck INTEGER DEFAULT 10;

-- Обновить значения по умолчанию для новых пользователей
-- Установить базовые характеристики для существующих пользователей
UPDATE users SET strength = 10 WHERE strength IS NULL;
UPDATE users SET intelligence = 10 WHERE intelligence IS NULL;
UPDATE users SET dexterity = 10 WHERE dexterity IS NULL;
UPDATE users SET charisma = 10 WHERE charisma IS NULL;
UPDATE users SET luck = 10 WHERE luck IS NULL;
UPDATE users SET race = 'human' WHERE race IS NULL;

