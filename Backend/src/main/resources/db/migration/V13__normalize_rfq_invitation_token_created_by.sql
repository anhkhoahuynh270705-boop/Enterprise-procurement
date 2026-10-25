DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'rfq_invitation_tokens'
          AND column_name = 'create_by'
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'rfq_invitation_tokens'
          AND column_name = 'created_by'
    ) THEN
        UPDATE rfq_invitation_tokens
        SET created_by = COALESCE(created_by, create_by)
        WHERE created_by IS NULL;

        ALTER TABLE rfq_invitation_tokens
            DROP COLUMN create_by;
    ELSIF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'rfq_invitation_tokens'
          AND column_name = 'create_by'
    ) THEN
        ALTER TABLE rfq_invitation_tokens
            RENAME COLUMN create_by TO created_by;
    END IF;
END $$;
