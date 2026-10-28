package com.sanjeevanigrid.camps.domain;

import com.sanjeevanigrid.camps.infra.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class CampSyncServiceTest {

    private CampSyncService syncService;
    private UUID campId;
    private MedicalCamp camp;
    private final Map<UUID, CampAttendee> attendeeDb = new HashMap<>();
    private final Map<String, CampMedicineStock> stockDb = new HashMap<>();

    @BeforeEach
    void setUp() {
        attendeeDb.clear();
        stockDb.clear();

        campId = UUID.randomUUID();
        camp = new MedicalCamp("Lohara Camp", "Org", "GENERAL_HEALTH", "Zilla Parishad School",
                UUID.randomUUID(), "Lohara", "Yavatmal", 20.4, 78.1,
                LocalDate.now(), null, null, "Dr. Patil", 100);
        camp.setId(campId);

        MedicalCampRepository campRepo = createCampRepoStub();
        CampAttendeeRepository attendeeRepo = createAttendeeRepoStub();
        CampServiceLogRepository serviceRepo = createServiceRepoStub();
        CampVaccinationRepository vaccRepo = createVaccRepoStub();
        CampMedicineStockRepository stockRepo = createStockRepoStub();
        VaccineScheduleEngine vaccineEngine = new VaccineScheduleEngine();

        syncService = new CampSyncService(
                campRepo,
                attendeeRepo,
                serviceRepo,
                vaccRepo,
                stockRepo,
                vaccineEngine
        );
    }

    @Test
    @DisplayName("Idempotent registration: duplicate clientSyncId is ignored and not re-inserted")
    void testIdempotentRegistrationSync() {
        UUID syncId1 = UUID.randomUUID();
        UUID syncId2 = UUID.randomUUID();

        // Pre-populate syncId2 in the database
        CampAttendee preExisting = new CampAttendee();
        preExisting.setId(syncId2);
        preExisting.setClientSyncId(syncId2);
        preExisting.setCampId(campId);
        preExisting.setFullName("Existing Patient");
        preExisting.setPhone("+919822000000");
        attendeeDb.put(syncId2, preExisting);

        CampSyncDto.SyncRegistration reg1 = new CampSyncDto.SyncRegistration(
                syncId1, syncId1, "Anil Deshmukh", "+919822112233",
                LocalDate.of(1990, 1, 1), 36, "MALE", "SG-CARD-1",
                "Lohara", "120/80", 72, 98.6, 99, "ROUTINE", "None", "None", true, Instant.now()
        );

        CampSyncDto.SyncRegistration reg2 = new CampSyncDto.SyncRegistration(
                syncId2, syncId2, "Existing Patient", "+919822000000",
                LocalDate.of(1995, 5, 5), 31, "FEMALE", "SG-CARD-2",
                "Lohara", "118/76", 70, 98.4, 99, "ROUTINE", "None", "None", true, Instant.now()
        );

        CampSyncDto.SyncBatch batch = new CampSyncDto.SyncBatch(
                campId,
                List.of(reg1, reg2),
                List.of(),
                List.of(),
                List.of()
        );

        CampSyncDto.SyncResult result = syncService.processSyncBatch(campId, batch);

        assertEquals("SUCCESS", result.status());
        assertEquals(1, result.syncedRegistrations(), "Only 1 new registration should be processed");
        assertEquals(1, result.duplicateRegistrations(), "Pre-existing registration should be deduplicated");
        assertEquals(2, attendeeDb.size(), "Database should have exactly 2 attendees total");
        assertTrue(attendeeDb.containsKey(syncId1));
    }

    @Test
    @DisplayName("Medicine dispense decrements stock quantity properly")
    void testMedicineStockDispenseDuringSync() {
        CampMedicineStock stock = new CampMedicineStock(campId, "Paracetamol 500mg", "tablets", 100);
        stockDb.put("paracetamol 500mg", stock);

        CampSyncDto.SyncMedicineDispense dispense = new CampSyncDto.SyncMedicineDispense("Paracetamol 500mg", 20);
        CampSyncDto.SyncBatch batch = new CampSyncDto.SyncBatch(
                campId, List.of(), List.of(), List.of(), List.of(dispense)
        );

        CampSyncDto.SyncResult result = syncService.processSyncBatch(campId, batch);

        assertEquals("SUCCESS", result.status());
        assertEquals(80, stock.getRemainingQuantity());
        assertEquals(20, stock.getDispensedQuantity());
    }

    // --- Lightweight in-memory stubs ---
    private MedicalCampRepository createCampRepoStub() {
        return new MedicalCampRepository() {
            public Optional<MedicalCamp> findById(UUID id) { return Optional.of(camp); }
            public MedicalCamp save(MedicalCamp entity) { return entity; }
            public List<MedicalCamp> findAllByOrderByCampDateDesc() { return List.of(camp); }
            public List<MedicalCamp> findByStatus(String status) { return List.of(); }
            public List<MedicalCamp> findByCampDate(LocalDate campDate) { return List.of(); }
            public Optional<MedicalCamp> findByShareableSlug(String slug) { return Optional.empty(); }
            public List<MedicalCamp> findByVillageId(UUID villageId) { return List.of(); }
            public void flush() {}
            public <S extends MedicalCamp> S saveAndFlush(S entity) { return entity; }
            public <S extends MedicalCamp> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
            public void deleteAllInBatch(Iterable<MedicalCamp> entities) {}
            public void deleteAllByIdInBatch(Iterable<UUID> uuids) {}
            public void deleteAllInBatch() {}
            public MedicalCamp getOne(UUID uuid) { return null; }
            public MedicalCamp getById(UUID uuid) { return null; }
            public MedicalCamp getReferenceById(UUID uuid) { return null; }
            public <S extends MedicalCamp> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
            public <S extends MedicalCamp> List<S> findAll(Example<S> example) { return List.of(); }
            public <S extends MedicalCamp> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
            public <S extends MedicalCamp> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
            public <S extends MedicalCamp> long count(Example<S> example) { return 0; }
            public <S extends MedicalCamp> boolean exists(Example<S> example) { return false; }
            public <S extends MedicalCamp, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
            public <S extends MedicalCamp> List<S> saveAll(Iterable<S> entities) { return List.of(); }
            public List<MedicalCamp> findAll() { return List.of(); }
            public List<MedicalCamp> findAllById(Iterable<UUID> uuids) { return List.of(); }
            public boolean existsById(UUID uuid) { return false; }
            public long count() { return 0; }
            public void deleteById(UUID uuid) {}
            public void delete(MedicalCamp entity) {}
            public void deleteAllById(Iterable<? extends UUID> uuids) {}
            public void deleteAll(Iterable<? extends MedicalCamp> entities) {}
            public void deleteAll() {}
            public List<MedicalCamp> findAll(Sort sort) { return List.of(); }
            public Page<MedicalCamp> findAll(Pageable pageable) { return null; }
        };
    }

    private CampAttendeeRepository createAttendeeRepoStub() {
        return new CampAttendeeRepository() {
            public Optional<CampAttendee> findByClientSyncId(UUID syncId) {
                return attendeeDb.values().stream().filter(a -> syncId.equals(a.getClientSyncId())).findFirst();
            }
            public boolean existsByClientSyncId(UUID syncId) {
                return attendeeDb.values().stream().anyMatch(a -> syncId.equals(a.getClientSyncId()));
            }
            public CampAttendee save(CampAttendee entity) {
                attendeeDb.put(entity.getClientSyncId(), entity);
                return entity;
            }
            public long countByCampId(UUID cid) { return attendeeDb.size(); }
            public List<CampAttendee> findByCampIdOrderByCreatedAtDesc(UUID cid) { return new ArrayList<>(attendeeDb.values()); }
            public List<CampAttendee> findByCampIdAndTriageLevel(UUID cid, String triage) { return List.of(); }
            public Optional<CampAttendee> findByPhone(String phone) { return Optional.empty(); }
            public void flush() {}
            public <S extends CampAttendee> S saveAndFlush(S entity) { return entity; }
            public <S extends CampAttendee> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
            public void deleteAllInBatch(Iterable<CampAttendee> entities) {}
            public void deleteAllByIdInBatch(Iterable<UUID> uuids) {}
            public void deleteAllInBatch() {}
            public CampAttendee getOne(UUID uuid) { return null; }
            public CampAttendee getById(UUID uuid) { return null; }
            public CampAttendee getReferenceById(UUID uuid) { return null; }
            public <S extends CampAttendee> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
            public <S extends CampAttendee> List<S> findAll(Example<S> example) { return List.of(); }
            public <S extends CampAttendee> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
            public <S extends CampAttendee> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
            public <S extends CampAttendee> long count(Example<S> example) { return 0; }
            public <S extends CampAttendee> boolean exists(Example<S> example) { return false; }
            public <S extends CampAttendee, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
            public <S extends CampAttendee> List<S> saveAll(Iterable<S> entities) { return List.of(); }
            public Optional<CampAttendee> findById(UUID uuid) { return Optional.ofNullable(attendeeDb.get(uuid)); }
            public boolean existsById(UUID uuid) { return attendeeDb.containsKey(uuid); }
            public List<CampAttendee> findAll() { return new ArrayList<>(attendeeDb.values()); }
            public List<CampAttendee> findAllById(Iterable<UUID> uuids) { return List.of(); }
            public long count() { return attendeeDb.size(); }
            public void deleteById(UUID uuid) {}
            public void delete(CampAttendee entity) {}
            public void deleteAllById(Iterable<? extends UUID> uuids) {}
            public void deleteAll(Iterable<? extends CampAttendee> entities) {}
            public void deleteAll() {}
            public List<CampAttendee> findAll(Sort sort) { return List.of(); }
            public Page<CampAttendee> findAll(Pageable pageable) { return null; }
        };
    }

    private CampServiceLogRepository createServiceRepoStub() {
        return new CampServiceLogRepository() {
            public boolean existsByClientSyncId(UUID syncId) { return false; }
            public CampServiceLog save(CampServiceLog s) { return s; }
            public List<CampServiceLog> findByCampId(UUID campId) { return List.of(); }
            public List<CampServiceLog> findByAttendeeId(UUID attendeeId) { return List.of(); }
            public Optional<CampServiceLog> findByClientSyncId(UUID clientSyncId) { return Optional.empty(); }
            public void flush() {}
            public <S extends CampServiceLog> S saveAndFlush(S entity) { return entity; }
            public <S extends CampServiceLog> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
            public void deleteAllInBatch(Iterable<CampServiceLog> entities) {}
            public void deleteAllByIdInBatch(Iterable<UUID> uuids) {}
            public void deleteAllInBatch() {}
            public CampServiceLog getOne(UUID uuid) { return null; }
            public CampServiceLog getById(UUID uuid) { return null; }
            public CampServiceLog getReferenceById(UUID uuid) { return null; }
            public <S extends CampServiceLog> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
            public <S extends CampServiceLog> List<S> findAll(Example<S> example) { return List.of(); }
            public <S extends CampServiceLog> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
            public <S extends CampServiceLog> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
            public <S extends CampServiceLog> long count(Example<S> example) { return 0; }
            public <S extends CampServiceLog> boolean exists(Example<S> example) { return false; }
            public <S extends CampServiceLog, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
            public <S extends CampServiceLog> List<S> saveAll(Iterable<S> entities) { return List.of(); }
            public Optional<CampServiceLog> findById(UUID uuid) { return Optional.empty(); }
            public boolean existsById(UUID uuid) { return false; }
            public List<CampServiceLog> findAll() { return List.of(); }
            public List<CampServiceLog> findAllById(Iterable<UUID> uuids) { return List.of(); }
            public long count() { return 0; }
            public void deleteById(UUID uuid) {}
            public void delete(CampServiceLog entity) {}
            public void deleteAllById(Iterable<? extends UUID> uuids) {}
            public void deleteAll(Iterable<? extends CampServiceLog> entities) {}
            public void deleteAll() {}
            public List<CampServiceLog> findAll(Sort sort) { return List.of(); }
            public Page<CampServiceLog> findAll(Pageable pageable) { return null; }
        };
    }

    private CampVaccinationRepository createVaccRepoStub() {
        return new CampVaccinationRepository() {
            public boolean existsByClientSyncId(UUID syncId) { return false; }
            public CampVaccination save(CampVaccination v) { return v; }
            public List<CampVaccination> findByCampId(UUID campId) { return List.of(); }
            public List<CampVaccination> findByAttendeeId(UUID attendeeId) { return List.of(); }
            public Optional<CampVaccination> findByClientSyncId(UUID clientSyncId) { return Optional.empty(); }
            public void flush() {}
            public <S extends CampVaccination> S saveAndFlush(S entity) { return entity; }
            public <S extends CampVaccination> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
            public void deleteAllInBatch(Iterable<CampVaccination> entities) {}
            public void deleteAllByIdInBatch(Iterable<UUID> uuids) {}
            public void deleteAllInBatch() {}
            public CampVaccination getOne(UUID uuid) { return null; }
            public CampVaccination getById(UUID uuid) { return null; }
            public CampVaccination getReferenceById(UUID uuid) { return null; }
            public <S extends CampVaccination> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
            public <S extends CampVaccination> List<S> findAll(Example<S> example) { return List.of(); }
            public <S extends CampVaccination> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
            public <S extends CampVaccination> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
            public <S extends CampVaccination> long count(Example<S> example) { return 0; }
            public <S extends CampVaccination> boolean exists(Example<S> example) { return false; }
            public <S extends CampVaccination, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
            public <S extends CampVaccination> List<S> saveAll(Iterable<S> entities) { return List.of(); }
            public Optional<CampVaccination> findById(UUID uuid) { return Optional.empty(); }
            public boolean existsById(UUID uuid) { return false; }
            public List<CampVaccination> findAll() { return List.of(); }
            public List<CampVaccination> findAllById(Iterable<UUID> uuids) { return List.of(); }
            public long count() { return 0; }
            public void deleteById(UUID uuid) {}
            public void delete(CampVaccination entity) {}
            public void deleteAllById(Iterable<? extends UUID> uuids) {}
            public void deleteAll(Iterable<? extends CampVaccination> entities) {}
            public void deleteAll() {}
            public List<CampVaccination> findAll(Sort sort) { return List.of(); }
            public Page<CampVaccination> findAll(Pageable pageable) { return null; }
        };
    }

    private CampMedicineStockRepository createStockRepoStub() {
        return new CampMedicineStockRepository() {
            public Optional<CampMedicineStock> findByCampIdAndMedicineNameIgnoreCase(UUID campId, String name) {
                return Optional.ofNullable(stockDb.get(name.toLowerCase()));
            }
            public CampMedicineStock save(CampMedicineStock s) {
                stockDb.put(s.getMedicineName().toLowerCase(), s);
                return s;
            }
            public List<CampMedicineStock> findByCampId(UUID campId) { return new ArrayList<>(stockDb.values()); }
            public void flush() {}
            public <S extends CampMedicineStock> S saveAndFlush(S entity) { return entity; }
            public <S extends CampMedicineStock> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
            public void deleteAllInBatch(Iterable<CampMedicineStock> entities) {}
            public void deleteAllByIdInBatch(Iterable<UUID> uuids) {}
            public void deleteAllInBatch() {}
            public CampMedicineStock getOne(UUID uuid) { return null; }
            public CampMedicineStock getById(UUID uuid) { return null; }
            public CampMedicineStock getReferenceById(UUID uuid) { return null; }
            public <S extends CampMedicineStock> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
            public <S extends CampMedicineStock> List<S> findAll(Example<S> example) { return List.of(); }
            public <S extends CampMedicineStock> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
            public <S extends CampMedicineStock> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
            public <S extends CampMedicineStock> long count(Example<S> example) { return 0; }
            public <S extends CampMedicineStock> boolean exists(Example<S> example) { return false; }
            public <S extends CampMedicineStock, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
            public <S extends CampMedicineStock> List<S> saveAll(Iterable<S> entities) { return List.of(); }
            public Optional<CampMedicineStock> findById(UUID uuid) { return Optional.empty(); }
            public boolean existsById(UUID uuid) { return false; }
            public List<CampMedicineStock> findAll() { return List.of(); }
            public List<CampMedicineStock> findAllById(Iterable<UUID> uuids) { return List.of(); }
            public long count() { return 0; }
            public void deleteById(UUID uuid) {}
            public void delete(CampMedicineStock entity) {}
            public void deleteAllById(Iterable<? extends UUID> uuids) {}
            public void deleteAll(Iterable<? extends CampMedicineStock> entities) {}
            public void deleteAll() {}
            public List<CampMedicineStock> findAll(Sort sort) { return List.of(); }
            public Page<CampMedicineStock> findAll(Pageable pageable) { return null; }
        };
    }
}
