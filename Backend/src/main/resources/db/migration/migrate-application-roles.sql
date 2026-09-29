BEGIN;

ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS email_verification_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS role VARCHAR(255) NOT NULL DEFAULT 'USER';

UPDATE app_users
SET role = CASE
    WHEN role IS NULL OR trim(role) = '' THEN 'USER'
    WHEN upper(trim(role)) IN ('MAKER', 'ROLE_MAKER', 'ROLE_USER', 'USER') THEN 'USER'
    WHEN upper(trim(role)) IN ('ADMIN', 'ROLE_ADMIN') THEN 'ADMIN'
    WHEN upper(trim(role)) IN ('CHECKER', 'ROLE_CHECKER') THEN 'CHECKER'
    ELSE role
END;

CREATE INDEX IF NOT EXISTS idx_app_users_enabled_deleted
    ON app_users (enabled, deleted);

COMMIT;

-- Review any unrecognized legacy roles; these accounts are denied API access.
SELECT id, username, role
FROM app_users
WHERE role NOT IN ('ADMIN', 'USER', 'CHECKER')
ORDER BY username;
