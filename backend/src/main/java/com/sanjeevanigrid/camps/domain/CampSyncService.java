package com.sanjeevanigrid.camps.domain;

import com.sanjeevanigrid.camps.infra.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class CampSyncService {

    private static final Logger log = LoggerFactory.getLogger(CampSyncService.class);

    private final MedicalCampRepository campRepository;
    private final CampAttendeeRepository attendeeRepository;
    private final CampServiceLogRepository serviceLogRepository;
    private final CampVaccinationRepository vaccinationRepository;
    private final CampMedicineStockRepository stockRepository;
    private final VaccineScheduleEngine vaccineScheduleEngine;

    public CampSyncService(MedicalCampRepository campRepository,
                           CampAttendeeRepository attendeeRepository,
                           CampServiceLogRepository serviceLogRepository,
                           CampVaccinationRepository vaccinationRepository,
                           CampMedicineStockRepository stockRepository,
                           VaccineScheduleEngine vaccineScheduleEngine) {
        this.campRepository = campRepository;
        this.attendeeRepository = attendeeRepository;
        this.serviceLogRepository = serviceLogRepository;
        this.vaccinationRepository = vaccinationRepository;
        this.stockRepository = stockRepository;
        this.vaccineScheduleEngine = vaccineScheduleEngine;
    }

    @Transactional
    public synchronized CampSyncDto.SyncResult processSyncBatch(UUID campId, CampSyncDto.SyncBatch batch) {
        MedicalCamp camp = campRepository.findById(campId)
                .orElseThrow(() -> new IllegalArgumentException("Medical camp not found: " + campId));

        int syncedRegs = 0;
        int dupRegs = 0;
        int syncedServices = 0;
        int syncedVaccs = 0;

        Map<UUID, UUID> clientSyncIdToAttendeeId = new HashMap<>();

        // 1. Process Registrations idempotently
        if (batch.registrations() != null) {
            for (CampSyncDto.SyncRegistration reg : batch.registrations()) {
                UUID syncId = (reg.clientSyncId() != null) ? reg.clientSyncId() : (reg.id() != null ? reg.id() : UUID.randomUUID());
                UUID attendeeId = (reg.id() != null) ? reg.id() : syncId;

                Optional<CampAttendee> existing = attendeeRepository.findByClientSyncId(syncId);
                if (existing.isEmpty() && reg.id() != null) {
                    existing = attendeeRepository.findById(reg.id());
                }

                if (existing.isPresent()) {
                    dupRegs++;
                    clientSyncIdToAttendeeId.put(syncId, existing.get().getId());
                    log.debug("Duplicate registration skipped for clientSyncId: {} / id: {}", syncId, attendeeId);
                } else {
                    CampAttendee attendee = new CampAttendee(
                            attendeeId,
                            campId,
                            syncId,
                            reg.fullName(),
                            reg.phone(),
                            reg.dob(),
                            reg.age(),
                            reg.gender(),
                            reg.qrCodeId(),
                            reg.village() != null ? reg.village() : camp.getVillageName(),
                            reg.vitalBp(),
                            reg.vitalPulse(),
                            reg.vitalTemp(),
                            reg.vitalSpo2(),
                            reg.triageLevel(),
                            reg.symptoms(),
                            reg.notes(),
                            reg.registeredOffline()
                    );
                    try {
                        attendee = attendeeRepository.save(attendee);
                        clientSyncIdToAttendeeId.put(syncId, attendee.getId());
                        camp.incrementRegistration();
                        syncedRegs++;
                    } catch (DataIntegrityViolationException ex) {
                        dupRegs++;
                        clientSyncIdToAttendeeId.put(syncId, attendeeId);
                        log.debug("Duplicate key caught on concurrent insert for id: {} / syncId: {}", attendeeId, syncId);
                    }
                }
            }
        }

        // 2. Process Services idempotently
        if (batch.services() != null) {
            for (CampSyncDto.SyncService s : batch.services()) {
                UUID syncId = (s.clientSyncId() != null) ? s.clientSyncId() : (s.id() != null ? s.id() : UUID.randomUUID());
                UUID serviceId = (s.id() != null) ? s.id() : syncId;
                if (!serviceLogRepository.existsByClientSyncId(syncId) && !serviceLogRepository.existsById(serviceId)) {
                    UUID attId = s.attendeeId();
                    if (attId == null && s.attendeeClientSyncId() != null) {
                        attId = clientSyncIdToAttendeeId.get(s.attendeeClientSyncId());
                    }
                    if (attId != null) {
                        CampServiceLog logEntity = new CampServiceLog(
                                serviceId,
                                campId,
                                attId,
                                syncId,
                                s.serviceType() != null ? s.serviceType() : "CONSULTATION",
                                s.details(),
                                s.doctorNotes(),
                                s.performedBy() != null ? s.performedBy() : camp.getDoctorInCharge()
                        );
                        serviceLogRepository.save(logEntity);
                        syncedServices++;
                    }
                }
            }
        }

        // 3. Process Vaccinations idempotently
        if (batch.vaccinations() != null) {
            for (CampSyncDto.SyncVaccination v : batch.vaccinations()) {
                UUID syncId = (v.clientSyncId() != null) ? v.clientSyncId() : (v.id() != null ? v.id() : UUID.randomUUID());
                UUID vaccId = (v.id() != null) ? v.id() : syncId;
                if (!vaccinationRepository.existsByClientSyncId(syncId) && !vaccinationRepository.existsById(vaccId)) {
                    UUID attId = v.attendeeId();
                    if (attId == null && v.attendeeClientSyncId() != null) {
                        attId = clientSyncIdToAttendeeId.get(v.attendeeClientSyncId());
                    }
                    if (attId != null) {
                        LocalDate nextDue = v.nextDueDate();
                        LocalDate adminDate = (v.administeredDate() != null) ? v.administeredDate() : LocalDate.now();
                        if (nextDue == null && vaccineScheduleEngine != null) {
                            VaccineScheduleEngine.ScheduleResult schedule = vaccineScheduleEngine.evaluateSchedule(
                                    adminDate,
                                    List.of(new VaccineScheduleEngine.AdministeredDose(v.vaccineName(), v.doseNumber(), adminDate)),
                                    adminDate
                            );
                            nextDue = schedule.schedule().stream()
                                    .filter(s -> !"COMPLETED".equals(s.status()) && s.recommendedDate() != null && s.recommendedDate().isAfter(adminDate))
                                    .map(VaccineScheduleEngine.VaccineStatus::recommendedDate)
                                    .findFirst()
                                    .orElse(adminDate.plusDays(28));
                        } else if (nextDue == null) {
                            nextDue = adminDate.plusDays(28);
                        }

                        CampVaccination vacc = new CampVaccination(
                                vaccId,
                                campId,
                                attId,
                                syncId,
                                v.vaccineName(),
                                v.doseNumber() > 0 ? v.doseNumber() : 1,
                                adminDate,
                                nextDue,
                                v.batchNumber() != null ? v.batchNumber() : "CAMP-BATCH-" + LocalDate.now().getYear(),
                                v.administeredBy() != null ? v.administeredBy() : "ANM Staff"
                        );
                        vaccinationRepository.save(vacc);
                        syncedVaccs++;
                    }
                }
            }
        }

        // 4. Process Medicine Dispenses
        if (batch.dispenses() != null) {
            for (CampSyncDto.SyncMedicineDispense d : batch.dispenses()) {
                if (d.medicineName() != null && d.quantity() > 0) {
                    stockRepository.findByCampIdAndMedicineNameIgnoreCase(campId, d.medicineName().trim())
                            .ifPresent(stock -> stock.dispense(d.quantity()));
                }
            }
        }

        campRepository.save(camp);

        long totalAttendees = attendeeRepository.countByCampId(campId);

        log.info("Camp sync completed for camp {}: {} new, {} dup, {} services, {} vaccines",
                campId, syncedRegs, dupRegs, syncedServices, syncedVaccs);

        return new CampSyncDto.SyncResult(
                "SUCCESS",
                campId,
                syncedRegs,
                dupRegs,
                syncedServices,
                syncedVaccs,
                (int) totalAttendees,
                Instant.now()
        );
    }
}

