CREATE SCHEMA IF NOT EXISTS auth;

-- =========================================================
-- ACCOUNT
-- Shared login identity for CUSTOMER, OPS and ADMIN
-- =========================================================

CREATE TABLE auth.account (
    id BIGINT GENERATED ALWAYS AS IDENTITY,

    public_id UUID NOT NULL,

    email VARCHAR(254),
    mobile VARCHAR(16),
    password_hash VARCHAR(255),

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    email_verified_at TIMESTAMPTZ,
    mobile_verified_at TIMESTAMPTZ,

    failed_login_count SMALLINT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,

    credentials_changed_at TIMESTAMPTZ,
    security_version INTEGER NOT NULL DEFAULT 1,
    last_login_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_account
        PRIMARY KEY (id),

    CONSTRAINT uk_account_public_id
        UNIQUE (public_id),

    CONSTRAINT uk_account_email
        UNIQUE (email),

    CONSTRAINT uk_account_mobile
        UNIQUE (mobile),

    CONSTRAINT chk_account_identity
        CHECK (email IS NOT NULL OR mobile IS NOT NULL),

    CONSTRAINT chk_account_email_normalized
        CHECK (email IS NULL OR email = LOWER(email)),

    CONSTRAINT chk_account_status
        CHECK (
            status IN (
                'PENDING',
                'ACTIVE',
                'LOCKED',
                'DISABLED'
            )
        ),

    CONSTRAINT chk_account_failed_login_count
        CHECK (failed_login_count >= 0),

    CONSTRAINT chk_account_security_version
        CHECK (security_version > 0)
);

-- =========================================================
-- ROLE
-- Groups accounts by responsibility
-- =========================================================

CREATE TABLE auth.role (
    id SMALLINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    system_role BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_role
        PRIMARY KEY (id),

    CONSTRAINT uk_role_code
        UNIQUE (code)
);

-- =========================================================
-- ACCOUNT_ROLE
-- Assigns roles to accounts
-- =========================================================

CREATE TABLE auth.account_role (
    account_id BIGINT NOT NULL,
    role_id SMALLINT NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by_account_id BIGINT,

    CONSTRAINT pk_account_role
        PRIMARY KEY (account_id, role_id),

    CONSTRAINT fk_account_role_account
        FOREIGN KEY (account_id)
        REFERENCES auth.account(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_account_role_role
        FOREIGN KEY (role_id)
        REFERENCES auth.role(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_account_role_assigned_by
        FOREIGN KEY (assigned_by_account_id)
        REFERENCES auth.account(id)
        ON DELETE SET NULL
);

-- =========================================================
-- BUILT-IN ROLE INSERTION
-- =========================================================

INSERT INTO auth.role (
    id,
    code,
    display_name,
    description,
    system_role
)
VALUES
    (
        700,
        'CUSTOMER',
        'Customer',
        'Customer who accesses the Aparna Jewels storefront',
        TRUE
    ),
    (
        770,
        'OPS',
        'Operations',
        'Staff responsible for operational activities',
        TRUE
    ),
    (
        777,
        'ADMIN',
        'Administrator',
        'Administrator with privileged management access',
        TRUE
    );