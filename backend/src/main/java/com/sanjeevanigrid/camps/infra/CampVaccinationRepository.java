package com.sanjeevanigrid.camps.infra;

import com.sanjeevanigrid.camps.domain.CampVaccination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampVaccinationRepository extends JpaRepository<CampVaccination, UUID> {
    List<CampVaccination> findByCampId(UUID campId);
    List<CampVaccination> findByAttendeeId(UUID attendeeId);
    Optional<CampVaccination> findByClientSyncId(UUID clientSyncId);
    boolean existsByClientSyncId(UUID clientSyncId);
}

