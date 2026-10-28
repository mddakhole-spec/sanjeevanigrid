package com.sanjeevanigrid.blood.infra;

import com.sanjeevanigrid.blood.domain.CampRsvp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampRsvpRepository extends JpaRepository<CampRsvp, UUID> {

    List<CampRsvp> findByCampIdOrderByCreatedAtAsc(UUID campId);

    long countByCampId(UUID campId);

    Optional<CampRsvp> findByCampIdAndDonorPhone(UUID campId, String donorPhone);
}
