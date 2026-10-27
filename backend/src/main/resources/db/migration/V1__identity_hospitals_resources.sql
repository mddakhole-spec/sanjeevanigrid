-- V1: Enable PostGIS and create Identity & Hospital schema
-- SanjeevaniGrid Phase 1 Migration

-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- IDENTITY MODULE
-- ============================================================

-- Users table
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone           VARCHAR(15) UNIQUE,
    email           VARCHAR(255) UNIQUE,
    password_hash   VARCHAR(255),
    full_name       VARCHAR(255) NOT NULL,
    preferred_lang  VARCHAR(5) NOT NULL DEFAULT 'en',
    auth_provider   VARCHAR(20) NOT NULL DEFAULT 'PHONE',
    onboarding_state VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    profile_complete BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Roles table
CREATE TABLE roles (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(30) NOT NULL UNIQUE
);

-- User-Role join table
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- OTP table for phone-based auth
CREATE TABLE otp_tokens (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone       VARCHAR(15) NOT NULL,
    code        VARCHAR(10) NOT NULL,
    attempts    INT NOT NULL DEFAULT 0,
    verified    BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Refresh tokens
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Onboarding approval queue
CREATE TABLE approval_queue (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id),
    requested_role VARCHAR(30) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by UUID REFERENCES users(id),
    review_note TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMP WITH TIME ZONE
);

-- Audit log
CREATE TABLE audit_log (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_id    UUID REFERENCES users(id),
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id   UUID,
    details     JSONB,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- ============================================================
-- HOSPITAL / WARDS / RESOURCES
-- ============================================================

-- Hospitals
CREATE TABLE hospitals (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(255) NOT NULL,
    licence_number  VARCHAR(100),
    address         TEXT,
    district        VARCHAR(100),
    state           VARCHAR(100) DEFAULT 'Maharashtra',
    phone           VARCHAR(15),
    email           VARCHAR(255),
    location        GEOMETRY(Point, 4326),
    verified        BOOLEAN NOT NULL DEFAULT FALSE,
    admin_user_id   UUID REFERENCES users(id),
    api_key_hash    VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- GiST index on hospital location for spatial queries
CREATE INDEX idx_hospitals_location ON hospitals USING GIST (location);

-- Wards
CREATE TABLE wards (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hospital_id     UUID NOT NULL REFERENCES hospitals(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    ward_type       VARCHAR(30) NOT NULL,
    total_beds      INT NOT NULL DEFAULT 0,
    total_icu_beds  INT NOT NULL DEFAULT 0,
    total_ventilators INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE(hospital_id, name)
);

-- Resource snapshots (current state of resources per ward)
CREATE TABLE ward_resources (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ward_id             UUID NOT NULL REFERENCES wards(id) ON DELETE CASCADE,
    available_beds      INT NOT NULL DEFAULT 0,
    occupied_beds       INT NOT NULL DEFAULT 0,
    available_icu       INT NOT NULL DEFAULT 0,
    occupied_icu        INT NOT NULL DEFAULT 0,
    available_ventilators INT NOT NULL DEFAULT 0,
    in_use_ventilators  INT NOT NULL DEFAULT 0,
    staff_on_duty       INT NOT NULL DEFAULT 0,
    oxygen_level_pct    DECIMAL(5,2) NOT NULL DEFAULT 100.0,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Notifications (in-app)
CREATE TABLE notifications (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel     VARCHAR(20) NOT NULL DEFAULT 'IN_APP',
    title       VARCHAR(255) NOT NULL,
    body        TEXT,
    read        BOOLEAN NOT NULL DEFAULT FALSE,
    metadata    JSONB,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- ============================================================
-- SEED ROLES
-- ============================================================
INSERT INTO roles (id, name) VALUES
    (uuid_generate_v4(), 'CITIZEN'),
    (uuid_generate_v4(), 'DOCTOR'),
    (uuid_generate_v4(), 'HOSPITAL_ADMIN'),
    (uuid_generate_v4(), 'DONOR'),
    (uuid_generate_v4(), 'CAMP_WORKER'),
    (uuid_generate_v4(), 'CAMP_ORGANIZER'),
    (uuid_generate_v4(), 'PLATFORM_ADMIN');
