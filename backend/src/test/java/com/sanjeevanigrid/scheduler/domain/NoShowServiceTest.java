package com.sanjeevanigrid.scheduler.domain;

import com.sanjeevanigrid.notification.domain.NotificationGateway;
import com.sanjeevanigrid.scheduler.infra.AppointmentRepository;
import com.sanjeevanigrid.scheduler.infra.AppointmentSlotRepository;
import com.sanjeevanigrid.scheduler.infra.AppointmentWaitlistRepository;
import com.sanjeevanigrid.scheduler.infra.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NoShowServiceTest {

    private AppointmentRepository appointmentRepository;
    private AppointmentSlotRepository slotRepository;
    private AppointmentWaitlistRepository waitlistRepository;
    private DoctorRepository doctorRepository;
    private NotificationGateway notificationGateway;
    private NoShowService noShowService;

    private Doctor doctor;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        slotRepository = mock(AppointmentSlotRepository.class);
        waitlistRepository = mock(AppointmentWaitlistRepository.class);
        doctorRepository = mock(DoctorRepository.class);
        notificationGateway = mock(NotificationGateway.class);

        noShowService = new NoShowService(
                appointmentRepository, slotRepository, waitlistRepository, doctorRepository, notificationGateway
        );

        doctor = new Doctor(
                UUID.randomUUID(), UUID.randomUUID(), "Dr. Priya Sharma",
                Specialty.GENERAL_MEDICINE, "MBBS, MD", 8,
                BigDecimal.ZERO, 15, 20
        );
        testDate = LocalDate.now();

        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
    }

    @Test
    @DisplayName("No-show marks appointment NO_SHOW and promotes top waitlisted patient with SMS")
    void testNoShowPromotesWaitlistPatient() {
        UUID slotId = UUID.randomUUID();
        UUID apptId = UUID.randomUUID();

        Appointment appt = new Appointment(
                slotId, doctor.getId(), doctor.getHospitalId(), null,
                "Absent Patient", "1234567890", 35, "M", "Cough",
                AppointmentPriority.ROUTINE, "T-101", testDate,
                LocalTime.of(10, 0), LocalTime.of(10, 15), false, "IDEMP-NOSHOW"
        );
        appt.setId(apptId);

        when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(appt));

        // Waiting patient on waitlist
        AppointmentWaitlist waitlistEntry = new AppointmentWaitlist(
                doctor.getId(), testDate, null, "Waitlist Patient", "9876500000", AppointmentPriority.ROUTINE
        );
        when(waitlistRepository.findAllByDoctorIdAndAppointmentDateAndStatusOrderByCreatedAtAsc(
                eq(doctor.getId()), eq(testDate), eq("WAITING")))
                .thenReturn(List.of(waitlistEntry));

        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<Appointment> promotedOpt = noShowService.processNoShow(apptId);

        // 1. Verify original appointment is NO_SHOW
        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);

        // 2. Verify waitlist entry is promoted
        assertThat(waitlistEntry.getStatus()).isEqualTo("PROMOTED");
        verify(waitlistRepository).save(waitlistEntry);

        // 3. Verify promoted appointment returned
        assertThat(promotedOpt).isPresent();
        Appointment promoted = promotedOpt.get();
        assertThat(promoted.getPatientName()).isEqualTo("Waitlist Patient");
        assertThat(promoted.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(promoted.getSlotId()).isEqualTo(slotId);

        // 4. Verify SMS dispatched to promoted patient
        verify(notificationGateway).sendSms(eq("9876500000"), contains("CONFIRMED with Token"));
    }

    @Test
    @DisplayName("No-show with empty waitlist releases slot back to AVAILABLE")
    void testNoShowReleasesSlotWhenWaitlistEmpty() {
        UUID slotId = UUID.randomUUID();
        UUID apptId = UUID.randomUUID();

        Appointment appt = new Appointment(
                slotId, doctor.getId(), doctor.getHospitalId(), null,
                "Absent Patient", "1234567890", 35, "M", "Cough",
                AppointmentPriority.ROUTINE, "T-101", testDate,
                LocalTime.of(10, 0), LocalTime.of(10, 15), false, "IDEMP-NOSHOW"
        );
        appt.setId(apptId);

        AppointmentSlot slot = new AppointmentSlot(
                doctor.getId(), doctor.getHospitalId(), testDate, LocalTime.of(10, 0), LocalTime.of(10, 15)
        );
        slot.book();
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.BOOKED);

        when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(appt));
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(waitlistRepository.findAllByDoctorIdAndAppointmentDateAndStatusOrderByCreatedAtAsc(
                any(), any(), any())).thenReturn(List.of());

        Optional<Appointment> result = noShowService.processNoShow(apptId);

        assertThat(result).isEmpty();
        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
        verify(slotRepository).save(slot);
    }
}
