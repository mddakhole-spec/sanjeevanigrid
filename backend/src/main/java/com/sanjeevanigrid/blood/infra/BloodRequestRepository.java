package com.sanjeevanigrid.blood.infra;

import com.sanjeevanigrid.blood.domain.BloodRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BloodRequestRepository extends JpaRepository<BloodRequest, UUID> {

    Optional<BloodRequest> findByRequestNumber(String requestNumber);

    List<BloodRequest> findByHospitalIdOrderByCreatedAtDesc(UUID hospitalId);

    List<BloodRequest> findAllByOrderByCreatedAtDesc();
}
