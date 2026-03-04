-- BidMart Backend Schema
-- ===== Email verification =====
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS app_email_verifications (
    token UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE
                                                                                                            );

CREATE INDEX IF NOT EXISTS idx_email_verifications_user_id ON app_email_verifications(user_id);


-- ===== 2FA setup + login challenges =====
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS mfa_method VARCHAR(16);       -- 'EMAIL' | 'TOTP'
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS totp_secret TEXT;             -- future

CREATE TABLE IF NOT EXISTS app_mfa_challenges (
                                                  id UUID PRIMARY KEY,
                                                  user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    method VARCHAR(16) NOT NULL,                 -- 'EMAIL' | 'TOTP'
    code_hash VARCHAR(255) NOT NULL,             -- bcrypt hash of OTP
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
                                                                                                       attempts INT NOT NULL DEFAULT 0
                                                                                                       );

CREATE INDEX IF NOT EXISTS idx_mfa_challenges_user_id ON app_mfa_challenges(user_id);


-- ===== Permission model =====
CREATE TABLE IF NOT EXISTS app_roles (
                                         name VARCHAR(64) PRIMARY KEY                -- e.g. ADMIN, SELLER, BUYER, CUSTOM_ROLE
    );

CREATE TABLE IF NOT EXISTS app_permissions (
                                               perm_key VARCHAR(128) PRIMARY KEY,          -- e.g. bid:place
    description TEXT
    );

CREATE TABLE IF NOT EXISTS app_role_permissions (
                                                    role_name VARCHAR(64) NOT NULL REFERENCES app_roles(name) ON DELETE CASCADE,
    perm_key VARCHAR(128) NOT NULL REFERENCES app_permissions(perm_key) ON DELETE CASCADE,
    PRIMARY KEY(role_name, perm_key)
    );

-- seed built-in roles if missing
INSERT INTO app_roles(name) VALUES ('ADMIN')  ON CONFLICT DO NOTHING;
INSERT INTO app_roles(name) VALUES ('SELLER') ON CONFLICT DO NOTHING;
INSERT INTO app_roles(name) VALUES ('BUYER')  ON CONFLICT DO NOTHING;

-- ===== Email verification =====
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS app_email_verifications (
                                                       token UUID PRIMARY KEY,
                                                       user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE
                                                                                                            );

CREATE INDEX IF NOT EXISTS idx_email_verifications_user_id ON app_email_verifications(user_id);

-- ===== 2FA (MFA) =====
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS mfa_method VARCHAR(16);   -- 'EMAIL' | 'TOTP'

CREATE TABLE IF NOT EXISTS app_mfa_challenges (
                                                  id UUID PRIMARY KEY,
                                                  user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    method VARCHAR(16) NOT NULL,                 -- 'EMAIL' | 'TOTP'
    code_hash VARCHAR(255) NOT NULL,             -- bcrypt hash OTP
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
                                                                                                       attempts INT NOT NULL DEFAULT 0
                                                                                                       );

CREATE INDEX IF NOT EXISTS idx_mfa_challenges_user_id ON app_mfa_challenges(user_id);

-- ===== Profile fields =====
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS display_name VARCHAR(128);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS photo_url TEXT;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS shipping_address TEXT;

-- ===== Permission / RBAC =====
CREATE TABLE IF NOT EXISTS app_roles (
                                         name VARCHAR(64) PRIMARY KEY
    );

CREATE TABLE IF NOT EXISTS app_permissions (
                                               perm_key VARCHAR(128) PRIMARY KEY,
    description TEXT
    );

CREATE TABLE IF NOT EXISTS app_role_permissions (
                                                    role_name VARCHAR(64) NOT NULL REFERENCES app_roles(name) ON DELETE CASCADE,
    perm_key VARCHAR(128) NOT NULL REFERENCES app_permissions(perm_key) ON DELETE CASCADE,
    PRIMARY KEY(role_name, perm_key)
    );

-- seed built-in roles
INSERT INTO app_roles(name) VALUES ('ADMIN')  ON CONFLICT DO NOTHING;
INSERT INTO app_roles(name) VALUES ('SELLER') ON CONFLICT DO NOTHING;
INSERT INTO app_roles(name) VALUES ('BUYER')  ON CONFLICT DO NOTHING;


-- ===== Profile fields =====
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS display_name VARCHAR(128);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS photo_url TEXT;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS shipping_address TEXT;

-- ===== Permission / RBAC =====
CREATE TABLE IF NOT EXISTS app_roles (
                                         name VARCHAR(64) PRIMARY KEY
    );

CREATE TABLE IF NOT EXISTS app_permissions (
                                               perm_key VARCHAR(128) PRIMARY KEY,
    description TEXT
    );

CREATE TABLE IF NOT EXISTS app_role_permissions (
                                                    role_name VARCHAR(64) NOT NULL REFERENCES app_roles(name) ON DELETE CASCADE,
    perm_key VARCHAR(128) NOT NULL REFERENCES app_permissions(perm_key) ON DELETE CASCADE,
    PRIMARY KEY(role_name, perm_key)
    );

INSERT INTO app_roles(name) VALUES ('ADMIN')  ON CONFLICT DO NOTHING;
INSERT INTO app_roles(name) VALUES ('SELLER') ON CONFLICT DO NOTHING;
INSERT INTO app_roles(name) VALUES ('BUYER')  ON CONFLICT DO NOTHING;

-- Optional seed (biar langsung bisa demo permission)
INSERT INTO app_permissions(perm_key, description) VALUES ('bid:place','Place bid') ON CONFLICT DO NOTHING;
INSERT INTO app_permissions(perm_key, description) VALUES ('auction:create','Create auction') ON CONFLICT DO NOTHING;
INSERT INTO app_permissions(perm_key, description) VALUES ('wallet:read','Read wallet') ON CONFLICT DO NOTHING;

-- Default mapping (boleh kamu edit via admin runtime)
INSERT INTO app_role_permissions(role_name, perm_key) VALUES ('BUYER','bid:place') ON CONFLICT DO NOTHING;
INSERT INTO app_role_permissions(role_name, perm_key) VALUES ('SELLER','auction:create') ON CONFLICT DO NOTHING;
INSERT INTO app_role_permissions(role_name, perm_key) VALUES ('ADMIN','bid:place') ON CONFLICT DO NOTHING;
INSERT INTO app_role_permissions(role_name, perm_key) VALUES ('ADMIN','auction:create') ON CONFLICT DO NOTHING;
INSERT INTO app_role_permissions(role_name, perm_key) VALUES ('ADMIN','wallet:read') ON CONFLICT DO NOTHING;