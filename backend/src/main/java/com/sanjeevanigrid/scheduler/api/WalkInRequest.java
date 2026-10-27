package com.sanjeevanigrid.scheduler.api;

import com.sanjeevanigrid.scheduler.domain.AppointmentPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record WalkInRequest(
        @NotNull(message = "doctorId is required")
        UUID doctorId,
        @NotBlank(message = "patientName is required")
        String patientName,
        @NotBlank(message = "patientPhone is required")
        String patientPhone,
        AppointmentPriority priority,
        String chiefComplaint
) {}
