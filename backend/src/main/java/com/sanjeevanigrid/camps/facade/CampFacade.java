package com.sanjeevanigrid.camps.facade;

import com.sanjeevanigrid.camps.domain.*;
import com.sanjeevanigrid.camps.infra.CampAttendeeRepository;
import com.sanjeevanigrid.camps.infra.CampMedicineStockRepository;
import com.sanjeevanigrid.camps.infra.MedicalCampRepository;
import com.sanjeevanigrid.common.event.CampCompleted;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CampFacade {

    private final MedicalCampRepository campRepository;
    private final CampAttendeeRepository attendeeRepository;
    private final CampMedicineStockRepository stockRepository;
    private final CampSyncService syncService;
    private final CampReportGenerator reportGenerator;
    private final VaccineScheduleEngine vaccineScheduleEngine;
    private final ApplicationEventPublisher eventPublisher;

    public CampFacade(MedicalCampRepository campRepository,
                      CampAttendeeRepository attendeeRepository,
                      CampMedicineStockRepository stockRepository,
                      CampSyncService syncService,
                      CampReportGenerator reportGenerator,
                      VaccineScheduleEngine vaccineScheduleEngine,
                      ApplicationEventPublisher eventPublisher) {
        this.campRepository = campRepository;
        this.attendeeRepository = attendeeRepository;
        this.stockRepository = stockRepository;
        this.syncService = syncService;
        this.reportGenerator = reportGenerator;
        this.vaccineScheduleEngine = vaccineScheduleEngine;
        this.eventPublisher = eventPublisher;
    }

    public List<MedicalCamp> getAllCamps() {
        return campRepository.findAllByOrderByCampDateDesc();
    }

    public Optional<MedicalCamp> getCampById(UUID id) {
        return campRepository.findById(id);
    }

    @Transactional
    public MedicalCamp createCamp(MedicalCamp camp) {
        MedicalCamp saved = campRepository.save(camp);

        // Seed basic medicine stock for this camp
        List<CampMedicineStock> defaultStock = List.of(
                new CampMedicineStock(saved.getId(), "Paracetamol 500mg", "tablets", 300),
                new CampMedicineStock(saved.getId(), "Amoxicillin 250mg", "capsules", 200),
                new CampMedicineStock(saved.getId(), "ORS Sachets", "sachets", 150),
                new CampMedicineStock(saved.getId(), "Iron & Folic Acid", "tablets", 400),
                new CampMedicineStock(saved.getId(), "Cetirizine 10mg", "tablets", 200),
                new CampMedicineStock(saved.getId(), "Amlodipine 5mg", "tablets", 200),
                new CampMedicineStock(saved.getId(), "Antacid Gel 200ml", "bottles", 50)
        );
        stockRepository.saveAll(defaultStock);

        return saved;
    }

    @Transactional
    public MedicalCamp updateCampStatus(UUID id, String status) {
        MedicalCamp camp = campRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Camp not found: " + id));

        String oldStatus = camp.getStatus();
        camp.setStatus(status.toUpperCase());
        MedicalCamp updated = campRepository.save(camp);

        if ("COMPLETED".equalsIgnoreCase(status) && !"COMPLETED".equalsIgnoreCase(oldStatus)) {
            // Publish CampCompleted event
            eventPublisher.publishEvent(new CampCompleted(
                    camp.getId(),
                    camp.getVillageId(),
                    camp.getVillageName(),
                    camp.getAttendedCount()
            ));
        }

        return updated;
    }

    public CampSyncDto.SyncResult syncOfflineBatch(UUID campId, CampSyncDto.SyncBatch batch) {
        return syncService.processSyncBatch(campId, batch);
    }

    public List<CampAttendee> getCampAttendees(UUID campId) {
        return attendeeRepository.findByCampIdOrderByCreatedAtDesc(campId);
    }

    public List<CampMedicineStock> getCampMedicineStock(UUID campId) {
        return stockRepository.findByCampId(campId);
    }

    public boolean dispenseMedicine(UUID campId, String medicineName, int quantity) {
        return stockRepository.findByCampIdAndMedicineNameIgnoreCase(campId, medicineName)
                .map(stock -> {
                    boolean ok = stock.dispense(quantity);
                    if (ok) stockRepository.save(stock);
                    return ok;
                })
                .orElse(false);
    }

    public CampReportGenerator.CampReportDto getCampReport(UUID campId) {
        return reportGenerator.generateReport(campId);
    }

    public String getCampCsv(UUID campId) {
        return reportGenerator.generateCsv(campId);
    }

    public VaccineScheduleEngine.ScheduleResult evaluateVaccineSchedule(
            java.time.LocalDate dob,
            List<VaccineScheduleEngine.AdministeredDose> history
    ) {
        return vaccineScheduleEngine.evaluateSchedule(dob, history);
    }
}

