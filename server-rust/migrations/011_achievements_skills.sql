-- Система достижений и дерева навыков
-- Версия: 2.1.0
-- Дата: 31.10.2025

-- Таблица достижений
CREATE TABLE IF NOT EXISTS achievements (
    id SERIAL PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    icon TEXT NOT NULL,
    category TEXT NOT NULL, -- quests, combat, social, exploration, collection, progression
    rarity TEXT NOT NULL, -- common, rare, epic, legendary
    reward_experience INTEGER NOT NULL DEFAULT 0,
    reward_gold INTEGER NOT NULL DEFAULT 0,
    required_progress INTEGER NOT NULL DEFAULT 1,
    hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_achievements_code ON achievements(code);
CREATE INDEX idx_achievements_category ON achievements(category);
CREATE INDEX idx_achievements_rarity ON achievements(rarity);

-- Прогресс достижений пользователя
-- Удаляем старую версию таблицы если она есть
DROP TABLE IF EXISTS user_achievements CASCADE;

CREATE TABLE user_achievements (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    achievement_id INTEGER NOT NULL,
    progress INTEGER NOT NULL DEFAULT 0,
    required_progress INTEGER NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE,
    UNIQUE(user_id, achievement_id)
);

CREATE INDEX idx_user_achievements_user_id ON user_achievements(user_id);
CREATE INDEX idx_user_achievements_completed ON user_achievements(completed);

-- Таблица навыков (Skill Tree)
CREATE TABLE IF NOT EXISTS skills (
    id SERIAL PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    icon TEXT NOT NULL,
    tier INTEGER NOT NULL, -- 1-5 уровень в дереве
    max_level INTEGER NOT NULL DEFAULT 5,
    cost_per_level INTEGER NOT NULL DEFAULT 1,
    prerequisites TEXT, -- JSON массив кодов требуемых навыков
    stat_bonuses TEXT, -- JSON объект бонусов к характеристикам
    reward_multiplier REAL,
    unlock_feature TEXT,
    passive_bonus TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_skills_code ON skills(code);
CREATE INDEX idx_skills_tier ON skills(tier);

-- Навыки пользователя
CREATE TABLE IF NOT EXISTS user_skills (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    skill_id INTEGER NOT NULL,
    current_level INTEGER NOT NULL DEFAULT 0,
    is_unlocked BOOLEAN NOT NULL DEFAULT FALSE,
    unlocked_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    UNIQUE(user_id, skill_id)
);

CREATE INDEX idx_user_skills_user_id ON user_skills(user_id);
CREATE INDEX idx_user_skills_unlocked ON user_skills(is_unlocked);

-- Очки навыков
ALTER TABLE user_stat_points ADD COLUMN skill_points INTEGER DEFAULT 0;

-- Предзаполнение базовых достижений
INSERT INTO achievements (code, name, description, icon, category, rarity, reward_experience, reward_gold, required_progress) VALUES
('first_quest', 'Первый квест', 'Выполните свой первый квест', '⭐', 'quests', 'common', 100, 50, 1),
('quest_master', 'Мастер квестов', 'Выполните 100 квестов', '🏆', 'quests', 'rare', 500, 250, 100),
('speed_demon', 'Скоростной демон', 'Выполните 10 квестов за один день', '⚡', 'quests', 'epic', 1000, 500, 10),
('legendary_hero', 'Легендарный герой', 'Достигните 20 уровня', '👑', 'progression', 'legendary', 2000, 1000, 1),
('social_butterfly', 'Социальная бабочка', 'Пригласите 5 друзей', '🦋', 'social', 'rare', 300, 150, 5),
('treasure_hunter', 'Охотник за сокровищами', 'Соберите 50 предметов', '💎', 'collection', 'rare', 400, 200, 50),
('streak_champion', 'Чемпион стрика', 'Достигните стрика в 30 дней', '🔥', 'progression', 'epic', 1500, 750, 30);

-- Предзаполнение базовых навыков
INSERT INTO skills (code, name, description, icon, tier, max_level, cost_per_level, prerequisites) VALUES
-- Tier 1 (базовые)
('task_efficiency', 'Эффективность', 'Увеличивает награды за задачи на 5% за уровень', '📈', 1, 5, 1, '[]'),
('quick_learner', 'Быстрое обучение', 'Дополнительно +10% опыта', '🎓', 1, 5, 1, '[]'),
('lucky_coin', 'Удачливая монета', '+15% золота за квесты', '🪙', 1, 5, 1, '[]'),

-- Tier 2 (требуют Tier 1)
('master_planner', 'Мастер планирования', 'Получайте бонус за планирование квестов заранее', '📋', 2, 3, 2, '["task_efficiency"]'),
('experience_boost', 'Усиление опыта', '+25% опыта от сложных квестов', '⭐', 2, 3, 2, '["quick_learner"]'),
('gold_rush', 'Золотая лихорадка', 'Шанс удвоить золото 10%', '💰', 2, 3, 2, '["lucky_coin"]'),

-- Tier 3 (мощные навыки)
('party_leader', 'Лидер группы', 'Дополнительные награды в мультиплеере', '👥', 3, 3, 3, '["master_planner"]'),
('legendary_luck', 'Легендарная удача', 'Шанс легендарных предметов +5%', '🍀', 3, 3, 3, '["gold_rush"]');

