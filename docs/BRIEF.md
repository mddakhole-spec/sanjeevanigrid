You are a senior full-stack engineer. We are building "SanjeevaniGrid", an integrated healthcare coordination and crisis resource platform, as a demo-ready, deployable product. Users are non-technical (rural citizens, doctors, camp workers), so usability matters as much as features.

FIRST: read this whole brief, then produce an implementation plan (task list + folder structure) and wait for my approval before writing code. Build in phases; after each phase, run the app and tests and summarize what works.

## Product
One platform, six modules sharing one database:
0. Platform Core: auth (phone+OTP for citizens with sandbox OTP shown on screen; email+password for staff), JWT, RBAC roles (CITIZEN, DOCTOR, HOSPITAL_ADMIN, DONOR, CAMP_WORKER, CAMP_ORGANIZER, PLATFORM_ADMIN), onboarding state machine (REGISTERED -> VERIFIED -> ACTIVE, plus REJECTED/SUSPENDED) with admin approval queue, digital health card with QR, notification service, audit log, i18n (English, Hindi, Marathi).
1. Crisis Resource Simulator (showpiece): synthetic data engine (90 days hourly admissions per ward, Poisson arrivals, weekday/hour patterns, 2-3 injected surge events; beds, ICU, ventilators, staff, oxygen), forecaster in plain Java (Holt-Winters style exponential smoothing with hour-of-week seasonality; 72h inflow forecast with confidence band; hour-of-depletion per resource), scenario engine POST /api/sim/run (surge multiplier, triage mode, extra beds, staff reassignment, duration; hour-by-hour simulation; KPIs: patients turned away, peak occupancy, hours above 90%), dashboard with ward x hour heatmap (CSS grid), baseline-vs-scenario line chart, sliders, smart alerts, rule-based recommendations, preset scenarios (Monsoon flood, Dengue outbreak, Highway pile-up), strategy compare view. Label as "statistical forecast, not clinical advice".
2. Service Coordination: doctors, weekly tap-grid availability, slots, booking wizard (one question per screen), constraint-based allocation (working hours, breaks, max/day, unique doctor+start constraint + optimistic locking), emergency priority that bumps the lowest-priority appointment and notifies the patient, wait-time prediction (queue position x rolling average consultation time), @Scheduled reminders, live queue/token screen, walk-in tokens, no-show auto-release with waitlist, surge-aware slot reservation driven by simulator events.
3. Blood Locator: hospital x blood group stock with expiry and low-stock thresholds, blood requests (stock first, donors second), donor registry with availability toggle and consent, eligibility cool-down check, reliability score (0.5 response rate + 0.3 fulfilment rate + 0.2 recency) deciding alert order, PostGIS nearby-donor search, alert channels via adapters (in-app, SMS simulator inbox, WhatsApp wa.me prefilled link, optional Telegram bot), donation camp calendar with RSVP, shortage analytics chart.
4. Medical Camp Records: camp creation on a map with shareable link and printable QR poster; offline-first PWA (service worker, Workbox, IndexedDB via idb) with idempotent sync using client UUIDs and timestamps; fast registration (scan QR or phone; 3 fields minimum; one big green "Registered" button); services log; vaccine schedule engine computing next due dates from DOB and history; camp medicine stock; one-click printable camp report + CSV.
5. Equity Map (GIS): Leaflet + OpenStreetMap, ~30 villages and ~10 hospitals with real coordinates for one Maharashtra district (e.g., Yavatmal), access score per village (travel time to nearest hospital with open bed/ICU, doctor availability, recent camp coverage), green/yellow/red villages, "Where should the next camp go?" ranking by population x access gap that pre-fills a camp creation form.
6. Integration Hub: hospital onboarding portal (register, upload licence, admin verifies, receives API key), POST /api/feed/{hospitalId} authenticated by hashed API key with FHIR-aligned payload names (Location, Appointment, Patient, Observation), CSV/Excel upload with column mapping and validation preview, simulated feed adapter pushing realistic updates, webhook outbox.

