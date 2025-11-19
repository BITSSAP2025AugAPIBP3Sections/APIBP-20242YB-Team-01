-- Flyway migration: create users and refresh_tokens tables for auth service (PostgreSQL)

-- users table
CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  full_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  role VARCHAR(50) NOT NULL DEFAULT 'BUYER',
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- refresh_tokens table: stores opaque refresh tokens (rotation + revocation)
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id BIGSERIAL PRIMARY KEY,
  token VARCHAR(512) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
  revoked BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes to speed common queries
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expiry_date ON refresh_tokens(expiry_date);
