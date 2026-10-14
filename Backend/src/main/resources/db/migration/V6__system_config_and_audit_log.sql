-- V6: System config key-value store and audit log table

CREATE TABLE IF NOT EXISTS system_configs (
    config_key   VARCHAR(100) PRIMARY KEY,
    config_value TEXT         NOT NULL,
    updated_at   TIMESTAMP(6) WITHOUT TIME ZONE,
    updated_by   VARCHAR(100)
);

-- Seed defaults (INSERT … ON CONFLICT DO NOTHING so re-runs are idempotent)
INSERT INTO system_configs (config_key, config_value, updated_at, updated_by) VALUES
    ('security.rateLimitAuthSpam',        '30',                     NOW(), 'system'),
    ('security.rateLimitGeneralApi',      '120',                    NOW(), 'system'),
    ('security.rateLimitFaceVerify',      '10',                     NOW(), 'system'),
    ('security.faceMatchThreshold',       '85',                     NOW(), 'system'),
    ('security.faceMinImagesEnroll',      '3',                      NOW(), 'system'),
    ('security.jwtSessionTimeoutMinutes', '60',                     NOW(), 'system'),
    ('security.maxFailedLoginAttempts',   '5',                      NOW(), 'system'),
    ('security.enableBruteForceProtection', 'true',                 NOW(), 'system'),
    ('procurement.autoApproveThreshold',      '5000000',            NOW(), 'system'),
    ('procurement.defaultCurrency',           'VND',                NOW(), 'system'),
    ('procurement.ticketCodePrefix',          'YCM',                NOW(), 'system'),
    ('procurement.purchaseOrderCodePrefix',   'PO',                 NOW(), 'system'),
    ('procurement.contractCodePrefix',        'HĐMB',               NOW(), 'system'),
    ('procurement.requireSupplierProposal',   'true',               NOW(), 'system'),
    ('procurement.proposalMinAmountThreshold','20000000',           NOW(), 'system'),
    ('procurement.camundaAutoAssignChecker',  'true',               NOW(), 'system'),
    ('notification.notifyMakerOnApproval',    'true',               NOW(), 'system'),
    ('notification.notifyMakerOnRejection',   'true',               NOW(), 'system'),
    ('notification.notifyCheckerNewTicket',   'true',               NOW(), 'system'),
    ('notification.notifyContractExpiringDays','30',                NOW(), 'system'),
    ('notification.notifyAdminOnRateLimit',   'true',               NOW(), 'system'),
    ('notification.enableEmailDigest',        'false',              NOW(), 'system')
ON CONFLICT (config_key) DO NOTHING;

CREATE TABLE IF NOT EXISTS audit_logs (
    id              UUID         PRIMARY KEY,
    log_code        VARCHAR(20)  NOT NULL,
    occurred_at     TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    username        VARCHAR(255),
    user_full_name  VARCHAR(255),
    action          VARCHAR(500) NOT NULL,
    module          VARCHAR(50)  NOT NULL,
    module_label    VARCHAR(100),
    ip_address      VARCHAR(64),
    status          VARCHAR(20)  NOT NULL,
    description     TEXT,
    user_agent      VARCHAR(500),
    http_method     VARCHAR(10),
    endpoint        VARCHAR(500),
    http_status     INTEGER,
    response_time_ms INTEGER,
    payload_summary TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_occurred ON audit_logs (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_module   ON audit_logs (module, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_username ON audit_logs (username, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_status   ON audit_logs (status, occurred_at DESC);
