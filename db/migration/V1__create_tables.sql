-- Placeholder migration for message and user storage
CREATE TABLE IF NOT EXISTS messages (
    id IDENTITY PRIMARY KEY,
    author VARCHAR(64) NOT NULL,
    content VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chat_users (
    id IDENTITY PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(128)
);
