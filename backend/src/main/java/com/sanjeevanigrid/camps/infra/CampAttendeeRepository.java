package com.sanjeevanigrid.camps.infra;

import com.sanjeevanigrid.camps.domain.CampAttendee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampAttendeeRepository extends JpaRepository<CampAttendee, UUID> {
    List<CampAttendee> findByCampIdOrderByCreatedAtDesc(UUID campId);
    Optional<CampAttendee> findByClientSyncId(UUID clientSyncId);
    boolean existsByClientSyncId(UUID clientSyncId);
    List<CampAttendee> findByCampIdAndTriageLevel(UUID campId, String triageLevel);
    Optional<CampAttendee> findByPhone(String phone);
    long countByCampId(UUID campId);
}

