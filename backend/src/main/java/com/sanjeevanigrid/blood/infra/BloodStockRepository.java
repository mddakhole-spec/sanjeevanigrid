package com.sanjeevanigrid.blood.infra;

import com.sanjeevanigrid.blood.domain.BloodGroup;
import com.sanjeevanigrid.blood.domain.BloodStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BloodStockRepository extends JpaRepository<BloodStock, UUID> {

    List<BloodStock> findByHospitalId(UUID hospitalId);

    List<BloodStock> findByHospitalIdAndBloodGroup(UUID hospitalId, BloodGroup bloodGroup);

    @Query("SELECT s FROM BloodStock s WHERE s.hospitalId = :hospitalId AND s.bloodGroup = :bloodGroup AND s.expiryDate >= :today AND (s.unitsAvailable - s.unitsReserved) > 0 ORDER BY s.expiryDate ASC")
    List<BloodStock> findAvailableStockEarliestExpiry(
            @Param("hospitalId") UUID hospitalId,
            @Param("bloodGroup") BloodGroup bloodGroup,
            @Param("today") LocalDate today
    );

    @Query("SELECT s FROM BloodStock s WHERE s.hospitalId = :hospitalId AND s.expiryDate <= :cutoffDate AND s.expiryDate >= :today")
    List<BloodStock> findExpiringSoon(
            @Param("hospitalId") UUID hospitalId,
            @Param("today") LocalDate today,
            @Param("cutoffDate") LocalDate cutoffDate
    );

    @Query("SELECT s FROM BloodStock s WHERE s.hospitalId = :hospitalId AND s.unitsAvailable <= s.lowStockThreshold")
    List<BloodStock> findLowStockForHospital(@Param("hospitalId") UUID hospitalId);

    @Query("SELECT s FROM BloodStock s WHERE s.unitsAvailable <= s.lowStockThreshold")
    List<BloodStock> findAllLowStock();

    Optional<BloodStock> findByHospitalIdAndBloodGroupAndExpiryDate(UUID hospitalId, BloodGroup bloodGroup, LocalDate expiryDate);
}
