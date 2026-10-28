package com.sanjeevanigrid.blood.infra;

import com.sanjeevanigrid.blood.domain.BloodDonor;
import com.sanjeevanigrid.blood.domain.BloodGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BloodDonorRepository extends JpaRepository<BloodDonor, UUID> {

    Optional<BloodDonor> findByPhone(String phone);

    List<BloodDonor> findByAvailableTrueAndConsentGivenTrue();

    List<BloodDonor> findByBloodGroup(BloodGroup bloodGroup);

    List<BloodDonor> findByDistrict(String district);

    @Query("SELECT d FROM BloodDonor d WHERE d.available = true AND d.consentGiven = true AND d.bloodGroup IN :compatibleGroups")
    List<BloodDonor> findEligibleCompatibleDonors(@Param("compatibleGroups") List<BloodGroup> compatibleGroups);

    /**
     * Native PostGIS query searching donors within radius in meters using ST_DWithin on geography point.
     * Computes distance in kilometers via ST_Distance.
     */
    @Query(value = """
        SELECT d.*, 
               ST_Distance(d.location::geography, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography) / 1000.0 AS distance_km
        FROM blood_donors d
        WHERE d.is_available = true
          AND d.consent_given = true
          AND d.blood_group IN (:groupNames)
          AND ST_DWithin(d.location::geography, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :radiusMeters)
        ORDER BY distance_km ASC
        """, nativeQuery = true)
    List<BloodDonor> findNearbyDonorsPostgis(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusMeters") double radiusMeters,
            @Param("groupNames") List<String> groupNames
    );
}
