-- User profile extensions (avatar, contact, employee metadata) and role normalization

ALTER TABLE app_users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(2048);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS avatar_data BYTEA;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS avatar_content_type VARCHAR(50);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS phone VARCHAR(30);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS date_of_birth DATE;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS gender VARCHAR(20);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS address VARCHAR(500);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS employee_id VARCHAR(50);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS department VARCHAR(255);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS job_title VARCHAR(255);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS office_location VARCHAR(255);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS manager VARCHAR(255);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS employee_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS ux_app_users_employee_id ON app_users(employee_id);

-- Normalize legacy roles if present
UPDATE app_users
SET role = CASE
    WHEN role IS NULL OR trim(role) = '' THEN 'USER'
    WHEN upper(trim(role)) IN ('MAKER', 'ROLE_MAKER', 'ROLE_USER', 'USER') THEN 'USER'
    WHEN upper(trim(role)) IN ('ADMIN', 'ROLE_ADMIN') THEN 'ADMIN'
    WHEN upper(trim(role)) IN ('CHECKER', 'ROLE_CHECKER') THEN 'CHECKER'
    ELSE role
END;
