ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS avatar_public_id VARCHAR(255);
