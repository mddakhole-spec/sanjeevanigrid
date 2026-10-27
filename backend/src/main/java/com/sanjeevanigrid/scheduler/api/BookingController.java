package com.sanjeevanigrid.scheduler.api;

import com.sanjeevanigrid.scheduler.domain.Appointment;
import com.sanjeevanigrid.scheduler.facade.SchedulerFacade;
import com.sanjeevanigrid.scheduler.infra.AppointmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Appointment Booking & Management", description = "Endpoints for booking wizard, walk-in tokens, and status")
public class BookingController {

    private final SchedulerFacade schedulerFacade;
    private final AppointmentRepository appointmentRepository;

    public BookingController(SchedulerFacade schedulerFacade,
                             AppointmentRepository appointmentRepository) {
        this.schedulerFacade = schedulerFacade;
        this.appointmentRepository = appointmentRepository;
    }

    @PostMapping("/book")
    @Operation(summary = "Book appointment from wizard with idempotency, wait-time prediction, and emergency priority bump")
    public ResponseEntity<Appointment> bookAppointment(@Valid @RequestBody BookAppointmentRequest req) {
        Appointment appointment = schedulerFacade.bookAppointment(
                req.doctorId(),
                req.slotId(),
                req.patientName(),
                req.patientPhone(),
                req.patientAge(),
                req.patientGender(),
                req.chiefComplaint(),
                req.priority(),
                req.appointmentDate(),
                req.idempotencyKey()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(appointment);
    }

    @PostMapping("/walk-in")
    @Operation(summary = "Issue immediate walk-in token at clinic OPD desk")
    public ResponseEntity<Appointment> createWalkInToken(@Valid @RequestBody WalkInRequest req) {
        Appointment walkIn = schedulerFacade.createWalkInToken(
                req.doctorId(),
                req.patientName(),
                req.patientPhone(),
                req.priority(),
                req.chiefComplaint()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(walkIn);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment details by ID")
    public ResponseEntity<Appointment> getAppointment(@PathVariable UUID id) {
        return appointmentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-phone")
    @Operation(summary = "Get appointments for a citizen by phone number")
    public ResponseEntity<List<Appointment>> getAppointmentsByPhone(@RequestParam String phone) {
        return ResponseEntity.ok(appointmentRepository.findAllByPatientPhoneOrderByAppointmentDateDescStartTimeDesc(phone));
    }
}
