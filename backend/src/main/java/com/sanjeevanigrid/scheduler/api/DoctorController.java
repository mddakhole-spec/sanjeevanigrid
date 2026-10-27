package com.sanjeevanigrid.scheduler.api;

import com.sanjeevanigrid.scheduler.domain.AppointmentSlot;
import com.sanjeevanigrid.scheduler.domain.Doctor;
import com.sanjeevanigrid.scheduler.domain.DoctorAvailability;
import com.sanjeevanigrid.scheduler.domain.Specialty;
import com.sanjeevanigrid.scheduler.facade.SchedulerFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctors")
@Tag(name = "Doctor & Availability Management", description = "Endpoints for doctor profiles, weekly tap-grid availability, and slot management")
public class DoctorController {

    private final SchedulerFacade schedulerFacade;

    public DoctorController(SchedulerFacade schedulerFacade) {
        this.schedulerFacade = schedulerFacade;
    }

    @GetMapping
    @Operation(summary = "List doctors, optionally filtered by medical specialty")
    public ResponseEntity<List<Doctor>> listDoctors(@RequestParam(required = false) Specialty specialty) {
        if (specialty != null) {
            return ResponseEntity.ok(schedulerFacade.getDoctorsBySpecialty(specialty));
        }
        return ResponseEntity.ok(schedulerFacade.getAllDoctors());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get doctor details by ID")
    public ResponseEntity<Doctor> getDoctor(@PathVariable UUID id) {
        return schedulerFacade.getDoctorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/availability")
    @Operation(summary = "Get doctor weekly availability schedule template (tap-grid)")
    public ResponseEntity<List<DoctorAvailability>> getAvailability(@PathVariable UUID id) {
        return ResponseEntity.ok(schedulerFacade.getDoctorAvailability(id));
    }

    @PutMapping("/{id}/availability")
    @Operation(summary = "Update doctor weekly availability schedule template (tap-grid)")
    public ResponseEntity<List<DoctorAvailability>> updateAvailability(
            @PathVariable UUID id,
            @RequestBody List<AvailabilityDto> dtos) {

        List<DoctorAvailability> availabilities = dtos.stream()
                .map(d -> {
                    DoctorAvailability a = new DoctorAvailability(
                            id,
                            d.dayOfWeek(),
                            d.startTime(),
                            d.endTime(),
                            d.breakStartTime(),
                            d.breakEndTime(),
                            d.slotDurationMins()
                    );
                    a.setActive(d.isActive());
                    return a;
                })
                .toList();

        return ResponseEntity.ok(schedulerFacade.updateDoctorAvailability(id, availabilities));
    }

    @GetMapping("/{id}/slots")
    @Operation(summary = "Get or generate available/booked slots for doctor on a specific date")
    public ResponseEntity<List<AppointmentSlot>> getSlots(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(schedulerFacade.getOrCreateSlots(id, queryDate));
    }
}
