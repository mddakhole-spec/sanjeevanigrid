package com.sanjeevanigrid.blood.facade;

import com.sanjeevanigrid.blood.domain.*;
import com.sanjeevanigrid.blood.infra.BloodDonorRepository;
import com.sanjeevanigrid.blood.infra.BloodStockRepository;
import com.sanjeevanigrid.blood.infra.PostGisDonorGeoService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Public facade for the Blood Locator module.
 * Other modules interact with the Blood module exclusively through this facade.
 */
@Component
public class BloodFacade {

    private final BloodAllocationService allocationService;
    private final BloodStockRepository stockRepo;
    private final BloodDonorRepository donorRepo;
    private final PostGisDonorGeoService geoService;
    private final EligibilityCalculator eligibilityCalculator;
    private final ReliabilityScorer reliabilityScorer;

    public BloodFacade(BloodAllocationService allocationService,
                       BloodStockRepository stockRepo,
                       BloodDonorRepository donorRepo,
                       PostGisDonorGeoService geoService,
                       EligibilityCalculator eligibilityCalculator,
                       ReliabilityScorer reliabilityScorer) {
        this.allocationService = allocationService;
        this.stockRepo = stockRepo;
        this.donorRepo = donorRepo;
        this.geoService = geoService;
        this.eligibilityCalculator = eligibilityCalculator;
        this.reliabilityScorer = reliabilityScorer;
    }

    public BloodAllocationService.AllocationResult requestBlood(UUID hospitalId, String patientName,
                                                               String patientPhone, BloodGroup bloodGroup,
                                                               int unitsNeeded, String urgency, Double customRadiusKm) {
        return allocationService.requestBlood(hospitalId, patientName, patientPhone, bloodGroup, unitsNeeded, urgency, customRadiusKm);
    }

    public List<BloodStock> getAvailableStock(UUID hospitalId, BloodGroup bloodGroup) {
        return stockRepo.findAvailableStockEarliestExpiry(hospitalId, bloodGroup, LocalDate.now());
    }

    public List<PostGisDonorGeoService.RankedDonor> findNearbyDonors(double lat, double lon, double radiusKm, BloodGroup bloodGroup) {
        return geoService.searchAndRankDonors(lat, lon, radiusKm, bloodGroup.getCompatibleDonors());
    }

    public EligibilityCalculator.EligibilityResult checkEligibility(BloodDonor donor) {
        return eligibilityCalculator.checkEligibility(donor);
    }

    public double calculateReliability(BloodDonor donor) {
        return reliabilityScorer.calculate(donor);
    }
}
