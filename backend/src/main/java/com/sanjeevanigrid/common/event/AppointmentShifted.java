package com.sanjeevanigrid.common.event;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Domain event emitted when an appointment is bumped, rescheduled, or shifted.
 * Published via Spring ApplicationEventPublisher.
 * Can later be swapped for Kafka.
 */
public record AppointmentShifted(
        UUID appointmentId,
        UUID doctorId,
        String doctorName,
        String patientName,
        String patientPhone,
        LocalDate appointmentDate,
        LocalTime originalStartTime,
        String reason,
        String newStatus,
        Instant timestamp
) {
    public AppointmentShifted(UUID appointmentId, UUID doctorId, String doctorName,
                              String patientName, String patientPhone,
                              LocalDate appointmentDate, LocalTime originalStartTime,
                              String reason, String newStatus) {
        this(appointmentId, doctorId, doctorName, patientName, patientPhone,
                appointmentDate, originalStartTime, reason, newStatus, Instant.now());
    }
}
