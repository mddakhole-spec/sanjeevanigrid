package com.sanjeevanigrid.equity.infra;

import com.sanjeevanigrid.common.event.CampCompleted;
import com.sanjeevanigrid.equity.domain.EquityCalculator;
import com.sanjeevanigrid.equity.domain.Village;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Component
public class CampCompletedEventListener {

    private static final Logger log = LoggerFactory.getLogger(CampCompletedEventListener.class);

    private final VillageRepository villageRepository;
    private final EquityCalculator equityCalculator;

    public CampCompletedEventListener(VillageRepository villageRepository, EquityCalculator equityCalculator) {
        this.villageRepository = villageRepository;
        this.equityCalculator = equityCalculator;
    }

    @EventListener
    @Transactional
    public void onCampCompleted(CampCompleted event) {
        log.info("Received CampCompleted event for camp: {} in village: {}", event.campId(), event.villageName());

        Optional<Village> villageOpt = Optional.empty();
        if (event.villageId() != null) {
            villageOpt = villageRepository.findById(event.villageId());
        }
        if (villageOpt.isEmpty() && event.villageName() != null && !event.villageName().isBlank()) {
            villageOpt = villageRepository.findByNameIgnoreCase(event.villageName().trim());
        }

        villageOpt.ifPresent(village -> {
            log.info("Updating village {} equity profile after successful camp with {} attendees",
                    village.getName(), event.attendeesCount());
            village.updateCampDate(LocalDate.now());
            equityCalculator.recalculateVillageEquity(village);
            villageRepository.save(village);
            log.info("Village {} new access score: {}, category: {}",
                    village.getName(), village.getAccessScore(), village.getVulnerabilityCategory());
        });
    }
}

