package com.sanjeevanigrid.integration.infra;

import com.sanjeevanigrid.integration.domain.WebhookOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookOutboxRepository extends JpaRepository<WebhookOutbox, UUID> {

    List<WebhookOutbox> findByStatusOrderByCreatedAtDesc(String status);

    List<WebhookOutbox> findAllByOrderByCreatedAtDesc();
}
