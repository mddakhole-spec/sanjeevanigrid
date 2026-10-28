package com.sanjeevanigrid.camps.infra;

import com.sanjeevanigrid.camps.domain.CampMedicineStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampMedicineStockRepository extends JpaRepository<CampMedicineStock, UUID> {
    List<CampMedicineStock> findByCampId(UUID campId);
    Optional<CampMedicineStock> findByCampIdAndMedicineNameIgnoreCase(UUID campId, String medicineName);
}

