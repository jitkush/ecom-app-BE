CREATE TABLE auth.authentication_session (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    account_id BIGINT NOT NULL,
    secret_hash VARCHAR(64) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    last_activity_at TIMESTAMPTZ NOT NULL,
    idle_expires_at TIMESTAMPTZ NOT NULL,
    absolute_expires_at TIMESTAMPTZ NOT NULL,

    revoked_at TIMESTAMPTZ,
    revocation_reason VARCHAR(100),

    CONSTRAINT pk_authentication_session
        PRIMARY KEY (id),

    CONSTRAINT fk_authentication_session_account
        FOREIGN KEY (account_id)
        REFERENCES auth.account (id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_authentication_session_secret_hash
        UNIQUE (secret_hash),

    CONSTRAINT chk_authentication_session_secret_hash
        CHECK (secret_hash ~ '^[0-9a-f]{64}$'),

    CONSTRAINT chk_authentication_session_timestamps
        CHECK (
            created_at <= last_activity_at
            AND last_activity_at < idle_expires_at
            AND idle_expires_at <= absolute_expires_at
        ),

    CONSTRAINT chk_authentication_session_revocation
        CHECK (
            (revoked_at IS NULL AND revocation_reason IS NULL)
            OR (
                revoked_at IS NOT NULL
                AND revoked_at >= created_at
                AND revocation_reason IS NOT NULL
                AND char_length(btrim(revocation_reason)) > 0
            )
        )
);

CREATE INDEX ix_authentication_session_account_id
    ON auth.authentication_session (account_id);