package com.sanjeevanigrid.integration.infra;

import com.sanjeevanigrid.integration.domain.HospitalFeedLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HospitalFeedLogRepository extends JpaRepository<HospitalFeedLog, UUID> {

    List<HospitalFeedLog> findByHospitalIdOrderByCreatedAtDesc(UUID hospitalId);

    List<HospitalFeedLog> findAllByOrderByCreatedAtDesc();
}
