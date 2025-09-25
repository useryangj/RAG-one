-- 角色扮演系统相关表结构
-- 执行此脚本来添加角色扮演功能所需的数据库表

-- 角色表 (characters)
CREATE TABLE IF NOT EXISTS characters (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    avatar_url VARCHAR(500),
    personality TEXT,
    background TEXT,
    speaking_style TEXT,
    example_conversations TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL,
    knowledge_base_id BIGINT,
    CONSTRAINT fk_character_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_character_knowledge_base FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases(id) ON DELETE SET NULL
);

-- 角色配置文件表 (character_profiles)
CREATE TABLE IF NOT EXISTS character_profiles (
    id BIGSERIAL PRIMARY KEY,
    system_prompt TEXT NOT NULL,
    greeting_message TEXT,
    conversation_starters JSONB,
    personality_traits JSONB,
    response_style JSONB,
    knowledge_context TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    character_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_profile_character FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE
);

-- 角色扮演会话表 (roleplay_sessions)
CREATE TABLE IF NOT EXISTS roleplay_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL UNIQUE,
    session_name VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    session_config JSONB,
    message_count INTEGER NOT NULL DEFAULT 0,
    total_tokens BIGINT DEFAULT 0,
    last_activity_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL,
    character_id BIGINT NOT NULL,
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_session_character FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE
);

-- 角色扮演对话历史表 (roleplay_histories)
CREATE TABLE IF NOT EXISTS roleplay_histories (
    id BIGSERIAL PRIMARY KEY,
    turn_number INTEGER NOT NULL,
    user_message TEXT NOT NULL,
    character_response TEXT NOT NULL,
    context_chunks JSONB,
    used_rag BOOLEAN NOT NULL DEFAULT FALSE,
    response_time_ms BIGINT,
    token_usage JSONB,
    user_rating INTEGER,
    user_feedback TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    roleplay_session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    character_id BIGINT NOT NULL,
    CONSTRAINT fk_history_session FOREIGN KEY (roleplay_session_id) REFERENCES roleplay_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_history_character FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE,
    CONSTRAINT chk_user_rating CHECK (user_rating >= 1 AND user_rating <= 5)
);

-- 创建索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_character_user_id ON characters(user_id);
CREATE INDEX IF NOT EXISTS idx_character_status ON characters(status);
CREATE INDEX IF NOT EXISTS idx_character_knowledge_base_id ON characters(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_character_public ON characters(is_public, status);

CREATE INDEX IF NOT EXISTS idx_rp_session_user_id ON roleplay_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_rp_session_character_id ON roleplay_sessions(character_id);
CREATE INDEX IF NOT EXISTS idx_rp_session_id ON roleplay_sessions(session_id);
CREATE INDEX IF NOT EXISTS idx_rp_session_status ON roleplay_sessions(status);
CREATE INDEX IF NOT EXISTS idx_rp_session_last_activity ON roleplay_sessions(last_activity_at);

CREATE INDEX IF NOT EXISTS idx_rp_history_session_id ON roleplay_histories(roleplay_session_id);
CREATE INDEX IF NOT EXISTS idx_rp_history_user_id ON roleplay_histories(user_id);
CREATE INDEX IF NOT EXISTS idx_rp_history_character_id ON roleplay_histories(character_id);
CREATE INDEX IF NOT EXISTS idx_rp_history_turn_number ON roleplay_histories(roleplay_session_id, turn_number);
CREATE INDEX IF NOT EXISTS idx_rp_history_created_at ON roleplay_histories(created_at);
CREATE INDEX IF NOT EXISTS idx_rp_history_rating ON roleplay_histories(user_rating);

-- 添加触发器以自动更新 updated_at 字段
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为相关表创建触发器
DROP TRIGGER IF EXISTS update_characters_updated_at ON characters;
CREATE TRIGGER update_characters_updated_at
    BEFORE UPDATE ON characters
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_character_profiles_updated_at ON character_profiles;
CREATE TRIGGER update_character_profiles_updated_at
    BEFORE UPDATE ON character_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_roleplay_sessions_updated_at ON roleplay_sessions;
CREATE TRIGGER update_roleplay_sessions_updated_at
    BEFORE UPDATE ON roleplay_sessions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 插入一些示例数据（可选）
-- 注意：这些示例数据需要根据实际的用户ID和知识库ID进行调整

-- 示例角色（需要先有用户和知识库）
-- INSERT INTO characters (name, description, personality, background, speaking_style, status, is_public, user_id, knowledge_base_id)
-- VALUES 
-- ('小助手', '一个友善的AI助手', '友善、耐心、乐于助人', '专业的AI助手，擅长回答各种问题', '温和、专业、清晰', 'ACTIVE', true, 1, 1);

COMMIT;