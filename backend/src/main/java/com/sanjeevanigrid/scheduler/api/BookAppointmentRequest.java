package com.sanjeevanigrid.scheduler.api;

import com.sanjeevanigrid.scheduler.domain.AppointmentPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record BookAppointmentRequest(
        @NotNull(message = "doctorId is required")
        UUID doctorId,
        UUID slotId,
        @NotBlank(message = "patientName is required")
        String patientName,
        @NotBlank(message = "patientPhone is required")
        String patientPhone,
        Integer patientAge,
        String patientGender,
        String chiefComplaint,
        AppointmentPriority priority,
        @NotNull(message = "appointmentDate is required")
        LocalDate appointmentDate,
        String idempotencyKey
) {}
