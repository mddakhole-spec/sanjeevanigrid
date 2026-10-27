package com.sanjeevanigrid.scheduler.infra;

import com.sanjeevanigrid.scheduler.domain.Doctor;
import com.sanjeevanigrid.scheduler.domain.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
    List<Doctor> findAllByActiveTrue();
    List<Doctor> findAllBySpecialtyAndActiveTrue(Specialty specialty);
    List<Doctor> findAllByHospitalIdAndActiveTrue(UUID hospitalId);
}
