-- Enhanced IRL Quest Schema

-- Add user profiles and stats
ALTER TABLE users ADD COLUMN IF NOT EXISTS 
    level INTEGER DEFAULT 1,
    experience INTEGER DEFAULT 0,
    avatar_url TEXT,
    bio TEXT,
    timezone VARCHAR(50) DEFAULT 'UTC',
    last_login TIMESTAMPTZ,
    settings JSONB DEFAULT '{}'::jsonb;

-- Create user achievements table
CREATE TABLE IF NOT EXISTS user_achievements (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    achievement_type VARCHAR(100) NOT NULL,
    achievement_data JSONB DEFAULT '{}'::jsonb,
    earned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, achievement_type)
);

-- Enhanced quests with more features
ALTER TABLE quests ADD COLUMN IF NOT EXISTS
    status VARCHAR(20) DEFAULT 'active' CHECK (status IN ('active', 'completed', 'paused', 'archived')),
    priority VARCHAR(20) DEFAULT 'medium' CHECK (priority IN ('low', 'medium', 'high', 'critical')),
    deadline TIMESTAMPTZ,
    completion_percentage INTEGER DEFAULT 0 CHECK (completion_percentage >= 0 AND completion_percentage <= 100),
    reward_experience INTEGER DEFAULT 0,
    reward_description TEXT,
    tags TEXT[] DEFAULT '{}',
    is_public BOOLEAN DEFAULT FALSE,
    location GEOGRAPHY(POINT),
    location_name TEXT,
    quest_type VARCHAR(50) DEFAULT 'personal' CHECK (quest_type IN ('personal', 'collaborative', 'challenge', 'learning')),
    metadata JSONB DEFAULT '{}'::jsonb;

-- Enhanced tasks with quest relationship and more features  
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS
    quest_id INTEGER REFERENCES quests(id) ON DELETE SET NULL,
    status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'in_progress', 'completed', 'cancelled')),
    priority VARCHAR(20) DEFAULT 'medium' CHECK (priority IN ('low', 'medium', 'high', 'critical')),
    deadline TIMESTAMPTZ,
    estimated_duration INTEGER, -- in minutes
    actual_duration INTEGER, -- in minutes  
    difficulty INTEGER DEFAULT 1 CHECK (difficulty >= 1 AND difficulty <= 5),
    experience_reward INTEGER DEFAULT 0,
    tags TEXT[] DEFAULT '{}',
    location GEOGRAPHY(POINT),
    location_name TEXT,
    subtasks JSONB DEFAULT '[]'::jsonb,
    notes TEXT,
    attachments TEXT[] DEFAULT '{}',
    completion_proof TEXT, -- URL or description of completion proof
    metadata JSONB DEFAULT '{}'::jsonb;

-- Create focus sessions table (Pomodoro-style)
CREATE TABLE IF NOT EXISTS focus_sessions (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    task_id INTEGER REFERENCES tasks(id) ON DELETE SET NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 25,
    actual_duration_minutes INTEGER,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at TIMESTAMPTZ,
    session_type VARCHAR(20) DEFAULT 'work' CHECK (session_type IN ('work', 'break', 'long_break')),
    notes TEXT,
    interruptions INTEGER DEFAULT 0,
    productivity_rating INTEGER CHECK (productivity_rating >= 1 AND productivity_rating <= 5)
);

-- Create teams/groups table
CREATE TABLE IF NOT EXISTS teams (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    avatar_url TEXT,
    owner_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    is_public BOOLEAN DEFAULT FALSE,
    max_members INTEGER DEFAULT 10,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    settings JSONB DEFAULT '{}'::jsonb
);

-- Create team memberships
CREATE TABLE IF NOT EXISTS team_memberships (
    id SERIAL PRIMARY KEY,
    team_id INTEGER REFERENCES teams(id) ON DELETE CASCADE,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) DEFAULT 'member' CHECK (role IN ('owner', 'admin', 'member')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(team_id, user_id)
);

-- Create collaborative quests
CREATE TABLE IF NOT EXISTS team_quests (
    id SERIAL PRIMARY KEY,
    team_id INTEGER REFERENCES teams(id) ON DELETE CASCADE,
    quest_id INTEGER REFERENCES quests(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(team_id, quest_id)
);

-- Create notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    data JSONB DEFAULT '{}'::jsonb,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create daily stats table
CREATE TABLE IF NOT EXISTS daily_stats (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    tasks_completed INTEGER DEFAULT 0,
    focus_sessions INTEGER DEFAULT 0,
    total_focus_time INTEGER DEFAULT 0, -- in minutes
    experience_gained INTEGER DEFAULT 0,
    quests_completed INTEGER DEFAULT 0,
    productivity_score DECIMAL(3,2), -- 0.00 to 5.00
    notes TEXT,
    UNIQUE(user_id, date)
);

-- Create indices for performance
CREATE INDEX IF NOT EXISTS idx_user_achievements_user_id ON user_achievements(user_id);
CREATE INDEX IF NOT EXISTS idx_user_achievements_type ON user_achievements(achievement_type);

CREATE INDEX IF NOT EXISTS idx_quests_status ON quests(status);
CREATE INDEX IF NOT EXISTS idx_quests_priority ON quests(priority);
CREATE INDEX IF NOT EXISTS idx_quests_deadline ON quests(deadline);
CREATE INDEX IF NOT EXISTS idx_quests_type ON quests(quest_type);

CREATE INDEX IF NOT EXISTS idx_tasks_quest_id ON tasks(quest_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_tasks_priority ON tasks(priority);
CREATE INDEX IF NOT EXISTS idx_tasks_deadline ON tasks(deadline);

CREATE INDEX IF NOT EXISTS idx_focus_sessions_user_id ON focus_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_focus_sessions_task_id ON focus_sessions(task_id);
CREATE INDEX IF NOT EXISTS idx_focus_sessions_started ON focus_sessions(started_at);

CREATE INDEX IF NOT EXISTS idx_teams_owner_id ON teams(owner_id);
CREATE INDEX IF NOT EXISTS idx_team_memberships_team_id ON team_memberships(team_id);
CREATE INDEX IF NOT EXISTS idx_team_memberships_user_id ON team_memberships(user_id);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_created ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;

CREATE INDEX IF NOT EXISTS idx_daily_stats_user_date ON daily_stats(user_id, date);

-- Create some useful functions
CREATE OR REPLACE FUNCTION update_user_experience(user_id_param INTEGER, exp_gained INTEGER)
RETURNS VOID AS $$
BEGIN
    UPDATE users 
    SET experience = experience + exp_gained,
        level = CASE 
            WHEN (experience + exp_gained) >= level * 1000 THEN level + 1 
            ELSE level 
        END
    WHERE id = user_id_param;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION calculate_quest_completion(quest_id_param INTEGER)
RETURNS INTEGER AS $$
DECLARE
    total_tasks INTEGER;
    completed_tasks INTEGER;
    completion_pct INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_tasks FROM tasks WHERE quest_id = quest_id_param;
    
    IF total_tasks = 0 THEN
        RETURN 0;
    END IF;
    
    SELECT COUNT(*) INTO completed_tasks 
    FROM tasks 
    WHERE quest_id = quest_id_param AND status = 'completed';
    
    completion_pct := (completed_tasks * 100) / total_tasks;
    
    UPDATE quests 
    SET completion_percentage = completion_pct,
        status = CASE WHEN completion_pct = 100 THEN 'completed' ELSE status END
    WHERE id = quest_id_param;
    
    RETURN completion_pct;
END;
$$ LANGUAGE plpgsql;