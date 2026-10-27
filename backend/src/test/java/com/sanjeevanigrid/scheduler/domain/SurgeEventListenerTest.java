package com.sanjeevanigrid.scheduler.domain;

import com.sanjeevanigrid.common.event.SurgePredicted;
import com.sanjeevanigrid.notification.domain.NotificationGateway;
import com.sanjeevanigrid.scheduler.infra.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SurgeEventListenerTest {

    private SlotAllocationEngine slotAllocationEngine;
    private DoctorRepository doctorRepository;
    private NotificationGateway notificationGateway;
    private SurgeEventListener listener;

    private Doctor doctor1;
    private Doctor doctor2;

    @BeforeEach
    void setUp() {
        slotAllocationEngine = mock(SlotAllocationEngine.class);
        doctorRepository = mock(DoctorRepository.class);
        notificationGateway = mock(NotificationGateway.class);

        listener = new SurgeEventListener(slotAllocationEngine, doctorRepository, notificationGateway);

        doctor1 = new Doctor(
                UUID.randomUUID(), UUID.randomUUID(), "Dr. Priya Sharma",
                Specialty.GENERAL_MEDICINE, "MBBS, MD", 8,
                BigDecimal.ZERO, 15, 20
        );

        doctor2 = new Doctor(
                UUID.randomUUID(), UUID.randomUUID(), "Dr. Sunita Gaikwad",
                Specialty.EMERGENCY, "MBBS, MEM", 7,
                BigDecimal.ZERO, 15, 30
        );

        when(doctorRepository.findAllByActiveTrue()).thenReturn(List.of(doctor1, doctor2));
        when(slotAllocationEngine.reserveSurgeBuffer(any(), any(), anyDouble())).thenReturn(3);
    }

    @Test
    @DisplayName("Should activate surge reservation upon receiving SurgePredicted event with high peak occupancy")
    void testListensToSurgePredicted() {
        SurgePredicted event = new SurgePredicted(
                "ICU Ward", "Monsoon Flood", 92.5, 8, "beds", 15
        );

        listener.onSurgePredicted(event);

        assertThat(listener.isSurgeActive()).isTrue();
        assertThat(listener.getLastSurgeEvent()).isEqualTo(event);

        // Should call reserveSurgeBuffer for both doctors
        verify(slotAllocationEngine).reserveSurgeBuffer(eq(doctor1.getId()), any(LocalDate.class), eq(0.35));
        verify(slotAllocationEngine).reserveSurgeBuffer(eq(doctor2.getId()), any(LocalDate.class), eq(0.35));

        // Should notify admin
        verify(notificationGateway).sendSms(eq("ADMIN"), contains("Surge protocol activated"));
    }

    @Test
    @DisplayName("Should release surge buffer when deactivated")
    void testDeactivateSurgeProtocol() {
        when(slotAllocationEngine.releaseSurgeBuffer(any(), any())).thenReturn(3);

        int released = listener.deactivateSurgeProtocol();

        assertThat(listener.isSurgeActive()).isFalse();
        assertThat(released).isEqualTo(6); // 3 per doctor x 2
        verify(slotAllocationEngine).releaseSurgeBuffer(eq(doctor1.getId()), any(LocalDate.class));
        verify(slotAllocationEngine).releaseSurgeBuffer(eq(doctor2.getId()), any(LocalDate.class));
    }
}
