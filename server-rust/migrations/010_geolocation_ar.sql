-- Система геолокации и AR
-- Версия: 2.1.0
-- Дата: 31.10.2025

-- Геозоны
CREATE TABLE IF NOT EXISTS geo_zones (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    radius_meters REAL NOT NULL,
    zone_type TEXT NOT NULL, -- home, work, gym, shop, park, custom
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_geo_zones_user_id ON geo_zones(user_id);
CREATE INDEX IF NOT EXISTS idx_geo_zones_type ON geo_zones(zone_type);

-- Гео-триггеры для квестов
CREATE TABLE IF NOT EXISTS geo_triggers (
    id SERIAL PRIMARY KEY,
    quest_id INTEGER NOT NULL,
    zone_id INTEGER NOT NULL,
    trigger_type TEXT NOT NULL, -- on_enter, on_exit, while_inside
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    triggered_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (quest_id) REFERENCES quests(id) ON DELETE CASCADE,
    FOREIGN KEY (zone_id) REFERENCES geo_zones(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_geo_triggers_quest_id ON geo_triggers(quest_id);
CREATE INDEX IF NOT EXISTS idx_geo_triggers_zone_id ON geo_triggers(zone_id);
CREATE INDEX IF NOT EXISTS idx_geo_triggers_active ON geo_triggers(is_active);

-- AR маркеры на карте
CREATE TABLE IF NOT EXISTS ar_markers (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    marker_type TEXT NOT NULL, -- artifact, chest, npc, waypoint
    quest_id INTEGER,
    is_collected BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (quest_id) REFERENCES quests(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_ar_markers_location ON ar_markers(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_ar_markers_quest_id ON ar_markers(quest_id);
CREATE INDEX IF NOT EXISTS idx_ar_markers_collected ON ar_markers(is_collected);

-- Метаданные обработки изображений (БЕЗ хранения самих изображений!)
CREATE TABLE IF NOT EXISTS image_processing_metadata (
    id SERIAL PRIMARY KEY,
    quest_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    image_hash TEXT NOT NULL UNIQUE, -- SHA256 hash
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verification_status TEXT NOT NULL, -- pending, ai_approved, ai_rejected, needs_review, approved, rejected
    ai_confidence REAL,
    ai_detected_objects TEXT, -- JSON массив обнаруженных объектов
    latitude REAL,
    longitude REAL,
    moderator_id INTEGER,
    moderator_notes TEXT,
    verified_at TIMESTAMP,
    FOREIGN KEY (quest_id) REFERENCES quests(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (moderator_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_image_metadata_quest_id ON image_processing_metadata(quest_id);
CREATE INDEX IF NOT EXISTS idx_image_metadata_user_id ON image_processing_metadata(user_id);
CREATE INDEX IF NOT EXISTS idx_image_metadata_status ON image_processing_metadata(verification_status);
CREATE INDEX IF NOT EXISTS idx_image_metadata_hash ON image_processing_metadata(image_hash);

-- История триггеров геолокации
CREATE TABLE IF NOT EXISTS geo_trigger_history (
    id SERIAL PRIMARY KEY,
    trigger_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    triggered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (trigger_id) REFERENCES geo_triggers(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_geo_trigger_history_trigger_id ON geo_trigger_history(trigger_id);
CREATE INDEX IF NOT EXISTS idx_geo_trigger_history_user_id ON geo_trigger_history(user_id);
CREATE INDEX IF NOT EXISTS idx_geo_trigger_history_triggered_at ON geo_trigger_history(triggered_at);

-- Согласие пользователя на обработку изображений
CREATE TABLE IF NOT EXISTS user_consents (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL UNIQUE,
    camera_consent BOOLEAN NOT NULL DEFAULT FALSE,
    location_consent BOOLEAN NOT NULL DEFAULT FALSE,
    data_processing_consent BOOLEAN NOT NULL DEFAULT FALSE,
    marketing_consent BOOLEAN NOT NULL DEFAULT FALSE,
    consent_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_consents_user_id ON user_consents(user_id);

