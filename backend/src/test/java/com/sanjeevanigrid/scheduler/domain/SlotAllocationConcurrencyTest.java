package com.sanjeevanigrid.scheduler.domain;

import com.sanjeevanigrid.notification.domain.NotificationGateway;
import com.sanjeevanigrid.scheduler.infra.AppointmentRepository;
import com.sanjeevanigrid.scheduler.infra.AppointmentSlotRepository;
import com.sanjeevanigrid.scheduler.infra.AppointmentWaitlistRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SlotAllocationConcurrencyTest {

    @Test
    @DisplayName("Concurrency: 20 simultaneous threads attempting to book the EXACT same slot -> exactly 1 succeeds")
    void testConcurrentBookingSameSlot() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AppointmentSlotRepository slotRepo = mock(AppointmentSlotRepository.class);
        SlotAllocationEngine engine = new SlotAllocationEngine(slotRepo);

        UUID slotId = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        // Realistic DB row state tracking version and status
        AtomicLong committedVersion = new AtomicLong(0);
        AtomicInteger successfulBookings = new AtomicInteger(0);
        AtomicInteger failedBookings = new AtomicInteger(0);
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

        // When each thread queries findById, return a separate entity instance simulating an ORM entity session
        when(slotRepo.findById(slotId)).thenAnswer(inv -> {
            AppointmentSlot instance = new AppointmentSlot(
                    UUID.randomUUID(), UUID.randomUUID(), today,
                    LocalTime.of(10, 0), LocalTime.of(10, 15)
            );
            instance.setId(slotId);
            long curVer = committedVersion.get();
            instance.setVersion(curVer);
            if (curVer > 0) {
                instance.setStatus(SlotStatus.BOOKED);
            } else {
                instance.setStatus(SlotStatus.AVAILABLE);
            }
            return Optional.of(instance);
        });

        when(slotRepo.save(any(AppointmentSlot.class))).thenAnswer(inv -> {
            AppointmentSlot toSave = inv.getArgument(0);
            // Simulate JPA optimistic lock check: UPDATE appointment_slots SET ... WHERE id = ? AND version = ?
            if (!committedVersion.compareAndSet(toSave.getVersion(), toSave.getVersion() + 1)) {
                throw new OptimisticLockingFailureException(
                        "Optimistic lock conflict: row was updated by another concurrent transaction"
                );
            }
            toSave.setVersion(committedVersion.get());
            return toSave;
        });

        // Launch 20 concurrent threads
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize all threads to fire simultaneously
                    engine.reserveSlot(slotId, AppointmentPriority.ROUTINE);
                    successfulBookings.incrementAndGet();
                } catch (Throwable t) {
                    failedBookings.incrementAndGet();
                    exceptions.add(t);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Trigger simultaneous execution
        startLatch.countDown();
        boolean completed = finishLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        // Exactly ONE thread must succeed!
        assertThat(successfulBookings.get()).isEqualTo(1);
        // All other 19 threads must fail with conflict/exception (OptimisticLockingFailureException or IllegalStateException)!
        assertThat(failedBookings.get()).isEqualTo(threadCount - 1);
        assertThat(committedVersion.get()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Concurrency: Simultaneous emergency bump operations maintain state integrity and waitlist order")
    void testConcurrentEmergencyBump() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AppointmentRepository apptRepo = mock(AppointmentRepository.class);
        AppointmentWaitlistRepository waitlistRepo = mock(AppointmentWaitlistRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        NotificationGateway notificationGateway = mock(NotificationGateway.class);

        EmergencyBumpService bumpService = new EmergencyBumpService(
                apptRepo, waitlistRepo, eventPublisher, notificationGateway
        );

        Doctor doctor = new Doctor(
                UUID.randomUUID(), UUID.randomUUID(), "Dr. Priya Sharma",
                Specialty.GENERAL_MEDICINE, "MBBS, MD", 8,
                BigDecimal.ZERO, 15, 20
        );
        LocalDate testDate = LocalDate.now();

        // Prepare 5 routine appointments that can be bumped
        List<Appointment> routineAppointments = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < 5; i++) {
            LocalTime start = LocalTime.of(10, 0).plusMinutes(i * 15L);
            LocalTime end = start.plusMinutes(15L);
            Appointment a = new Appointment(
                    UUID.randomUUID(), doctor.getId(), doctor.getHospitalId(), null,
                    "Routine Patient " + (i + 1), "900000000" + i, 30 + i, "M", "Routine checkup",
                    AppointmentPriority.ROUTINE, "T-10" + i, testDate,
                    start, end, false, "IDEMP-" + i
            );
            a.setId(UUID.randomUUID());
            routineAppointments.add(a);
        }

        List<AppointmentWaitlist> waitlist = Collections.synchronizedList(new ArrayList<>());

        when(apptRepo.findAllByDoctorIdAndAppointmentDateAndStatusInOrderByStartTimeAsc(eq(doctor.getId()), eq(testDate), any()))
                .thenAnswer(inv -> {
                    synchronized (routineAppointments) {
                        return routineAppointments.stream()
                                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                                .toList();
                    }
                });

        when(apptRepo.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(waitlistRepo.save(any(AppointmentWaitlist.class))).thenAnswer(inv -> {
            AppointmentWaitlist w = inv.getArgument(0);
            waitlist.add(w);
            return w;
        });

        AtomicInteger successfulBumps = new AtomicInteger(0);
        AtomicInteger rejectedBumps = new AtomicInteger(0);

        // 10 concurrent emergency bump requests
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Appointment emergencyAppt = new Appointment(
                            null, doctor.getId(), doctor.getHospitalId(), null,
                            "Emergency Trauma " + idx, "999999999" + idx, 25, "F", "Acute emergency",
                            AppointmentPriority.EMERGENCY, "E-" + idx, testDate,
                            null, null, false, "EMERGENCY-" + idx
                    );
                    bumpService.bumpForEmergency(doctor, testDate, emergencyAppt);
                    successfulBumps.incrementAndGet();
                } catch (IllegalStateException e) {
                    // When all routine slots have been bumped, subsequent bumps are safely rejected
                    rejectedBumps.incrementAndGet();
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        // Since there were only 5 routine appointments, exactly 5 could be bumped!
        assertThat(successfulBumps.get()).isEqualTo(5);
        // The other 5 requests were rejected safely with no lower-priority appointments left!
        assertThat(rejectedBumps.get()).isEqualTo(5);
        // Waitlist has exactly 5 bumped patients!
        assertThat(waitlist).hasSize(5);

        // Verify all 5 routine appointments are now BUMPED
        for (Appointment a : routineAppointments) {
            assertThat(a.getStatus()).isEqualTo(AppointmentStatus.BUMPED);
            assertThat(a.getBumpedReason()).contains("emergency");
        }
    }
}
