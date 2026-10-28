package com.sanjeevanigrid.camps.infra;

import com.sanjeevanigrid.camps.domain.MedicalCamp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalCampRepository extends JpaRepository<MedicalCamp, UUID> {
    List<MedicalCamp> findAllByOrderByCampDateDesc();
    List<MedicalCamp> findByStatus(String status);
    List<MedicalCamp> findByCampDate(LocalDate campDate);
    Optional<MedicalCamp> findByShareableSlug(String slug);
    List<MedicalCamp> findByVillageId(UUID villageId);
}

