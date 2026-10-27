package com.sanjeevanigrid.scheduler.infra;

import com.sanjeevanigrid.scheduler.domain.AppointmentSlot;
import com.sanjeevanigrid.scheduler.domain.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, UUID> {
    List<AppointmentSlot> findAllByDoctorIdAndSlotDateOrderByStartTime(UUID doctorId, LocalDate slotDate);
    Optional<AppointmentSlot> findByDoctorIdAndSlotDateAndStartTime(UUID doctorId, LocalDate slotDate, LocalTime startTime);
    long countByDoctorIdAndSlotDate(UUID doctorId, LocalDate slotDate);
    List<AppointmentSlot> findByDoctorIdAndSlotDateAndStatusOrderByStartTime(UUID doctorId, LocalDate slotDate, SlotStatus status);
    List<AppointmentSlot> findAllBySlotDateAndDoctorIdIn(LocalDate slotDate, List<UUID> doctorIds);
    List<AppointmentSlot> findAllByDoctorIdAndSlotDateAndIsSurgeReservedTrue(UUID doctorId, LocalDate slotDate);
}
