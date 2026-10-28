-- V3: Blood Locator & Integration Hub (Stocks, Donors, Requests, Alerts, Camps, Feed Logs, Webhooks)
-- SanjeevaniGrid Phase 4 Migration

-- 1. Blood Stocks Table
CREATE TABLE IF NOT EXISTS blood_stocks (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hospital_id             UUID NOT NULL REFERENCES hospitals(id) ON DELETE CASCADE,
    blood_group             VARCHAR(15) NOT NULL,
    units_available         INT NOT NULL DEFAULT 0,
    units_reserved          INT NOT NULL DEFAULT 0,
    expiry_date             DATE NOT NULL,
    low_stock_threshold     INT NOT NULL DEFAULT 5,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_hospital_blood_expiry UNIQUE (hospital_id, blood_group, expiry_date)
);

CREATE INDEX IF NOT EXISTS idx_blood_stocks_hospital ON blood_stocks(hospital_id);
CREATE INDEX IF NOT EXISTS idx_blood_stocks_group ON blood_stocks(blood_group);

-- 2. Blood Donors Table
CREATE TABLE IF NOT EXISTS blood_donors (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id                     UUID REFERENCES users(id) ON DELETE SET NULL,
    full_name                   VARCHAR(255) NOT NULL,
    phone                       VARCHAR(20) NOT NULL UNIQUE,
    email                       VARCHAR(255),
    blood_group                 VARCHAR(15) NOT NULL,
    location                    GEOMETRY(Point, 4326),
    latitude                    DOUBLE PRECISION NOT NULL,
    longitude                   DOUBLE PRECISION NOT NULL,
    address                     TEXT,
    district                    VARCHAR(100) NOT NULL DEFAULT 'Yavatmal',
    is_available                BOOLEAN NOT NULL DEFAULT TRUE,
    consent_given               BOOLEAN NOT NULL DEFAULT TRUE,
    last_donation_date          DATE,
    total_alerts_received       INT NOT NULL DEFAULT 0,
    total_alerts_responded      INT NOT NULL DEFAULT 0,
    total_donations_fulfilled   INT NOT NULL DEFAULT 0,
    last_response_at            TIMESTAMP WITH TIME ZONE,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_blood_donors_location ON blood_donors USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_blood_donors_group ON blood_donors(blood_group);
CREATE INDEX IF NOT EXISTS idx_blood_donors_available ON blood_donors(is_available);

-- 3. Blood Requests Table
CREATE TABLE IF NOT EXISTS blood_requests (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    request_number              VARCHAR(50) NOT NULL UNIQUE,
    hospital_id                 UUID NOT NULL REFERENCES hospitals(id) ON DELETE CASCADE,
    patient_name                VARCHAR(255) NOT NULL,
    patient_phone               VARCHAR(20),
    blood_group                 VARCHAR(15) NOT NULL,
    units_needed                INT NOT NULL DEFAULT 1,
    units_allocated_from_stock  INT NOT NULL DEFAULT 0,
    units_fulfilled_by_donors   INT NOT NULL DEFAULT 0,
    urgency                     VARCHAR(30) NOT NULL DEFAULT 'URGENT',
    status                      VARCHAR(30) NOT NULL DEFAULT 'SEARCHING_STOCK',
    fulfillment_source          VARCHAR(30),
    notes                       TEXT,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_blood_requests_hospital ON blood_requests(hospital_id);
CREATE INDEX IF NOT EXISTS idx_blood_requests_status ON blood_requests(status);

-- 4. Blood Donor Alerts Table
CREATE TABLE IF NOT EXISTS blood_donor_alerts (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    blood_request_id    UUID NOT NULL REFERENCES blood_requests(id) ON DELETE CASCADE,
    donor_id            UUID NOT NULL REFERENCES blood_donors(id) ON DELETE CASCADE,
    channel             VARCHAR(30) NOT NULL DEFAULT 'WHATSAPP',
    alert_order         INT NOT NULL,
    reliability_score   DOUBLE PRECISION NOT NULL,
    distance_km         DOUBLE PRECISION NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'SENT',
    whatsapp_link       TEXT,
    telegram_status     VARCHAR(30),
    sms_status          VARCHAR(30),
    sent_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    responded_at        TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_donor_alerts_request ON blood_donor_alerts(blood_request_id);
CREATE INDEX IF NOT EXISTS idx_donor_alerts_donor ON blood_donor_alerts(donor_id);

-- 5. Donation Camps Table
CREATE TABLE IF NOT EXISTS donation_camps (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hospital_id         UUID REFERENCES hospitals(id) ON DELETE SET NULL,
    name                VARCHAR(255) NOT NULL,
    organizer_name      VARCHAR(255) NOT NULL DEFAULT 'District Red Cross Society',
    venue               VARCHAR(255) NOT NULL,
    address             TEXT,
    district            VARCHAR(100) NOT NULL DEFAULT 'Yavatmal',
    location            GEOMETRY(Point, 4326),
    latitude            DOUBLE PRECISION,
    longitude           DOUBLE PRECISION,
    camp_date           DATE NOT NULL,
    start_time          TIME NOT NULL,
    end_time            TIME NOT NULL,
    target_units        INT NOT NULL DEFAULT 50,
    registered_count    INT NOT NULL DEFAULT 0,
    collected_units     INT NOT NULL DEFAULT 0,
    status              VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_donation_camps_date ON donation_camps(camp_date);

-- 6. Camp RSVPs Table
CREATE TABLE IF NOT EXISTS camp_rsvps (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    camp_id     UUID NOT NULL REFERENCES donation_camps(id) ON DELETE CASCADE,
    donor_name  VARCHAR(255) NOT NULL,
    donor_phone VARCHAR(20) NOT NULL,
    blood_group VARCHAR(15) NOT NULL,
    notes       TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_camp_rsvp UNIQUE (camp_id, donor_phone)
);

-- 7. Hospital Feed Logs Table
CREATE TABLE IF NOT EXISTS hospital_feed_logs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hospital_id     UUID NOT NULL REFERENCES hospitals(id) ON DELETE CASCADE,
    source_type     VARCHAR(30) NOT NULL,
    resource_type   VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    summary         TEXT,
    payload         TEXT,
    error_message   TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_feed_logs_hospital ON hospital_feed_logs(hospital_id);

-- 8. Webhook Outbox Table
CREATE TABLE IF NOT EXISTS webhook_outbox (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hospital_id     UUID REFERENCES hospitals(id) ON DELETE SET NULL,
    event_type      VARCHAR(50) NOT NULL,
    payload         TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    target_url      VARCHAR(500),
    attempts        INT NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    next_retry_at   TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_webhook_status ON webhook_outbox(status);

-- ============================================================
-- SEED DATA FOR YAVATMAL DISTRICT (Hospitals, Blood Stock, Donors, Camps)
-- ============================================================

-- Update Yavatmal District General Hospital location and API key
UPDATE hospitals
SET location = ST_SetSRID(ST_MakePoint(78.1204, 20.3888), 4326),
    api_key_hash = 'a344554a704b2f6223c76398ae3901c11f342b194bf5204783e14cc5a120154e' -- sha256 for sg_live_demo_yavatmal_2026
WHERE id = 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d';

-- Add a second hospital in Yavatmal: Pusad Sub-District Hospital
INSERT INTO hospitals (id, name, district, address, phone, email, verified, location, api_key_hash)
VALUES (
    'b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e',
    'Pusad Sub-District Hospital',
    'Yavatmal',
    'Washim Road, Pusad, Yavatmal, Maharashtra 445204',
    '+917233245678',
    'pusad.sdh@sanjeevanigrid.in',
    TRUE,
    ST_SetSRID(ST_MakePoint(77.5671, 19.9073), 4326),
    'a344554a704b2f6223c76398ae3901c11f342b194bf5204783e14cc5a120154e'
) ON CONFLICT (id) DO NOTHING;

-- Seed Initial Blood Stock for Yavatmal District General Hospital
-- Note: O_NEGATIVE and AB_NEGATIVE are below low-stock threshold (5) to demonstrate smart warnings!
INSERT INTO blood_stocks (id, hospital_id, blood_group, units_available, units_reserved, expiry_date, low_stock_threshold)
VALUES
    ('e1111111-0000-0000-0000-000000000001', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 'A_POSITIVE', 14, 2, CURRENT_DATE + INTERVAL '24 days', 5),
    ('e1111111-0000-0000-0000-000000000002', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 'A_NEGATIVE', 6, 0, CURRENT_DATE + INTERVAL '18 days', 5),
    ('e1111111-0000-0000-0000-000000000003', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 'B_POSITIVE', 22, 3, CURRENT_DATE + INTERVAL '30 days', 5),
    ('e1111111-0000-0000-0000-000000000004', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 'B_NEGATIVE', 7, 1, CURRENT_DATE + INTERVAL '14 days', 5),
    ('e1111111-0000-0000-0000-000000000005', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 'AB_POSITIVE', 9, 0, CURRENT_DATE + INTERVAL '28 days', 5),
    ('e1111111-0000-0000-0000-000000000006', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 'AB_NEGATIVE', 2, 0, CURRENT_DATE + INTERVAL '9 days', 5),  -- CRITICAL LOW STOCK
    ('e1111111-0000-0000-0000-000000000007', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 'O_POSITIVE', 26, 4, CURRENT_DATE + INTERVAL '32 days', 5),
    ('e1111111-0000-0000-0000-000000000008', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 'O_NEGATIVE', 1, 0, CURRENT_DATE + INTERVAL '7 days', 5)   -- CRITICAL LOW STOCK & EXPIRING
ON CONFLICT (hospital_id, blood_group, expiry_date) DO NOTHING;

-- Seed Donors across Yavatmal District with varying coordinates, availability, cool-down dates, and reliability stats
INSERT INTO blood_donors (id, full_name, phone, blood_group, latitude, longitude, location, address, district, is_available, consent_given, last_donation_date, total_alerts_received, total_alerts_responded, total_donations_fulfilled, last_response_at)
VALUES
    -- 1. High-reliability O-Negative donor in Yavatmal city (~2.1 km from GH)
    ('b1111111-1111-1111-1111-111111111101', 'Anita Deshmukh', '+919822100001', 'O_NEGATIVE', 20.3920, 78.1340, ST_SetSRID(ST_MakePoint(78.1340, 20.3920), 4326), 'Shivaji Nagar, Yavatmal', 'Yavatmal', TRUE, TRUE, CURRENT_DATE - INTERVAL '120 days', 10, 10, 8, now() - INTERVAL '2 days'),
    -- 2. Another O-Negative donor in Arni (~32 km from GH) - High reliability
    ('b1111111-1111-1111-1111-111111111102', 'Ganesh Rathod', '+919822100002', 'O_NEGATIVE', 20.0880, 77.9350, ST_SetSRID(ST_MakePoint(77.9350, 20.0880), 4326), 'Main Road, Arni, Yavatmal', 'Yavatmal', TRUE, TRUE, CURRENT_DATE - INTERVAL '105 days', 8, 7, 6, now() - INTERVAL '5 days'),
    -- 3. O-Negative donor currently IN COOL-DOWN (donated 25 days ago)
    ('b1111111-1111-1111-1111-111111111103', 'Sunil Chavan', '+919822100003', 'O_NEGATIVE', 20.4010, 78.1150, ST_SetSRID(ST_MakePoint(78.1150, 20.4010), 4326), 'Lohara, Yavatmal', 'Yavatmal', TRUE, TRUE, CURRENT_DATE - INTERVAL '25 days', 5, 5, 4, now() - INTERVAL '25 days'),
    -- 4. O-Negative donor WITH CONSENT=FALSE (testing exclusion)
    ('b1111111-1111-1111-1111-111111111104', 'Kavita Jadhav', '+919822100004', 'O_NEGATIVE', 20.3800, 78.1250, ST_SetSRID(ST_MakePoint(78.1250, 20.3800), 4326), 'Godhani Road, Yavatmal', 'Yavatmal', TRUE, FALSE, CURRENT_DATE - INTERVAL '140 days', 4, 1, 1, now() - INTERVAL '60 days'),
    -- 5. AB-Negative donor (critical group) in Yavatmal (~4.5 km) - Eligible & responsive
    ('b1111111-1111-1111-1111-111111111105', 'Manoj Gaikwad', '+919822100005', 'AB_NEGATIVE', 20.3750, 78.1550, ST_SetSRID(ST_MakePoint(78.1550, 20.3750), 4326), 'Darda Nagar, Yavatmal', 'Yavatmal', TRUE, TRUE, CURRENT_DATE - INTERVAL '95 days', 6, 5, 4, now() - INTERVAL '8 days'),
    -- 6. A-Positive donor in Kalamb (~22 km from GH)
    ('b1111111-1111-1111-1111-111111111106', 'Prakash Thakre', '+919822100006', 'A_POSITIVE', 20.4780, 78.3370, ST_SetSRID(ST_MakePoint(78.3370, 20.4780), 4326), 'Bazar Chowk, Kalamb, Yavatmal', 'Yavatmal', TRUE, TRUE, CURRENT_DATE - INTERVAL '150 days', 12, 11, 9, now() - INTERVAL '4 days'),
    -- 7. B-Positive donor in Yavatmal (~1.5 km)
    ('b1111111-1111-1111-1111-111111111107', 'Sachin Wankhede', '+919822100007', 'B_POSITIVE', 20.3950, 78.1280, ST_SetSRID(ST_MakePoint(78.1280, 20.3950), 4326), 'Tilak Chowk, Yavatmal', 'Yavatmal', TRUE, TRUE, CURRENT_DATE - INTERVAL '110 days', 15, 14, 12, now() - INTERVAL '1 day'),
    -- 8. O-Positive donor in Yavatmal (~3.0 km)
    ('b1111111-1111-1111-1111-111111111108', 'Rahul Shinde', '+919822100008', 'O_POSITIVE', 20.3820, 78.1020, ST_SetSRID(ST_MakePoint(78.1020, 20.3820), 4326), 'Wadgaon Road, Yavatmal', 'Yavatmal', TRUE, TRUE, CURRENT_DATE - INTERVAL '200 days', 9, 8, 7, now() - INTERVAL '10 days'),
    -- 9. A-Negative donor in Ner (~28 km)
    ('b1111111-1111-1111-1111-111111111109', 'Deepak More', '+919822100009', 'A_NEGATIVE', 20.4900, 77.8600, ST_SetSRID(ST_MakePoint(77.8600, 20.4900), 4326), 'Station Road, Ner, Yavatmal', 'Yavatmal', TRUE, TRUE, CURRENT_DATE - INTERVAL '100 days', 7, 6, 5, now() - INTERVAL '7 days'),
    -- 10. B-Negative donor in Babhulgaon (~21 km)
    ('b1111111-1111-1111-1111-111111111110', 'Nitin Meshram', '+919822100010', 'B_NEGATIVE', 20.5200, 78.1200, ST_SetSRID(ST_MakePoint(78.1200, 20.5200), 4326), 'Gram Panchayat Chowk, Babhulgaon', 'Yavatmal', TRUE, TRUE, CURRENT_DATE - INTERVAL '130 days', 8, 7, 5, now() - INTERVAL '12 days'),
    -- 11. AB-Positive donor in Yavatmal (~2.8 km)
    ('b1111111-1111-1111-1111-111111111111', 'Vishal Raut', '+919822100011', 'AB_POSITIVE', 20.3850, 78.1400, ST_SetSRID(ST_MakePoint(78.1400, 20.3850), 4326), 'Pimpalgaon Road, Yavatmal', 'Yavatmal', TRUE, TRUE, CURRENT_DATE - INTERVAL '180 days', 5, 4, 3, now() - INTERVAL '15 days'),
    -- 12. O-Negative donor UNAVAILABLE (testing toggle)
    ('b1111111-1111-1111-1111-111111111112', 'Pooja Tiwari', '+919822100012', 'O_NEGATIVE', 20.3900, 78.1180, ST_SetSRID(ST_MakePoint(78.1180, 20.3900), 4326), 'Civil Lines, Yavatmal', 'Yavatmal', FALSE, TRUE, CURRENT_DATE - INTERVAL '115 days', 6, 3, 2, now() - INTERVAL '40 days')
ON CONFLICT (phone) DO NOTHING;

-- Seed Donation Camps in Yavatmal District
INSERT INTO donation_camps (id, hospital_id, name, organizer_name, venue, address, district, location, latitude, longitude, camp_date, start_time, end_time, target_units, registered_count, collected_units, status)
VALUES
    ('c1111111-1111-1111-1111-111111111101', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 'Yavatmal District Blood Donation Mahotsav', 'Indian Red Cross Society & SanjeevaniGrid', 'Azad Maidan Community Hall', 'Civil Lines, Yavatmal, Maharashtra 445001', 'Yavatmal', ST_SetSRID(ST_MakePoint(78.1250, 20.3910), 4326), 20.3910, 78.1250, CURRENT_DATE + INTERVAL '5 days', '09:00:00', '17:00:00', 80, 18, 0, 'SCHEDULED'),
    ('c1111111-1111-1111-1111-111111111102', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 'Pusad Taluka Rural Blood Drive', 'Rotary Club of Pusad & Pusad SDH', 'Dr. Babasaheb Ambedkar Bhavan', 'Subhash Ward, Pusad, Yavatmal 445204', 'Yavatmal', ST_SetSRID(ST_MakePoint(77.5690, 19.9100), 4326), 19.9100, 77.5690, CURRENT_DATE + INTERVAL '12 days', '10:00:00', '16:00:00', 50, 12, 0, 'SCHEDULED')
ON CONFLICT (id) DO NOTHING;

-- Seed initial RSVPs for Camp 1
INSERT INTO camp_rsvps (id, camp_id, donor_name, donor_phone, blood_group, notes)
VALUES
    ('f1111111-1111-1111-1111-111111111101', 'c1111111-1111-1111-1111-111111111101', 'Sachin Wankhede', '+919822100007', 'B_POSITIVE', 'Will arrive at 10:30 AM'),
    ('f1111111-1111-1111-1111-111111111102', 'c1111111-1111-1111-1111-111111111101', 'Rahul Shinde', '+919822100008', 'O_POSITIVE', 'First-time camp donor')
ON CONFLICT (camp_id, donor_phone) DO NOTHING;
