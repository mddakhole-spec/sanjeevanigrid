package com.sanjeevanigrid.equity.infra;

import com.sanjeevanigrid.equity.domain.Village;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VillageRepository extends JpaRepository<Village, UUID> {
    List<Village> findAllByOrderByAccessScoreAsc();
    List<Village> findByVulnerabilityCategory(String vulnerabilityCategory);
    List<Village> findByTalukaIgnoreCase(String taluka);
    Optional<Village> findByNameIgnoreCase(String name);
}

