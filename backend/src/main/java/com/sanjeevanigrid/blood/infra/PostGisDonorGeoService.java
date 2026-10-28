package com.sanjeevanigrid.blood.infra;

import com.sanjeevanigrid.blood.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Geo service for searching and ranking nearby donors using PostGIS spatial functions
 * with Haversine great-circle fallback for unit tests and non-spatial database modes.
 */
@Service
public class PostGisDonorGeoService {

    private static final Logger log = LoggerFactory.getLogger(PostGisDonorGeoService.class);

    public record RankedDonor(
            BloodDonor donor,
            double distanceKm,
            double reliabilityScore,
            ReliabilityScorer.ReliabilityBreakdown reliabilityBreakdown,
            EligibilityCalculator.EligibilityResult eligibilityResult
    ) {}

    private final BloodDonorRepository donorRepo;
    private final ReliabilityScorer reliabilityScorer;
    private final EligibilityCalculator eligibilityCalculator;

    public PostGisDonorGeoService(BloodDonorRepository donorRepo,
                                  ReliabilityScorer reliabilityScorer,
                                  EligibilityCalculator eligibilityCalculator) {
        this.donorRepo = donorRepo;
        this.reliabilityScorer = reliabilityScorer;
        this.eligibilityCalculator = eligibilityCalculator;
    }

    /**
     * Finds and ranks nearby eligible donors compatible with the target blood group within radiusKm.
     * Orders first by reliability score descending, then by distance ascending.
     */
    public List<RankedDonor> searchAndRankDonors(double centerLat, double centerLon,
                                                 double radiusKm, List<BloodGroup> compatibleGroups) {
        List<BloodDonor> candidates;
        List<String> groupNames = compatibleGroups.stream().map(Enum::name).toList();

        try {
            // Attempt PostGIS native spatial query
            candidates = donorRepo.findNearbyDonorsPostgis(centerLat, centerLon, radiusKm * 1000.0, groupNames);
        } catch (Exception ex) {
            log.warn("PostGIS native spatial query not supported or failed (using in-memory Haversine): {}", ex.getMessage());
            candidates = donorRepo.findEligibleCompatibleDonors(compatibleGroups);
        }

        List<RankedDonor> ranked = new ArrayList<>();

        for (BloodDonor donor : candidates) {
            // 1. Mandatory Cool-down & Eligibility check
            EligibilityCalculator.EligibilityResult eligibility = eligibilityCalculator.checkEligibility(donor);
            if (!eligibility.isEligible()) {
                continue;
            }

            // 2. Distance calculation
            double distanceKm = calculateHaversineDistanceKm(centerLat, centerLon, donor.getLatitude(), donor.getLongitude());
            if (distanceKm > radiusKm) {
                continue;
            }

            // 3. Reliability scoring (0.5 response + 0.3 fulfilment + 0.2 recency)
            ReliabilityScorer.ReliabilityBreakdown breakdown = reliabilityScorer.evaluate(donor, java.time.Instant.now());

            ranked.add(new RankedDonor(donor, Math.round(distanceKm * 10.0) / 10.0, breakdown.overallScore(), breakdown, eligibility));
        }

        // Rank by Reliability Score DESC, then Distance ASC
        ranked.sort(Comparator
                .comparingDouble(RankedDonor::reliabilityScore).reversed()
                .thenComparingDouble(RankedDonor::distanceKm));

        return ranked;
    }

    /**
     * Haversine great-circle formula to calculate distance between two coordinates in kilometers.
     */
    public static double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
