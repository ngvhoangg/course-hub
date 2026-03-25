ALTER TABLE refresh_tokens ADD COLUMN session_id VARCHAR(255);
CREATE INDEX idx_refresh_tokens_session_id ON refresh_tokens(session_id);