package com.sanjeevanigrid.scheduler.domain;

import com.sanjeevanigrid.common.event.AppointmentShifted;
import com.sanjeevanigrid.notification.domain.NotificationGateway;
import com.sanjeevanigrid.scheduler.infra.AppointmentRepository;
import com.sanjeevanigrid.scheduler.infra.AppointmentWaitlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmergencyBumpServiceTest {

    private AppointmentRepository appointmentRepository;
    private AppointmentWaitlistRepository waitlistRepository;
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    private NotificationGateway notificationGateway;
    private EmergencyBumpService bumpService;

    private Doctor doctor;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        waitlistRepository = mock(AppointmentWaitlistRepository.class);
        eventPublisher = mock(org.springframework.context.ApplicationEventPublisher.class);
        notificationGateway = mock(NotificationGateway.class);

        bumpService = new EmergencyBumpService(
                appointmentRepository, waitlistRepository, eventPublisher, notificationGateway
        );

        doctor = new Doctor(
                UUID.randomUUID(), UUID.randomUUID(), "Dr. Priya Sharma",
                Specialty.GENERAL_MEDICINE, "MBBS, MD", 8,
                BigDecimal.ZERO, 15, 20
        );
        testDate = LocalDate.now();
    }

    @Test
    @DisplayName("Emergency appointment bumps the lowest-priority routine appointment and notifies via SMS")
    void testEmergencyBumpsRoutineAppointment() {
        UUID slotId = UUID.randomUUID();

        // Existing routine appointment
        Appointment routineAppt = new Appointment(
                slotId, doctor.getId(), doctor.getHospitalId(), null,
                "Ramesh Patil", "9876543210", 45, "M", "Routine checkup",
                AppointmentPriority.ROUTINE, "T-101", testDate,
                LocalTime.of(10, 0), LocalTime.of(10, 15), false, "IDEMP-1"
        );
        routineAppt.setQueuePosition(2);

        when(appointmentRepository.findAllByDoctorIdAndAppointmentDateAndStatusInOrderByStartTimeAsc(
                eq(doctor.getId()), eq(testDate), any()))
                .thenReturn(List.of(routineAppt));

        // Incoming emergency appointment
        Appointment emergencyAppt = new Appointment(
                null, doctor.getId(), doctor.getHospitalId(), null,
                "Sunil Shinde", "9988776655", 30, "M", "Acute chest trauma",
                AppointmentPriority.EMERGENCY, "E-001", testDate,
                null, null, false, "IDEMP-EMERGENCY"
        );

        Appointment bumped = bumpService.bumpForEmergency(doctor, testDate, emergencyAppt);

        // 1. Verify bumped appointment state
        assertThat(bumped.getStatus()).isEqualTo(AppointmentStatus.BUMPED);
        assertThat(bumped.getBumpedReason()).contains("E-001");
        verify(appointmentRepository).save(bumped);

        // 2. Verify emergency appointment receives the slot
        assertThat(emergencyAppt.getSlotId()).isEqualTo(slotId);
        assertThat(emergencyAppt.getStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(emergencyAppt.getEndTime()).isEqualTo(LocalTime.of(10, 15));

        // 3. Verify bumped patient added to priority waitlist
        ArgumentCaptor<AppointmentWaitlist> waitlistCaptor = ArgumentCaptor.forClass(AppointmentWaitlist.class);
        verify(waitlistRepository).save(waitlistCaptor.capture());
        AppointmentWaitlist waitlistEntry = waitlistCaptor.getValue();
        assertThat(waitlistEntry.getPatientName()).isEqualTo("Ramesh Patil");
        assertThat(waitlistEntry.getStatus()).isEqualTo("WAITING");

        // 4. Verify AppointmentShifted domain event published
        ArgumentCaptor<AppointmentShifted> eventCaptor = ArgumentCaptor.forClass(AppointmentShifted.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        AppointmentShifted event = eventCaptor.getValue();
        assertThat(event.doctorName()).isEqualTo("Dr. Priya Sharma");
        assertThat(event.patientName()).isEqualTo("Ramesh Patil");
        assertThat(event.newStatus()).isEqualTo("BUMPED");

        // 5. Verify SMS alert sent to bumped patient
        ArgumentCaptor<String> phoneCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationGateway).sendSms(phoneCaptor.capture(), msgCaptor.capture());
        assertThat(phoneCaptor.getValue()).isEqualTo("9876543210");
        assertThat(msgCaptor.getValue()).contains("rescheduled due to a life-threatening emergency trauma intake");
    }

    @Test
    @DisplayName("Should prioritize bumping routine over urgent appointment")
    void testPicksLowestPriorityForBump() {
        Appointment routine = new Appointment(
                UUID.randomUUID(), doctor.getId(), doctor.getHospitalId(), null,
                "Patient Routine", "1111111111", 40, "M", "Checkup",
                AppointmentPriority.ROUTINE, "T-101", testDate,
                LocalTime.of(10, 0), LocalTime.of(10, 15), false, "IDEMP-1"
        );

        Appointment urgent = new Appointment(
                UUID.randomUUID(), doctor.getId(), doctor.getHospitalId(), null,
                "Patient Urgent", "2222222222", 50, "F", "Severe fever",
                AppointmentPriority.URGENT, "U-102", testDate,
                LocalTime.of(10, 15), LocalTime.of(10, 30), false, "IDEMP-2"
        );

        when(appointmentRepository.findAllByDoctorIdAndAppointmentDateAndStatusInOrderByStartTimeAsc(
                eq(doctor.getId()), eq(testDate), any()))
                .thenReturn(List.of(urgent, routine));

        Appointment emergencyAppt = new Appointment(
                null, doctor.getId(), doctor.getHospitalId(), null,
                "Emergency Patient", "3333333333", 60, "M", "Cardiac arrest",
                AppointmentPriority.EMERGENCY, "E-001", testDate,
                null, null, false, "IDEMP-3"
        );

        Appointment bumped = bumpService.bumpForEmergency(doctor, testDate, emergencyAppt);

        // Must bump routine, not urgent
        assertThat(bumped.getPatientName()).isEqualTo("Patient Routine");
        assertThat(bumped.getPriority()).isEqualTo(AppointmentPriority.ROUTINE);
    }

    @Test
    @DisplayName("Should throw exception if no lower-priority appointment can be bumped")
    void testThrowsWhenAllAreEmergency() {
        Appointment existingEmergency = new Appointment(
                UUID.randomUUID(), doctor.getId(), doctor.getHospitalId(), null,
                "Patient E1", "1111111111", 40, "M", "Critical",
                AppointmentPriority.EMERGENCY, "E-101", testDate,
                LocalTime.of(10, 0), LocalTime.of(10, 15), false, "IDEMP-1"
        );

        when(appointmentRepository.findAllByDoctorIdAndAppointmentDateAndStatusInOrderByStartTimeAsc(
                eq(doctor.getId()), eq(testDate), any()))
                .thenReturn(List.of(existingEmergency));

        Appointment newEmergency = new Appointment(
                null, doctor.getId(), doctor.getHospitalId(), null,
                "Patient E2", "2222222222", 50, "F", "Critical",
                AppointmentPriority.EMERGENCY, "E-102", testDate,
                null, null, false, "IDEMP-2"
        );

        assertThatThrownBy(() -> bumpService.bumpForEmergency(doctor, testDate, newEmergency))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No lower-priority routine appointment found to bump");
    }
}
