package com.sanjeevanigrid.equity.facade;

import com.sanjeevanigrid.equity.domain.CampRecommendationDto;
import com.sanjeevanigrid.equity.domain.NextCampRecommender;
import com.sanjeevanigrid.equity.domain.Village;
import com.sanjeevanigrid.equity.infra.VillageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EquityFacade {

    private final VillageRepository villageRepository;
    private final NextCampRecommender recommender;

    public EquityFacade(VillageRepository villageRepository, NextCampRecommender recommender) {
        this.villageRepository = villageRepository;
        this.recommender = recommender;
    }

    public List<Village> getAllVillages() {
        return villageRepository.findAllByOrderByAccessScoreAsc();
    }

    public Optional<Village> getVillageById(UUID id) {
        return villageRepository.findById(id);
    }

    public List<CampRecommendationDto> getRecommendations(int limit) {
        List<Village> villages = villageRepository.findAll();
        return recommender.recommendNextCamps(villages, limit);
    }
}

