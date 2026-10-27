package com.sanjeevanigrid.scheduler.infra;

import com.sanjeevanigrid.scheduler.domain.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, UUID> {
    List<DoctorAvailability> findAllByDoctorIdAndIsActiveTrue(UUID doctorId);
    Optional<DoctorAvailability> findByDoctorIdAndDayOfWeek(UUID doctorId, int dayOfWeek);
    List<DoctorAvailability> findAllByDoctorId(UUID doctorId);
}
