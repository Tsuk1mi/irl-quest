-- Fix schema issues

-- Add user profile fields one by one  
ALTER TABLE users ADD COLUMN level INTEGER DEFAULT 1;
ALTER TABLE users ADD COLUMN experience INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN avatar_url TEXT;
ALTER TABLE users ADD COLUMN bio TEXT;
ALTER TABLE users ADD COLUMN timezone VARCHAR(50) DEFAULT 'UTC';
ALTER TABLE users ADD COLUMN last_login TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN settings JSONB DEFAULT '{}'::jsonb;

-- Enhanced quests with more features (add columns one by one)
ALTER TABLE quests ADD COLUMN status VARCHAR(20) DEFAULT 'active';
ALTER TABLE quests ADD COLUMN priority VARCHAR(20) DEFAULT 'medium';
ALTER TABLE quests ADD COLUMN deadline TIMESTAMPTZ;
ALTER TABLE quests ADD COLUMN completion_percentage INTEGER DEFAULT 0;
ALTER TABLE quests ADD COLUMN reward_experience INTEGER DEFAULT 0;
ALTER TABLE quests ADD COLUMN reward_description TEXT;
ALTER TABLE quests ADD COLUMN tags TEXT[] DEFAULT '{}';
ALTER TABLE quests ADD COLUMN is_public BOOLEAN DEFAULT FALSE;
ALTER TABLE quests ADD COLUMN location_name TEXT;
ALTER TABLE quests ADD COLUMN quest_type VARCHAR(50) DEFAULT 'personal';
ALTER TABLE quests ADD COLUMN metadata JSONB DEFAULT '{}'::jsonb;

-- Enhanced tasks with quest relationship and more features (add columns one by one)
ALTER TABLE tasks ADD COLUMN quest_id INTEGER REFERENCES quests(id) ON DELETE SET NULL;
ALTER TABLE tasks ADD COLUMN status VARCHAR(20) DEFAULT 'pending';
ALTER TABLE tasks ADD COLUMN priority VARCHAR(20) DEFAULT 'medium';
ALTER TABLE tasks ADD COLUMN deadline TIMESTAMPTZ;
ALTER TABLE tasks ADD COLUMN estimated_duration INTEGER; -- in minutes
ALTER TABLE tasks ADD COLUMN actual_duration INTEGER; -- in minutes  
ALTER TABLE tasks ADD COLUMN difficulty INTEGER DEFAULT 1;
ALTER TABLE tasks ADD COLUMN experience_reward INTEGER DEFAULT 0;
ALTER TABLE tasks ADD COLUMN tags TEXT[] DEFAULT '{}';
ALTER TABLE tasks ADD COLUMN location_name TEXT;
ALTER TABLE tasks ADD COLUMN subtasks JSONB DEFAULT '[]'::jsonb;
ALTER TABLE tasks ADD COLUMN notes TEXT;
ALTER TABLE tasks ADD COLUMN attachments TEXT[] DEFAULT '{}';
ALTER TABLE tasks ADD COLUMN completion_proof TEXT;
ALTER TABLE tasks ADD COLUMN metadata JSONB DEFAULT '{}'::jsonb;

-- Add constraints after columns are created
ALTER TABLE quests ADD CONSTRAINT quests_status_check CHECK (status IN ('active', 'completed', 'paused', 'archived'));
ALTER TABLE quests ADD CONSTRAINT quests_priority_check CHECK (priority IN ('low', 'medium', 'high', 'critical'));
ALTER TABLE quests ADD CONSTRAINT quests_completion_check CHECK (completion_percentage >= 0 AND completion_percentage <= 100);
ALTER TABLE quests ADD CONSTRAINT quests_type_check CHECK (quest_type IN ('personal', 'collaborative', 'challenge', 'learning'));

ALTER TABLE tasks ADD CONSTRAINT tasks_status_check CHECK (status IN ('pending', 'in_progress', 'completed', 'cancelled'));
ALTER TABLE tasks ADD CONSTRAINT tasks_priority_check CHECK (priority IN ('low', 'medium', 'high', 'critical'));
ALTER TABLE tasks ADD CONSTRAINT tasks_difficulty_check CHECK (difficulty >= 1 AND difficulty <= 5);

-- Create indices for new columns
CREATE INDEX IF NOT EXISTS idx_quests_status ON quests(status);
CREATE INDEX IF NOT EXISTS idx_quests_priority ON quests(priority);
CREATE INDEX IF NOT EXISTS idx_quests_deadline ON quests(deadline);
CREATE INDEX IF NOT EXISTS idx_quests_type ON quests(quest_type);

CREATE INDEX IF NOT EXISTS idx_tasks_quest_id ON tasks(quest_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_tasks_priority ON tasks(priority);
CREATE INDEX IF NOT EXISTS idx_tasks_deadline ON tasks(deadline);