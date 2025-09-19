-- Простая рабочая схема для быстрого запуска
-- Сначала очистим и пересоздадим таблицы

DROP TABLE IF EXISTS tasks CASCADE;
DROP TABLE IF EXISTS quests CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Создаем простые таблицы
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    hashed_password TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Создаем индексы для пользователей
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);

-- Создаем таблицу задач
CREATE TABLE tasks (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    owner_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE
);

-- Создаем индексы для задач
CREATE INDEX idx_tasks_title ON tasks(title);
CREATE INDEX idx_tasks_owner_id ON tasks(owner_id);
CREATE INDEX idx_tasks_completed ON tasks(completed);

-- Создаем таблицу квестов
CREATE TABLE quests (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    difficulty INTEGER NOT NULL DEFAULT 1 CHECK (difficulty >= 1 AND difficulty <= 5),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    owner_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE
);

-- Создаем индексы для квестов
CREATE INDEX idx_quests_title ON quests(title);
CREATE INDEX idx_quests_owner_id ON quests(owner_id);
CREATE INDEX idx_quests_difficulty ON quests(difficulty);

-- Создаем тестовые данные
INSERT INTO users (email, username, hashed_password) VALUES 
('test@example.com', 'testuser', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewrNYhQ8l.8XBOa6'), -- password: password123
('demo@example.com', 'demo', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewrNYhQ8l.8XBOa6'); -- password: password123

INSERT INTO tasks (title, description, owner_id) VALUES 
('Изучить Rust', 'Изучить основы языка программирования Rust', 1),
('Создать мобильное приложение', 'Разработать Android приложение с использованием Kotlin', 1),
('Настроить сервер', 'Развернуть и настроить веб-сервер', 1);

INSERT INTO quests (title, description, difficulty, owner_id) VALUES
('Стать Rust разработчиком', 'Изучить Rust и создать первый проект', 3, 1),
('Мастер мобильной разработки', 'Освоить Android разработку', 4, 1);

-- Включаем расширение для векторов (для будущего использования)
CREATE EXTENSION IF NOT EXISTS vector;