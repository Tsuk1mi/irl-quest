-- Дополнительная миграция: добавление недостающих таблиц если они не созданы в 012

-- Проверяем и создаём таблицу guilds если не существует
CREATE TABLE IF NOT EXISTS guilds (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    leader_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    level SMALLINT NOT NULL DEFAULT 1,
    experience INTEGER NOT NULL DEFAULT 0,
    member_count INTEGER NOT NULL DEFAULT 0,
    max_members INTEGER NOT NULL DEFAULT 50,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_guilds_leader ON guilds(leader_id);
CREATE INDEX IF NOT EXISTS idx_guilds_level ON guilds(level DESC, experience DESC);

-- Таблица членов гильдий
CREATE TABLE IF NOT EXISTS guild_members (
    id SERIAL PRIMARY KEY,
    guild_id INTEGER NOT NULL REFERENCES guilds(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'member',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(guild_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_guild_members_guild ON guild_members(guild_id);
CREATE INDEX IF NOT EXISTS idx_guild_members_user ON guild_members(user_id);

-- Таблица кооп-миссий
CREATE TABLE IF NOT EXISTS coop_missions (
    id SERIAL PRIMARY KEY,
    quest_id INTEGER NOT NULL REFERENCES quests(id) ON DELETE CASCADE,
    party_size SMALLINT NOT NULL DEFAULT 1,
    max_party_size SMALLINT NOT NULL DEFAULT 4,
    leader_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'recruiting',
    is_public BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_coop_missions_quest ON coop_missions(quest_id);
CREATE INDEX IF NOT EXISTS idx_coop_missions_leader ON coop_missions(leader_id);
CREATE INDEX IF NOT EXISTS idx_coop_missions_status ON coop_missions(status);

-- Таблица участников миссий
CREATE TABLE IF NOT EXISTS party_members (
    id SERIAL PRIMARY KEY,
    mission_id INTEGER NOT NULL REFERENCES coop_missions(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'support',
    contribution INTEGER NOT NULL DEFAULT 0,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(mission_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_party_members_mission ON party_members(mission_id);
CREATE INDEX IF NOT EXISTS idx_party_members_user ON party_members(user_id);

-- Enum для качества предметов
DO $$ BEGIN
    CREATE TYPE item_quality AS ENUM ('common', 'uncommon', 'rare', 'epic', 'legendary');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Таблица аукциона
CREATE TABLE IF NOT EXISTS auction_listings (
    id SERIAL PRIMARY KEY,
    seller_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_name VARCHAR(200) NOT NULL,
    item_description TEXT,
    quality item_quality NOT NULL DEFAULT 'common',
    price INTEGER NOT NULL CHECK (price > 0),
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_auction_seller ON auction_listings(seller_id);
CREATE INDEX IF NOT EXISTS idx_auction_expires ON auction_listings(expires_at);
CREATE INDEX IF NOT EXISTS idx_auction_quality ON auction_listings(quality);

-- История покупок на аукционе
CREATE TABLE IF NOT EXISTS auction_purchases (
    id SERIAL PRIMARY KEY,
    listing_id INTEGER NOT NULL,
    buyer_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seller_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_name VARCHAR(200) NOT NULL,
    quality item_quality NOT NULL,
    price INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    purchased_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_auction_purchases_buyer ON auction_purchases(buyer_id);
CREATE INDEX IF NOT EXISTS idx_auction_purchases_seller ON auction_purchases(seller_id);

-- Дневная статистика
CREATE TABLE IF NOT EXISTS daily_stats (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    tasks_completed INTEGER NOT NULL DEFAULT 0,
    tasks_total INTEGER NOT NULL DEFAULT 0,
    quests_completed INTEGER NOT NULL DEFAULT 0,
    quests_total INTEGER NOT NULL DEFAULT 0,
    experience_gained INTEGER NOT NULL DEFAULT 0,
    focus_time INTEGER NOT NULL DEFAULT 0,
    study_time INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, date)
);

CREATE INDEX IF NOT EXISTS idx_daily_stats_user_date ON daily_stats(user_id, date DESC);

-- Инвентарь пользователей
CREATE TABLE IF NOT EXISTS user_inventory (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_name VARCHAR(200) NOT NULL,
    item_description TEXT,
    quality item_quality NOT NULL DEFAULT 'common',
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    base_price INTEGER NOT NULL DEFAULT 10,
    acquired_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    equipped BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_inventory_user ON user_inventory(user_id);
CREATE INDEX IF NOT EXISTS idx_inventory_quality ON user_inventory(quality);

