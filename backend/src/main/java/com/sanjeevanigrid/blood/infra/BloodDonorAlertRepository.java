package com.sanjeevanigrid.blood.infra;

import com.sanjeevanigrid.blood.domain.BloodDonorAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BloodDonorAlertRepository extends JpaRepository<BloodDonorAlert, UUID> {

    List<BloodDonorAlert> findByBloodRequestIdOrderByAlertOrderAsc(UUID bloodRequestId);

    List<BloodDonorAlert> findByDonorIdOrderBySentAtDesc(UUID donorId);
}