## Architecture rules
- Spring Boot 3 / Java 21 MODULAR MONOLITH, one deployable. Packages: common, identity, simulator, scheduler, blood, camps, equity, integration, notification. Each module has api / domain / infra layers and exposes only a facade to other modules.
- Modules communicate via domain events (Spring ApplicationEventPublisher): SurgePredicted, StockLow, AppointmentShifted, CampCompleted. Design so the bus can later be swapped for Kafka.
- ADAPTER PATTERN for every external dependency: NotificationGateway (InApp, SmsSandbox, WhatsAppLink, Telegram), HospitalFeedGateway (Simulated, Csv, FhirStub), GeoService (OSM+PostGIS). Config flag mode=sandbox|live; show a visible "Sandbox mode" badge in the UI.
- Realtime dashboard updates via Server-Sent Events.
- PostgreSQL 16 + PostGIS, Flyway migrations, UUID ids, GiST indexes on geometry columns, deterministic seed data (Datafaker) plus an admin "Reset demo data" button. All data is synthetic.
- Security: Spring Security + JWT (access+refresh), BCrypt/Argon2, rate limiting on OTP/login, strict CORS, input validation, global exception handler with translatable friendly messages, secrets only via environment variables, audit log for approvals and overrides, idempotency keys on booking and sync endpoints.
- Docs: springdoc-openapi Swagger UI.

## Tech stack
Backend: Java 21, Spring Boot 3, Spring Web/Data JPA/Security, Flyway, Apache Commons Math, Datafaker, Actuator.
Frontend: React 18 + Vite + TypeScript + Tailwind + shadcn/ui, lucide-react, Recharts, react-leaflet, react-i18next, Web Speech API (en-IN, hi-IN, mr-IN), Workbox + idb, qrcode.react, html5-qrcode, driver.js.
Infra: Docker + docker-compose (app + postgres/postgis) for local dev; GitHub Actions CI; deploy targets Render (backend) and Vercel (frontend) with Neon/Supabase Postgres. Everything must run on free tiers.
Testing: JUnit 5, Mockito, Testcontainers, Vitest, Playwright for the main demo flow. Unit tests are mandatory for the forecaster and the slot allocator.

## UX requirements (non-negotiable)
- First screen: three huge language buttons (English, Hindi, Marathi), remembered afterwards.
- Simple Mode (default): four giant tiles - Book Doctor, Find Blood, Emergency, My Health Card. No menus, no tables. Pro Mode for admins/hospital staff: tables, filters, simulator.
- Always-visible Emergency button on every page: nearest hospital with an open bed, one-tap call, share location.
- One question per screen with progress bar and back button; plain words, no jargon; icon + label always; 48px+ touch targets, 18px+ base font, high contrast, WCAG 2.1 AA basics; voice input on key fields; read-aloud for key confirmations.
- Forgiving forms with inline hints, autosaved drafts; friendly empty/error states ("No doctors free today. Try tomorrow?").
- Low-bandwidth friendly: lazy loading, skeletons, small bundles.
- 60-second first-login tour (driver.js) and a Help button.
- Assisted mode: a helper (ASHA worker/relative) can act for a patient with recorded consent.
- Consent screen in plain language, "delete my data" button, medical disclaimer ("coordination tool, not medical advice").

## Design
Calm teal/green palette; red used only for emergencies. Clean, modern, trustworthy. Mobile-first.

## Definition of done
`docker compose up` starts everything; seeded demo accounts for every role are listed in the README; the demo story works end to end: flood preset -> heatmap turns red -> "ICU depletes in 9h" -> triage + extra beds improves outcome -> emergency booking bumps a routine appointment and the SMS simulator shows it -> blood request alerts ranked donors with a WhatsApp link -> camp PWA registers a patient offline and syncs -> equity map recommends the next camp and creates it.

Start now with the implementation plan only.