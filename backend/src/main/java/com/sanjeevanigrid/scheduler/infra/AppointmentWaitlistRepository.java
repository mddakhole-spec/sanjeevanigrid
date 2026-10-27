package com.sanjeevanigrid.scheduler.infra;

import com.sanjeevanigrid.scheduler.domain.AppointmentWaitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentWaitlistRepository extends JpaRepository<AppointmentWaitlist, UUID> {
    List<AppointmentWaitlist> findAllByDoctorIdAndAppointmentDateAndStatusOrderByCreatedAtAsc(UUID doctorId, LocalDate appointmentDate, String status);
}
