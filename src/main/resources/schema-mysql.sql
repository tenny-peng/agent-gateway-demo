-- MySQL 8.x — run once (create database + tables).
-- Example:
--   mysql -u root -p < src/main/resources/schema-mysql.sql

CREATE DATABASE IF NOT EXISTS agent_gateway_demo
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE agent_gateway_demo;

CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(16) NOT NULL DEFAULT 'USER',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_app_user_username (username)
) ENGINE=InnoDB;

-- One row per distinct conversationId the user has used (generic vs logistics counted separately).
CREATE TABLE IF NOT EXISTS user_conversation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  conversation_id CHAR(36) NOT NULL,
  session_type VARCHAR(16) NOT NULL,
  first_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  title VARCHAR(120) NULL COMMENT 'Conversation title (first user sentence).',
  last_message_at TIMESTAMP NULL DEFAULT NULL COMMENT 'Latest message timestamp.',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Row update timestamp.',
  PRIMARY KEY (id),
  UNIQUE KEY uk_uc (user_id, conversation_id, session_type),
  KEY idx_uc_user_type_lastmsg (user_id, session_type, last_message_at),
  CONSTRAINT fk_uc_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- One row per message in a conversation.
CREATE TABLE IF NOT EXISTS user_conversation_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  conversation_id CHAR(36) NOT NULL,
  session_type VARCHAR(16) NOT NULL,
  seq_no INT NOT NULL,
  role VARCHAR(16) NOT NULL,
  content MEDIUMTEXT NOT NULL,
  tool_name VARCHAR(64) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ucm_seq (user_id, conversation_id, session_type, seq_no),
  KEY idx_ucm_conv_seq (user_id, conversation_id, session_type, seq_no),
  KEY idx_ucm_conv_time (user_id, conversation_id, session_type, created_at),
  CONSTRAINT fk_ucm_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE=InnoDB;
