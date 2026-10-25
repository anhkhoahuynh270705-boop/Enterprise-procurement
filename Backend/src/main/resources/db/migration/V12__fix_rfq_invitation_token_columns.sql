DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'rfq_invitation_tokens'
          AND column_name = 'create_by'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'rfq_invitation_tokens'
          AND column_name = 'created_by'
    ) THEN
        ALTER TABLE rfq_invitation_tokens
            RENAME COLUMN create_by TO created_by;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_rfq_invitation_tokens_invited_supplier'
          AND conrelid = 'rfq_invitation_tokens'::regclass
    ) THEN
        ALTER TABLE rfq_invitation_tokens
            ADD CONSTRAINT fk_rfq_invitation_tokens_invited_supplier
                FOREIGN KEY (invited_supplier_id)
                REFERENCES rfq_invited_suppliers(id);
    END IF;
END $$;
