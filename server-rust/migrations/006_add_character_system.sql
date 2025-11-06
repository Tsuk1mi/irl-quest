-- Добавление игровых характеристик пользователя (RPG система)

-- Добавляем поля персонажа в таблицу users
ALTER TABLE users 
    ADD COLUMN IF NOT EXISTS level INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS experience INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS gold INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS avatar_url TEXT,
    ADD COLUMN IF NOT EXISTS bio TEXT,
    ADD COLUMN IF NOT EXISTS timezone TEXT NOT NULL DEFAULT 'UTC',
    ADD COLUMN IF NOT EXISTS last_login TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS settings JSONB NOT NULL DEFAULT '{}'::jsonb;

-- Добавляем D&D характеристики персонажа
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS strength INTEGER NOT NULL DEFAULT 10,
    ADD COLUMN IF NOT EXISTS intelligence INTEGER NOT NULL DEFAULT 10,
    ADD COLUMN IF NOT EXISTS charisma INTEGER NOT NULL DEFAULT 10,
    ADD COLUMN IF NOT EXISTS dexterity INTEGER NOT NULL DEFAULT 10,
    ADD COLUMN IF NOT EXISTS constitution INTEGER NOT NULL DEFAULT 10,
    ADD COLUMN IF NOT EXISTS wisdom INTEGER NOT NULL DEFAULT 10;

-- Добавляем класс и расу персонажа
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS character_class TEXT NOT NULL DEFAULT 'warrior',
    ADD COLUMN IF NOT EXISTS character_race TEXT NOT NULL DEFAULT 'human';

-- Создаем таблицу достижений
-- MOVED TO migration 011_achievements_skills.sql
-- CREATE TABLE IF NOT EXISTS achievements (
--     id SERIAL PRIMARY KEY,
--     name VARCHAR(255) NOT NULL,
--     description TEXT,
--     icon TEXT,
--     reward_xp INTEGER NOT NULL DEFAULT 0,
--     reward_gold INTEGER NOT NULL DEFAULT 0,
--     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
-- );

-- Создаем таблицу заработанных достижений пользователя
-- MOVED TO migration 011_achievements_skills.sql
-- CREATE TABLE IF NOT EXISTS user_achievements (
--     id SERIAL PRIMARY KEY,
--     user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
--     achievement_id INTEGER NOT NULL REFERENCES achievements(id) ON DELETE CASCADE,
--     earned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
--     UNIQUE(user_id, achievement_id)
-- );

-- CREATE INDEX IF NOT EXISTS idx_user_achievements_user_id ON user_achievements(user_id);
-- CREATE INDEX IF NOT EXISTS idx_user_achievements_achievement_id ON user_achievements(achievement_id);

-- Добавляем игровые поля к квестам
ALTER TABLE quests
    ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'active',
    ADD COLUMN IF NOT EXISTS priority TEXT NOT NULL DEFAULT 'medium',
    ADD COLUMN IF NOT EXISTS experience_reward INTEGER NOT NULL DEFAULT 100,
    ADD COLUMN IF NOT EXISTS gold_reward INTEGER NOT NULL DEFAULT 50,
    ADD COLUMN IF NOT EXISTS deadline TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS category TEXT NOT NULL DEFAULT 'general';

-- Добавляем игровые поля к задачам
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS quest_id INTEGER REFERENCES quests(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS priority TEXT NOT NULL DEFAULT 'medium',
    ADD COLUMN IF NOT EXISTS experience_reward INTEGER NOT NULL DEFAULT 25,
    ADD COLUMN IF NOT EXISTS gold_reward INTEGER NOT NULL DEFAULT 10,
    ADD COLUMN IF NOT EXISTS deadline TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS category TEXT NOT NULL DEFAULT 'general';

CREATE INDEX IF NOT EXISTS idx_tasks_quest_id ON tasks(quest_id);

-- Создаем таблицу зон карты мира
CREATE TABLE IF NOT EXISTS world_zones (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category TEXT NOT NULL, -- 'work', 'study', 'health', 'hobby', etc.
    icon TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Создаем таблицу прогресса пользователя по зонам
CREATE TABLE IF NOT EXISTS user_zone_progress (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    zone_id INTEGER NOT NULL REFERENCES world_zones(id) ON DELETE CASCADE,
    completion_percentage INTEGER NOT NULL DEFAULT 0 CHECK (completion_percentage >= 0 AND completion_percentage <= 100),
    last_activity TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, zone_id)
);

CREATE INDEX IF NOT EXISTS idx_user_zone_progress_user_id ON user_zone_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_user_zone_progress_zone_id ON user_zone_progress(zone_id);

-- Добавляем начальные достижения
-- MOVED TO migration 011_achievements_skills.sql
-- INSERT INTO achievements (name, description, icon, reward_xp, reward_gold) VALUES
--     ('Первый Шаг', 'Зарегистрируйтесь в игре', '🎯', 50, 20),
--     ('Новичок', 'Выполните первую задачу', '⭐', 100, 50),
--     ('Квестодатель', 'Создайте свой первый квест', '📜', 150, 75),
--     ('Мастер Дел', 'Выполните 10 задач', '🏆', 300, 150),
--     ('Легендарный Герой', 'Достигните 10 уровня', '👑', 1000, 500)
-- ON CONFLICT DO NOTHING;

-- Добавляем начальные зоны мира
INSERT INTO world_zones (name, description, category, icon) VALUES
    ('Город Дел', 'Место, где выполняются рабочие задачи и профессиональные квесты', 'work', '🏢'),
    ('Гора Знаний', 'Священное место обучения и развития навыков', 'study', '📚'),
    ('Лес Спокойствия', 'Тихая локация для отдыха и хобби', 'hobby', '🌲'),
    ('Храм Здоровья', 'Святилище, посвященное физическому и ментальному здоровью', 'health', '💪'),
    ('Пещера Хаоса', 'Темное место, где скапливаются просроченные задачи', 'overdue', '⚠️')
ON CONFLICT DO NOTHING;

-- Обновляем тестовых пользователей начальными характеристиками
UPDATE users SET 
    gold = 100,
    strength = 12,
    intelligence = 10,
    charisma = 8
WHERE username = 'testuser';

