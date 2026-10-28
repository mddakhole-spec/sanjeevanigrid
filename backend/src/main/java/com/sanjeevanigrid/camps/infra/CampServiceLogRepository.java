package com.sanjeevanigrid.camps.infra;

import com.sanjeevanigrid.camps.domain.CampServiceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampServiceLogRepository extends JpaRepository<CampServiceLog, UUID> {
    List<CampServiceLog> findByCampId(UUID campId);
    List<CampServiceLog> findByAttendeeId(UUID attendeeId);
    Optional<CampServiceLog> findByClientSyncId(UUID clientSyncId);
    boolean existsByClientSyncId(UUID clientSyncId);
}

