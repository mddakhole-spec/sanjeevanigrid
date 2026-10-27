package com.sanjeevanigrid.scheduler.infra;

import com.sanjeevanigrid.scheduler.domain.Appointment;
import com.sanjeevanigrid.scheduler.domain.AppointmentPriority;
import com.sanjeevanigrid.scheduler.domain.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    List<Appointment> findAllByDoctorIdAndAppointmentDateOrderByQueuePositionAscStartTimeAsc(UUID doctorId, LocalDate appointmentDate);
    List<Appointment> findAllByDoctorIdAndAppointmentDateAndStatusInOrderByStartTimeAsc(UUID doctorId, LocalDate appointmentDate, Collection<AppointmentStatus> statuses);
    long countByDoctorIdAndAppointmentDateAndStatusIn(UUID doctorId, LocalDate appointmentDate, Collection<AppointmentStatus> statuses);
    Optional<Appointment> findByIdempotencyKey(String idempotencyKey);
    List<Appointment> findAllByPatientPhoneOrderByAppointmentDateDescStartTimeDesc(String patientPhone);
    List<Appointment> findAllByStatusAndAppointmentDateAndReminderSentFalse(AppointmentStatus status, LocalDate appointmentDate);
    List<Appointment> findAllByStatusAndAppointmentDateAndStartTimeBefore(AppointmentStatus status, LocalDate appointmentDate, LocalTime time);
    List<Appointment> findAllByDoctorIdAndAppointmentDateAndPriorityAndStatus(UUID doctorId, LocalDate appointmentDate, AppointmentPriority priority, AppointmentStatus status);
}
