-- V4: Medical Camp Records (PWA, Attendees, Vitals, Vaccines, Stock) & Equity Map (Villages, GIS Access, Next-Camp Recommender)
-- SanjeevaniGrid Phase 5 Migration

-- 1. Villages Table for GIS Equity Map
CREATE TABLE IF NOT EXISTS villages (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name                        VARCHAR(255) NOT NULL,
    district                    VARCHAR(100) NOT NULL DEFAULT 'Yavatmal',
    taluka                      VARCHAR(100) NOT NULL,
    population                  INT NOT NULL,
    latitude                    DOUBLE PRECISION NOT NULL,
    longitude                   DOUBLE PRECISION NOT NULL,
    location                    GEOMETRY(Point, 4326),
    nearest_hospital_id         UUID REFERENCES hospitals(id) ON DELETE SET NULL,
    nearest_hospital_name       VARCHAR(255),
    distance_to_hospital_km     DOUBLE PRECISION NOT NULL,
    travel_time_minutes         INT NOT NULL,
    doctor_availability_score   DOUBLE PRECISION NOT NULL DEFAULT 0.5,
    last_camp_date              DATE,
    days_since_last_camp        INT NOT NULL DEFAULT 180,
    access_score                DOUBLE PRECISION NOT NULL DEFAULT 45.0,
    vulnerability_category      VARCHAR(20) NOT NULL DEFAULT 'YELLOW', -- RED (<40), YELLOW (40-70), GREEN (>70)
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_villages_location ON villages USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_villages_category ON villages (vulnerability_category);
CREATE INDEX IF NOT EXISTS idx_villages_taluka ON villages (taluka);

-- 2. Medical Camps Table
CREATE TABLE IF NOT EXISTS medical_camps (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name                VARCHAR(255) NOT NULL,
    organizer_name      VARCHAR(255) NOT NULL DEFAULT 'District Health Administration',
    camp_type           VARCHAR(50) NOT NULL DEFAULT 'GENERAL_HEALTH',
    venue               VARCHAR(255) NOT NULL,
    village_id          UUID REFERENCES villages(id) ON DELETE SET NULL,
    village_name        VARCHAR(255),
    district            VARCHAR(100) NOT NULL DEFAULT 'Yavatmal',
    latitude            DOUBLE PRECISION NOT NULL,
    longitude           DOUBLE PRECISION NOT NULL,
    location            GEOMETRY(Point, 4326),
    camp_date           DATE NOT NULL,
    start_time          TIME NOT NULL DEFAULT '09:00:00',
    end_time            TIME NOT NULL DEFAULT '17:00:00',
    doctor_in_charge    VARCHAR(255) DEFAULT 'Dr. Nilesh Patil, MD',
    target_attendees    INT NOT NULL DEFAULT 150,
    registered_count    INT NOT NULL DEFAULT 0,
    attended_count      INT NOT NULL DEFAULT 0,
    status              VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    shareable_slug      VARCHAR(100) UNIQUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_medical_camps_date ON medical_camps (camp_date);
CREATE INDEX IF NOT EXISTS idx_medical_camps_status ON medical_camps (status);
CREATE INDEX IF NOT EXISTS idx_medical_camps_location ON medical_camps USING GIST (location);

-- 3. Camp Attendees Table (Supports Offline Idempotent Sync via client_sync_id)
CREATE TABLE IF NOT EXISTS camp_attendees (
    id                  UUID PRIMARY KEY,
    camp_id             UUID NOT NULL REFERENCES medical_camps(id) ON DELETE CASCADE,
    client_sync_id      UUID NOT NULL UNIQUE,
    full_name           VARCHAR(255) NOT NULL,
    phone               VARCHAR(20) NOT NULL,
    dob                 DATE,
    age                 INT,
    gender              VARCHAR(20) NOT NULL DEFAULT 'OTHER',
    qr_code_id          VARCHAR(100),
    village             VARCHAR(255),
    vital_bp            VARCHAR(20),
    vital_pulse         INT,
    vital_temp          DOUBLE PRECISION,
    vital_spo2          INT,
    triage_level        VARCHAR(20) NOT NULL DEFAULT 'ROUTINE', -- ROUTINE, URGENT, EMERGENCY
    symptoms            TEXT,
    notes               TEXT,
    registered_offline  BOOLEAN NOT NULL DEFAULT FALSE,
    synced_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_camp_attendees_camp ON camp_attendees (camp_id);
CREATE INDEX IF NOT EXISTS idx_camp_attendees_phone ON camp_attendees (phone);
CREATE INDEX IF NOT EXISTS idx_camp_attendees_sync ON camp_attendees (client_sync_id);
CREATE INDEX IF NOT EXISTS idx_camp_attendees_triage ON camp_attendees (triage_level);

-- 4. Camp Services Log Table
CREATE TABLE IF NOT EXISTS camp_services_log (
    id                  UUID PRIMARY KEY,
    camp_id             UUID NOT NULL REFERENCES medical_camps(id) ON DELETE CASCADE,
    attendee_id         UUID NOT NULL REFERENCES camp_attendees(id) ON DELETE CASCADE,
    client_sync_id      UUID NOT NULL UNIQUE,
    service_type        VARCHAR(50) NOT NULL, -- CONSULTATION, VACCINATION, LAB_TEST, MEDICINE_DISPENSE, REFERRAL
    details             TEXT,
    doctor_notes        TEXT,
    performed_by        VARCHAR(255),
    performed_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_services_log_camp ON camp_services_log (camp_id);
CREATE INDEX IF NOT EXISTS idx_services_log_attendee ON camp_services_log (attendee_id);

-- 5. Camp Vaccinations Table
CREATE TABLE IF NOT EXISTS camp_vaccinations (
    id                  UUID PRIMARY KEY,
    camp_id             UUID NOT NULL REFERENCES medical_camps(id) ON DELETE CASCADE,
    attendee_id         UUID NOT NULL REFERENCES camp_attendees(id) ON DELETE CASCADE,
    client_sync_id      UUID NOT NULL UNIQUE,
    vaccine_name        VARCHAR(100) NOT NULL,
    dose_number         INT NOT NULL DEFAULT 1,
    administered_date   DATE NOT NULL,
    next_due_date       DATE,
    batch_number        VARCHAR(50),
    administered_by     VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_vaccinations_camp ON camp_vaccinations (camp_id);
CREATE INDEX IF NOT EXISTS idx_vaccinations_attendee ON camp_vaccinations (attendee_id);

-- 6. Camp Medicine Stock Table
CREATE TABLE IF NOT EXISTS camp_medicine_stock (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    camp_id             UUID NOT NULL REFERENCES medical_camps(id) ON DELETE CASCADE,
    medicine_name       VARCHAR(255) NOT NULL,
    unit                VARCHAR(50) NOT NULL DEFAULT 'tablets',
    initial_quantity    INT NOT NULL DEFAULT 0,
    dispensed_quantity  INT NOT NULL DEFAULT 0,
    remaining_quantity  INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_camp_medicine UNIQUE (camp_id, medicine_name)
);

CREATE INDEX IF NOT EXISTS idx_camp_stock_camp ON camp_medicine_stock (camp_id);

-- ============================================================
-- SEED DATA: 8 Additional Real Hospitals in Yavatmal District
-- ============================================================
INSERT INTO hospitals (id, name, district, address, phone, email, verified, location, api_key_hash)
VALUES
    ('b3c4d5e6-f7a8-9b0c-1d2e-3f4a5b6c7d8e', 'Darwha Sub-District Hospital', 'Yavatmal', 'Hospital Ward, Darwha, Yavatmal 445202', '+917238255100', 'darwha.sdh@sanjeevanigrid.in', TRUE, ST_SetSRID(ST_MakePoint(77.7680, 20.3150), 4326), 'a344554a704b2f6223c76398ae3901c11f342b194bf5204783e14cc5a120154e'),
    ('b4c5d6e7-f8a9-0b1c-2d3e-4f5a6b7c8d9e', 'Wani Sub-District Hospital', 'Yavatmal', 'Civil Lines, Wani, Yavatmal 445304', '+917239226200', 'wani.sdh@sanjeevanigrid.in', TRUE, ST_SetSRID(ST_MakePoint(78.9530, 20.0610), 4326), 'a344554a704b2f6223c76398ae3901c11f342b194bf5204783e14cc5a120154e'),
    ('b5c6d7e8-f9a0-1b2c-3d4e-5f6a7b8c9d0e', 'Ghatanji Rural Hospital', 'Yavatmal', 'Main Road, Ghatanji, Yavatmal 445301', '+917230277300', 'ghatanji.rh@sanjeevanigrid.in', TRUE, ST_SetSRID(ST_MakePoint(78.3180, 20.1340), 4326), 'a344554a704b2f6223c76398ae3901c11f342b194bf5204783e14cc5a120154e'),
    ('b6c7d8e9-f0a1-2b3c-4d5e-6f7a8b9c0d1e', 'Digras Rural Hospital', 'Yavatmal', 'Old Cotton Market Road, Digras, Yavatmal 445203', '+917234222400', 'digras.rh@sanjeevanigrid.in', TRUE, ST_SetSRID(ST_MakePoint(77.7210, 20.1080), 4326), 'a344554a704b2f6223c76398ae3901c11f342b194bf5204783e14cc5a120154e'),
    ('b7c8d9e0-f1a2-3b4c-5d6e-7f8a9b0c1d2e', 'Umarkhed Rural Hospital', 'Yavatmal', 'Nanded Road, Umarkhed, Yavatmal 445206', '+917231237500', 'umarkhed.rh@sanjeevanigrid.in', TRUE, ST_SetSRID(ST_MakePoint(77.6980, 19.5980), 4326), 'a344554a704b2f6223c76398ae3901c11f342b194bf5204783e14cc5a120154e'),
    ('b8c9d0e1-f2a3-4b5c-6d7e-8f9a0b1c2d3e', 'Pandharkawada Rural Hospital (Kelapur)', 'Yavatmal', 'National Highway 7, Pandharkawada, Yavatmal 445302', '+917235252600', 'kelapur.rh@sanjeevanigrid.in', TRUE, ST_SetSRID(ST_MakePoint(78.5320, 20.0220), 4326), 'a344554a704b2f6223c76398ae3901c11f342b194bf5204783e14cc5a120154e'),
    ('b9c0d1e2-f3a4-5b6c-7d8e-9f0a1b2c3d4e', 'Ralegaon Rural Hospital', 'Yavatmal', 'Wadki Road, Ralegaon, Yavatmal 445402', '+917237288700', 'ralegaon.rh@sanjeevanigrid.in', TRUE, ST_SetSRID(ST_MakePoint(78.5080, 20.4180), 4326), 'a344554a704b2f6223c76398ae3901c11f342b194bf5204783e14cc5a120154e'),
    ('ba0b1c2d-3e4f-5a6b-7c8d-9e0f1a2b3c4d', 'Ner Sub-District Hospital', 'Yavatmal', 'Murtizapur Road, Ner, Yavatmal 445102', '+917236266800', 'ner.rh@sanjeevanigrid.in', TRUE, ST_SetSRID(ST_MakePoint(77.8610, 20.4890), 4326), 'a344554a704b2f6223c76398ae3901c11f342b194bf5204783e14cc5a120154e')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- SEED DATA: 30 Real Villages in Yavatmal District for Equity Map
-- ============================================================
INSERT INTO villages (id, name, district, taluka, population, latitude, longitude, location, nearest_hospital_name, distance_to_hospital_km, travel_time_minutes, doctor_availability_score, last_camp_date, days_since_last_camp, access_score, vulnerability_category)
VALUES
    -- GREEN: Good access (Access score > 70)
    ('11111111-0000-0000-0000-000000000001', 'Lohara', 'Yavatmal', 'Yavatmal', 5800, 20.4010, 78.1150, ST_SetSRID(ST_MakePoint(78.1150, 20.4010), 4326), 'Yavatmal District General Hospital', 4.2, 14, 0.90, CURRENT_DATE - INTERVAL '25 days', 25, 88.5, 'GREEN'),
    ('11111111-0000-0000-0000-000000000002', 'Wadgaon', 'Yavatmal', 'Yavatmal', 7200, 20.3790, 78.1020, ST_SetSRID(ST_MakePoint(78.1020, 20.3790), 4326), 'Yavatmal District General Hospital', 5.1, 16, 0.88, CURRENT_DATE - INTERVAL '40 days', 40, 85.0, 'GREEN'),
    ('11111111-0000-0000-0000-000000000003', 'Pimpalgaon', 'Yavatmal', 'Yavatmal', 4300, 20.3850, 78.1420, ST_SetSRID(ST_MakePoint(78.1420, 20.3850), 4326), 'Yavatmal District General Hospital', 5.8, 18, 0.85, CURRENT_DATE - INTERVAL '55 days', 55, 82.0, 'GREEN'),
    ('11111111-0000-0000-0000-000000000004', 'Bhosa', 'Yavatmal', 'Yavatmal', 3100, 20.4200, 78.1500, ST_SetSRID(ST_MakePoint(78.1500, 20.4200), 4326), 'Yavatmal District General Hospital', 7.5, 22, 0.80, CURRENT_DATE - INTERVAL '70 days', 70, 78.5, 'GREEN'),
    ('11111111-0000-0000-0000-000000000005', 'Kinhi', 'Yavatmal', 'Darwha', 3600, 20.3340, 77.7920, ST_SetSRID(ST_MakePoint(77.7920, 20.3340), 4326), 'Darwha Sub-District Hospital', 5.2, 18, 0.82, CURRENT_DATE - INTERVAL '60 days', 60, 79.0, 'GREEN'),
    ('11111111-0000-0000-0000-000000000006', 'Bori', 'Yavatmal', 'Pusad', 4800, 19.9250, 77.5920, ST_SetSRID(ST_MakePoint(77.5920, 19.9250), 4326), 'Pusad Sub-District Hospital', 6.0, 20, 0.80, CURRENT_DATE - INTERVAL '65 days', 65, 76.5, 'GREEN'),
    ('11111111-0000-0000-0000-000000000007', 'Shelu', 'Yavatmal', 'Wani', 5100, 20.0750, 78.9280, ST_SetSRID(ST_MakePoint(78.9280, 20.0750), 4326), 'Wani Sub-District Hospital', 5.5, 19, 0.78, CURRENT_DATE - INTERVAL '75 days', 75, 74.0, 'GREEN'),

    -- YELLOW: Moderate Access (Access score 40 - 70)
    ('11111111-0000-0000-0000-000000000008', 'Babhulgaon Rural', 'Yavatmal', 'Babhulgaon', 8400, 20.5200, 78.1200, ST_SetSRID(ST_MakePoint(78.1200, 20.5200), 4326), 'Yavatmal District General Hospital', 21.0, 42, 0.58, CURRENT_DATE - INTERVAL '120 days', 120, 62.0, 'YELLOW'),
    ('11111111-0000-0000-0000-000000000009', 'Kalamb Khurd', 'Yavatmal', 'Kalamb', 6200, 20.4780, 78.3370, ST_SetSRID(ST_MakePoint(78.3370, 20.4780), 4326), 'Yavatmal District General Hospital', 23.5, 48, 0.55, CURRENT_DATE - INTERVAL '110 days', 110, 59.5, 'YELLOW'),
    ('11111111-0000-0000-0000-000000000010', 'Arni Khurd', 'Yavatmal', 'Arni', 9100, 20.0880, 77.9350, ST_SetSRID(ST_MakePoint(77.9350, 20.0880), 4326), 'Darwha Sub-District Hospital', 31.0, 58, 0.52, CURRENT_DATE - INTERVAL '140 days', 140, 55.0, 'YELLOW'),
    ('11111111-0000-0000-0000-000000000011', 'Mahagaon Kasba', 'Yavatmal', 'Mahagaon', 7500, 19.8920, 77.8200, ST_SetSRID(ST_MakePoint(77.8200, 19.8920), 4326), 'Pusad Sub-District Hospital', 35.0, 62, 0.48, CURRENT_DATE - INTERVAL '130 days', 130, 52.5, 'YELLOW'),
    ('11111111-0000-0000-0000-000000000012', 'Kolura', 'Yavatmal', 'Ghatanji', 3200, 20.1480, 78.2950, ST_SetSRID(ST_MakePoint(78.2950, 20.1480), 4326), 'Ghatanji Rural Hospital', 13.5, 32, 0.60, CURRENT_DATE - INTERVAL '100 days', 100, 66.0, 'YELLOW'),
    ('11111111-0000-0000-0000-000000000013', 'Chikhali', 'Yavatmal', 'Digras', 2900, 20.1200, 77.6850, ST_SetSRID(ST_MakePoint(77.6850, 20.1200), 4326), 'Digras Rural Hospital', 11.8, 28, 0.62, CURRENT_DATE - INTERVAL '95 days', 95, 68.0, 'YELLOW'),
    ('11111111-0000-0000-0000-000000000014', 'Vasantnagar', 'Yavatmal', 'Umarkhed', 4400, 19.6100, 77.7200, ST_SetSRID(ST_MakePoint(77.7200, 19.6100), 4326), 'Umarkhed Rural Hospital', 9.5, 25, 0.64, CURRENT_DATE - INTERVAL '105 days', 105, 67.0, 'YELLOW'),
    ('11111111-0000-0000-0000-000000000015', 'Shirpur', 'Yavatmal', 'Kelapur', 3800, 20.0350, 78.4900, ST_SetSRID(ST_MakePoint(78.4900, 20.0350), 4326), 'Pandharkawada Rural Hospital', 10.5, 29, 0.59, CURRENT_DATE - INTERVAL '115 days', 115, 64.5, 'YELLOW'),
    ('11111111-0000-0000-0000-000000000016', 'Gujri', 'Yavatmal', 'Ralegaon', 3400, 20.4300, 78.4750, ST_SetSRID(ST_MakePoint(78.4750, 20.4300), 4326), 'Ralegaon Rural Hospital', 8.5, 24, 0.63, CURRENT_DATE - INTERVAL '120 days', 120, 65.5, 'YELLOW'),
    ('11111111-0000-0000-0000-000000000017', 'Dhanora', 'Yavatmal', 'Ner', 2800, 20.4700, 77.8800, ST_SetSRID(ST_MakePoint(77.8800, 20.4700), 4326), 'Ner Sub-District Hospital', 7.8, 22, 0.65, CURRENT_DATE - INTERVAL '110 days', 110, 67.5, 'YELLOW'),
    ('11111111-0000-0000-0000-000000000018', 'Maregaon Rural', 'Yavatmal', 'Maregaon', 6500, 20.0250, 78.7800, ST_SetSRID(ST_MakePoint(78.7800, 20.0250), 4326), 'Wani Sub-District Hospital', 26.0, 52, 0.50, CURRENT_DATE - INTERVAL '150 days', 150, 56.0, 'YELLOW'),
    ('11111111-0000-0000-0000-000000000019', 'Zari', 'Yavatmal', 'Zari-Jamani', 7100, 19.8500, 78.6800, ST_SetSRID(ST_MakePoint(78.6800, 19.8500), 4326), 'Pandharkawada Rural Hospital', 38.0, 72, 0.42, CURRENT_DATE - INTERVAL '170 days', 170, 48.0, 'YELLOW'),
    ('11111111-0000-0000-0000-000000000020', 'Runza', 'Yavatmal', 'Kelapur', 2700, 20.0500, 78.5800, ST_SetSRID(ST_MakePoint(78.5800, 20.0500), 4326), 'Pandharkawada Rural Hospital', 15.2, 36, 0.55, CURRENT_DATE - INTERVAL '135 days', 135, 60.5, 'YELLOW'),

    -- RED: Critical Need & Vulnerability (Access score < 40)
    ('11111111-0000-0000-0000-000000000021', 'Mukutban', 'Yavatmal', 'Zari-Jamani', 9500, 19.7800, 78.8500, ST_SetSRID(ST_MakePoint(78.8500, 19.7800), 4326), 'Wani Sub-District Hospital', 58.0, 115, 0.22, CURRENT_DATE - INTERVAL '300 days', 300, 22.0, 'RED'),
    ('11111111-0000-0000-0000-000000000022', 'Bittergaon', 'Yavatmal', 'Umarkhed', 8900, 19.5300, 77.5800, ST_SetSRID(ST_MakePoint(77.5800, 19.5300), 4326), 'Umarkhed Rural Hospital', 51.0, 98, 0.25, CURRENT_DATE - INTERVAL '260 days', 260, 26.5, 'RED'),
    ('11111111-0000-0000-0000-000000000023', 'Patan Bori', 'Yavatmal', 'Kelapur', 7800, 19.8900, 78.5600, ST_SetSRID(ST_MakePoint(78.5600, 19.8900), 4326), 'Pandharkawada Rural Hospital', 54.0, 105, 0.24, CURRENT_DATE - INTERVAL '280 days', 280, 24.0, 'RED'),
    ('11111111-0000-0000-0000-000000000024', 'Fulsawangi', 'Yavatmal', 'Mahagaon', 8200, 19.8200, 77.7100, ST_SetSRID(ST_MakePoint(77.7100, 19.8200), 4326), 'Pusad Sub-District Hospital', 46.0, 88, 0.28, CURRENT_DATE - INTERVAL '230 days', 230, 29.0, 'RED'),
    ('11111111-0000-0000-0000-000000000025', 'Parwa', 'Yavatmal', 'Ghatanji', 6100, 20.0100, 78.2100, ST_SetSRID(ST_MakePoint(78.2100, 20.0100), 4326), 'Ghatanji Rural Hospital', 53.0, 102, 0.26, CURRENT_DATE - INTERVAL '270 days', 270, 25.5, 'RED'),
    ('11111111-0000-0000-0000-000000000026', 'Titwi', 'Yavatmal', 'Ghatanji', 4200, 20.0800, 78.2500, ST_SetSRID(ST_MakePoint(78.2500, 20.0800), 4326), 'Ghatanji Rural Hospital', 44.0, 84, 0.30, CURRENT_DATE - INTERVAL '210 days', 210, 31.0, 'RED'),
    ('11111111-0000-0000-0000-000000000027', 'Vidul', 'Yavatmal', 'Umarkhed', 5900, 19.6400, 77.8100, ST_SetSRID(ST_MakePoint(77.8100, 19.6400), 4326), 'Umarkhed Rural Hospital', 42.0, 78, 0.32, CURRENT_DATE - INTERVAL '195 days', 195, 33.5, 'RED'),
    ('11111111-0000-0000-0000-000000000028', 'Jawala', 'Yavatmal', 'Arni', 5300, 20.1500, 77.8500, ST_SetSRID(ST_MakePoint(77.8500, 20.1500), 4326), 'Darwha Sub-District Hospital', 39.0, 74, 0.34, CURRENT_DATE - INTERVAL '215 days', 215, 34.0, 'RED'),
    ('11111111-0000-0000-0000-000000000029', 'Kumbhari', 'Yavatmal', 'Wani', 4100, 19.9800, 79.0500, ST_SetSRID(ST_MakePoint(79.0500, 19.9800), 4326), 'Wani Sub-District Hospital', 43.0, 82, 0.31, CURRENT_DATE - INTERVAL '245 days', 245, 30.5, 'RED'),
    ('11111111-0000-0000-0000-000000000030', 'Kalambani', 'Yavatmal', 'Ralegaon', 3500, 20.3500, 78.6100, ST_SetSRID(ST_MakePoint(78.6100, 20.3500), 4326), 'Ralegaon Rural Hospital', 37.0, 70, 0.35, CURRENT_DATE - INTERVAL '220 days', 220, 36.0, 'RED')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- SEED DATA: Demo Medical Camp at Lohara Village with Stock
-- ============================================================
INSERT INTO medical_camps (id, name, organizer_name, camp_type, venue, village_id, village_name, district, latitude, longitude, location, camp_date, start_time, end_time, doctor_in_charge, target_attendees, registered_count, attended_count, status, shareable_slug)
VALUES
    ('c2222222-2222-2222-2222-222222222201', 'Lohara Comprehensive Health & Immunization Camp', 'District Health Administration & SanjeevaniGrid', 'GENERAL_HEALTH', 'Zilla Parishad High School Ground', '11111111-0000-0000-0000-000000000001', 'Lohara', 'Yavatmal', 20.4010, 78.1150, ST_SetSRID(ST_MakePoint(78.1150, 20.4010), 4326), CURRENT_DATE, '09:00:00', '17:00:00', 'Dr. Nilesh Patil, MD (Medicine)', 120, 3, 3, 'IN_PROGRESS', 'lohara-health-camp-2026'),
    ('c2222222-2222-2222-2222-222222222202', 'Arni Rural Maternal & Child Care Camp', 'District Health Society & PHC Arni', 'MATERNAL_CHILD', 'Gram Panchayat Community Hall', '11111111-0000-0000-0000-000000000010', 'Arni Khurd', 'Yavatmal', 20.0880, 77.9350, ST_SetSRID(ST_MakePoint(77.9350, 20.0880), 4326), CURRENT_DATE + INTERVAL '4 days', '09:30:00', '16:30:00', 'Dr. Sunita Deshmukh, DGO', 150, 0, 0, 'SCHEDULED', 'arni-child-care-camp-2026')
ON CONFLICT (id) DO NOTHING;

-- Seed Medicine Stock for Camp 1
INSERT INTO camp_medicine_stock (id, camp_id, medicine_name, unit, initial_quantity, dispensed_quantity, remaining_quantity)
VALUES
    ('d2222222-0000-0000-0000-000000000001', 'c2222222-2222-2222-2222-222222222201', 'Paracetamol 500mg', 'tablets', 500, 45, 455),
    ('d2222222-0000-0000-0000-000000000002', 'c2222222-2222-2222-2222-222222222201', 'Amoxicillin 250mg', 'capsules', 300, 24, 276),
    ('d2222222-0000-0000-0000-000000000003', 'c2222222-2222-2222-2222-222222222201', 'ORS Sachets', 'sachets', 200, 30, 170),
    ('d2222222-0000-0000-0000-000000000004', 'c2222222-2222-2222-2222-222222222201', 'Iron & Folic Acid', 'tablets', 600, 60, 540),
    ('d2222222-0000-0000-0000-000000000005', 'c2222222-2222-2222-2222-222222222201', 'Cetirizine 10mg', 'tablets', 250, 15, 235),
    ('d2222222-0000-0000-0000-000000000006', 'c2222222-2222-2222-2222-222222222201', 'Metformin 500mg', 'tablets', 300, 20, 280),
    ('d2222222-0000-0000-0000-000000000007', 'c2222222-2222-2222-2222-222222222201', 'Amlodipine 5mg', 'tablets', 300, 25, 275),
    ('d2222222-0000-0000-0000-000000000008', 'c2222222-2222-2222-2222-222222222201', 'Antacid Gel 200ml', 'bottles', 80, 8, 72)
ON CONFLICT (camp_id, medicine_name) DO NOTHING;

-- Seed Initial Attendees for Demo Camp
INSERT INTO camp_attendees (id, camp_id, client_sync_id, full_name, phone, dob, age, gender, qr_code_id, village, vital_bp, vital_pulse, vital_temp, vital_spo2, triage_level, symptoms, notes, registered_offline, synced_at)
VALUES
    ('e2222222-1111-1111-1111-111111111101', 'c2222222-2222-2222-2222-222222222201', 'f2222222-1111-1111-1111-111111111101', 'Rameshwar Pawar', '+919822300001', '1982-04-15', 44, 'MALE', 'SG-CARD-YVT-0001', 'Lohara', '145/95', 82, 98.4, 98, 'URGENT', 'Persistent headache, dizziness', 'Elevated BP recorded. Prescribed Amlodipine 5mg.', FALSE, now() - INTERVAL '2 hours'),
    ('e2222222-1111-1111-1111-111111111102', 'c2222222-2222-2222-2222-222222222201', 'f2222222-1111-1111-1111-111111111102', 'Sunita Jadhav', '+919822300002', '1995-08-20', 31, 'FEMALE', 'SG-CARD-YVT-0002', 'Lohara', '118/76', 74, 98.6, 99, 'ROUTINE', 'Routine ANC checkup, fatigue', 'Hb 9.8 g/dL. Given Iron Folic Acid tablets.', FALSE, now() - INTERVAL '90 minutes'),
    ('e2222222-1111-1111-1111-111111111103', 'c2222222-2222-2222-2222-222222222201', 'f2222222-1111-1111-1111-111111111103', 'Aarav Rathod', '+919822300003', CURRENT_DATE - INTERVAL '80 days', 0, 'MALE', 'SG-CARD-YVT-0003', 'Lohara', '90/60', 115, 98.2, 99, 'ROUTINE', 'Infant 10-week immunization', 'Received Pentavalent-2 and OPV-2.', TRUE, now() - INTERVAL '30 minutes')
ON CONFLICT (client_sync_id) DO NOTHING;

-- Seed Initial Vaccinations
INSERT INTO camp_vaccinations (id, camp_id, attendee_id, client_sync_id, vaccine_name, dose_number, administered_date, next_due_date, batch_number, administered_by)
VALUES
    ('a2222222-3333-3333-3333-333333333301', 'c2222222-2222-2222-2222-222222222201', 'e2222222-1111-1111-1111-111111111103', 'f2222222-3333-3333-3333-333333333301', 'Pentavalent', 2, CURRENT_DATE, CURRENT_DATE + INTERVAL '28 days', 'PENTA-2026-B81', 'Sister Rekha (ANM)'),
    ('a2222222-3333-3333-3333-333333333302', 'c2222222-2222-2222-2222-222222222201', 'e2222222-1111-1111-1111-111111111103', 'f2222222-3333-3333-3333-333333333302', 'OPV', 2, CURRENT_DATE, CURRENT_DATE + INTERVAL '28 days', 'OPV-2026-X12', 'Sister Rekha (ANM)')
ON CONFLICT (client_sync_id) DO NOTHING;

