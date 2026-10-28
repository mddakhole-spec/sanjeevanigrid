package com.sanjeevanigrid.blood.infra;

import com.sanjeevanigrid.blood.domain.DonationCamp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DonationCampRepository extends JpaRepository<DonationCamp, UUID> {

    List<DonationCamp> findByCampDateGreaterThanEqualOrderByCampDateAsc(LocalDate date);

    List<DonationCamp> findByDistrictOrderByCampDateAsc(String district);

    List<DonationCamp> findAllByOrderByCampDateAsc();
}
