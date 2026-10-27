-- V2: Service Coordination (Doctors, Availability, Slots, Appointments, Waitlist)
-- SanjeevaniGrid Phase 3 Migration

-- Doctors table
CREATE TABLE IF NOT EXISTS doctors (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id                     UUID REFERENCES users(id) ON DELETE SET NULL,
    hospital_id                 UUID REFERENCES hospitals(id) ON DELETE CASCADE,
    full_name                   VARCHAR(255) NOT NULL,
    specialty                   VARCHAR(100) NOT NULL,
    qualification               VARCHAR(100) DEFAULT 'MBBS, MD',
    experience_years            INT DEFAULT 5,
    consultation_fee            DECIMAL(10,2) DEFAULT 0.00,
    consultation_duration_mins  INT NOT NULL DEFAULT 15,
    max_daily_patients          INT NOT NULL DEFAULT 24,
    rolling_avg_consultation_mins DOUBLE PRECISION NOT NULL DEFAULT 15.0,
    completed_consultations_count INT NOT NULL DEFAULT 0,
    active                      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Doctor Weekly Availability Templates (Tap-grid schedule)
-- Day of week: 1 = Monday, 2 = Tuesday, ..., 7 = Sunday
CREATE TABLE IF NOT EXISTS doctor_availabilities (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    doctor_id           UUID NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    day_of_week         INT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time          TIME NOT NULL,
    end_time            TIME NOT NULL,
    break_start_time    TIME,
    break_end_time      TIME,
    slot_duration_mins  INT NOT NULL DEFAULT 15,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (doctor_id, day_of_week)
);

-- Appointment Slots (Specific date instances)
CREATE TABLE IF NOT EXISTS appointment_slots (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    doctor_id           UUID NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    hospital_id         UUID REFERENCES hospitals(id) ON DELETE CASCADE,
    slot_date           DATE NOT NULL,
    start_time          TIME NOT NULL,
    end_time            TIME NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    is_surge_reserved   BOOLEAN NOT NULL DEFAULT FALSE,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_doctor_slot UNIQUE (doctor_id, slot_date, start_time)
);

CREATE INDEX IF NOT EXISTS idx_slots_doctor_date ON appointment_slots(doctor_id, slot_date);
CREATE INDEX IF NOT EXISTS idx_slots_status ON appointment_slots(status);

-- Appointments
CREATE TABLE IF NOT EXISTS appointments (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    slot_id                 UUID REFERENCES appointment_slots(id) ON DELETE SET NULL,
    doctor_id               UUID NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    hospital_id             UUID REFERENCES hospitals(id) ON DELETE CASCADE,
    patient_id              UUID REFERENCES users(id) ON DELETE SET NULL,
    patient_name            VARCHAR(255) NOT NULL,
    patient_phone           VARCHAR(20) NOT NULL,
    patient_age             INT,
    patient_gender          VARCHAR(10),
    chief_complaint         TEXT,
    priority                VARCHAR(20) NOT NULL DEFAULT 'ROUTINE',
    priority_score          INT NOT NULL DEFAULT 1,
    status                  VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    token_number            VARCHAR(30) NOT NULL,
    queue_position          INT NOT NULL DEFAULT 0,
    estimated_wait_mins     INT NOT NULL DEFAULT 0,
    is_walk_in              BOOLEAN NOT NULL DEFAULT FALSE,
    appointment_date        DATE NOT NULL,
    start_time              TIME NOT NULL,
    end_time                TIME NOT NULL,
    bumped_reason           TEXT,
    bumped_at               TIMESTAMP WITH TIME ZONE,
    idempotency_key         VARCHAR(128) UNIQUE,
    reminder_sent           BOOLEAN NOT NULL DEFAULT FALSE,
    checked_in_at           TIMESTAMP WITH TIME ZONE,
    consultation_start_at   TIMESTAMP WITH TIME ZONE,
    consultation_end_at     TIMESTAMP WITH TIME ZONE,
    version                 BIGINT NOT NULL DEFAULT 0,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_appointments_doctor_date ON appointments(doctor_id, appointment_date);
CREATE INDEX IF NOT EXISTS idx_appointments_status ON appointments(status);
CREATE INDEX IF NOT EXISTS idx_appointments_patient_phone ON appointments(patient_phone);

-- Appointment Waitlist
CREATE TABLE IF NOT EXISTS appointment_waitlist (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    doctor_id           UUID NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    appointment_date    DATE NOT NULL,
    patient_id          UUID REFERENCES users(id) ON DELETE SET NULL,
    patient_name        VARCHAR(255) NOT NULL,
    patient_phone       VARCHAR(20) NOT NULL,
    priority            VARCHAR(20) NOT NULL DEFAULT 'ROUTINE',
    status              VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    promoted_at         TIMESTAMP WITH TIME ZONE
);

-- Seed Default Hospital in Yavatmal District
INSERT INTO hospitals (id, name, district, address, verified)
VALUES ('a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 'Yavatmal District General Hospital', 'Yavatmal', 'Civil Lines, Yavatmal, Maharashtra 445001', TRUE)
ON CONFLICT (id) DO NOTHING;

-- Seed Initial Doctors in Yavatmal District Hospital
INSERT INTO doctors (id, hospital_id, full_name, specialty, qualification, experience_years, consultation_fee, consultation_duration_mins, max_daily_patients, rolling_avg_consultation_mins)
VALUES
    ('d1111111-1111-1111-1111-111111111111', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 'Dr. Priya Sharma', 'GENERAL_MEDICINE', 'MBBS, MD (Medicine)', 8, 0.00, 15, 24, 14.5),
    ('d2222222-2222-2222-2222-222222222222', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 'Dr. Rajesh Deshmukh', 'PEDIATRICS', 'MBBS, DCH, DNB', 12, 0.00, 20, 20, 18.0),
    ('d3333333-3333-3333-3333-333333333333', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 'Dr. Anand Patil', 'ORTHOPEDICS', 'MBBS, MS (Ortho)', 15, 0.00, 15, 20, 15.0),
    ('d4444444-4444-4444-4444-444444444444', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 'Dr. Sneha Kulkarni', 'GYNECOLOGY', 'MBBS, DGO, MD', 9, 0.00, 20, 18, 19.5),
    ('d5555555-5555-5555-5555-555555555555', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 'Dr. Vikram Joshi', 'CARDIOLOGY', 'MBBS, MD, DM (Cardio)', 14, 0.00, 20, 16, 22.0),
    ('d6666666-6666-6666-6666-666666666666', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 'Dr. Sunita Gaikwad', 'EMERGENCY', 'MBBS, MEM (Emergency)', 7, 0.00, 15, 30, 12.0)
ON CONFLICT (id) DO NOTHING;

-- Seed default Monday-Friday availability for Dr. Priya Sharma
INSERT INTO doctor_availabilities (id, doctor_id, day_of_week, start_time, end_time, break_start_time, break_end_time, slot_duration_mins)
VALUES
    ('a1111111-1111-1111-1111-111111111101', 'd1111111-1111-1111-1111-111111111111', 1, '09:00:00', '17:00:00', '13:00:00', '14:00:00', 15),
    ('a1111111-1111-1111-1111-111111111102', 'd1111111-1111-1111-1111-111111111111', 2, '09:00:00', '17:00:00', '13:00:00', '14:00:00', 15),
    ('a1111111-1111-1111-1111-111111111103', 'd1111111-1111-1111-1111-111111111111', 3, '09:00:00', '17:00:00', '13:00:00', '14:00:00', 15),
    ('a1111111-1111-1111-1111-111111111104', 'd1111111-1111-1111-1111-111111111111', 4, '09:00:00', '17:00:00', '13:00:00', '14:00:00', 15),
    ('a1111111-1111-1111-1111-111111111105', 'd1111111-1111-1111-1111-111111111111', 5, '09:00:00', '17:00:00', '13:00:00', '14:00:00', 15)
ON CONFLICT (doctor_id, day_of_week) DO NOTHING;
