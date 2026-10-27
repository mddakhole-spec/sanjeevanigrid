package com.sanjeevanigrid.identity.infra;

import com.sanjeevanigrid.identity.domain.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {
    Optional<OtpToken> findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(String phone);
}
