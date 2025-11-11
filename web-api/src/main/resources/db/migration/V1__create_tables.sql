-- Chat messages
CREATE TABLE IF NOT EXISTS messages (
    id IDENTITY PRIMARY KEY,
    author VARCHAR(64) NOT NULL,
    content VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Users
CREATE TABLE IF NOT EXISTS chat_users (
    id IDENTITY PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(128),
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_admin BOOLEAN DEFAULT FALSE
);

-- Announcements (admin → everyone)
CREATE TABLE IF NOT EXISTS announcements (
    id IDENTITY PRIMARY KEY,
    author VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(2048) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (author) REFERENCES chat_users(user_id)
);

-- Tasks (for task board)
CREATE TABLE IF NOT EXISTS tasks (
    id IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1024),
    status VARCHAR(50) NOT NULL DEFAULT 'todo',
    assignee VARCHAR(64),
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (assignee) REFERENCES chat_users(user_id),
    FOREIGN KEY (created_by) REFERENCES chat_users(user_id)
);

-- Files metadata
CREATE TABLE IF NOT EXISTS files (
    id IDENTITY PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT NOT NULL,
    uploader VARCHAR(64) NOT NULL,
    checksum VARCHAR(64),
    file_path VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (uploader) REFERENCES chat_users(user_id)
);

-- Link previews cache
CREATE TABLE IF NOT EXISTS link_previews (
    id IDENTITY PRIMARY KEY,
    url VARCHAR(1024) NOT NULL UNIQUE,
    title VARCHAR(255),
    description VARCHAR(1024),
    icon_url VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

-- Create admin user for testing
INSERT INTO chat_users (user_id, display_name, is_admin) 
SELECT 'admin', 'Administrator', TRUE
WHERE NOT EXISTS (SELECT 1 FROM chat_users WHERE user_id = 'admin');

