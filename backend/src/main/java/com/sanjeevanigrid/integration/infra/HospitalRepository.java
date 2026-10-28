package com.sanjeevanigrid.integration.infra;

import com.sanjeevanigrid.integration.domain.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, UUID> {

    List<Hospital> findByVerifiedTrue();

    List<Hospital> findByVerifiedFalse();

    Optional<Hospital> findByLicenceNumber(String licenceNumber);
}
