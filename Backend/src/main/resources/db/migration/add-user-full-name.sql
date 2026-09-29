DO $$
BEGIN
    IF to_regclass('app_users') IS NOT NULL THEN
        ALTER TABLE app_users ADD COLUMN IF NOT EXISTS full_name VARCHAR(255);
        UPDATE app_users
        SET full_name = COALESCE(NULLIF(BTRIM(CONCAT_WS(' ', NULLIF(BTRIM(last_name), ''),
                                                          NULLIF(BTRIM(first_name), ''))), ''),
                                 'Chưa cập nhật họ tên')
        WHERE full_name IS NULL OR BTRIM(full_name) = '';
        ALTER TABLE app_users ALTER COLUMN full_name SET NOT NULL;
    END IF;
END
$$;
