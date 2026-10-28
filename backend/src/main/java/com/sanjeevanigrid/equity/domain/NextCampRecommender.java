package com.sanjeevanigrid.equity.domain;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Recommends where the next medical camp should be deployed based on
 * Population x Access Gap (with camp deprivation weighting).
 * Pre-fills camp creation parameters for fast one-click camp organization.
 */
@Component
public class NextCampRecommender {

    public List<CampRecommendationDto> recommendNextCamps(List<Village> villages, int limit) {
        if (villages == null || villages.isEmpty()) {
            return List.of();
        }

        record ScoredVillage(Village village, double needScore) {}

        List<ScoredVillage> scoredList = new ArrayList<>();
        for (Village v : villages) {
            double accessGap = Math.max(1.0, 100.0 - v.getAccessScore());
            // Time multiplier gives higher priority if no camp has happened for a long time
            double timeDeprivation = 1.0 + (v.getDaysSinceLastCamp() / 200.0);
            double needScore = (v.getPopulation() / 1000.0) * accessGap * timeDeprivation;
            scoredList.add(new ScoredVillage(v, Math.round(needScore * 10.0) / 10.0));
        }

        // Sort descending by need score
        scoredList.sort(Comparator.comparingDouble(ScoredVillage::needScore).reversed());

        List<CampRecommendationDto> recommendations = new ArrayList<>();
        int rank = 1;
        int max = Math.min(limit, scoredList.size());

        for (int i = 0; i < max; i++) {
            ScoredVillage sv = scoredList.get(i);
            Village v = sv.village();

            String venue = v.getName() + " Gram Panchayat Bhavan / Zilla Parishad School Ground";
            int targetAttendees = Math.min(300, Math.max(60, (int) (v.getPopulation() * 0.04)));
            LocalDate suggestedDate = LocalDate.now().plusDays(5 + (i * 3));

            String urgency = String.format(
                    "Critical Gap (Access: %.1f) - %s days since last camp for %s residents (%s km from %s)",
                    v.getAccessScore(),
                    v.getDaysSinceLastCamp(),
                    String.format("%,d", v.getPopulation()),
                    v.getDistanceToHospitalKm(),
                    v.getNearestHospitalName()
            );

            recommendations.add(new CampRecommendationDto(
                    rank++,
                    v.getId(),
                    v.getName(),
                    v.getTaluka(),
                    v.getDistrict(),
                    v.getPopulation(),
                    v.getAccessScore(),
                    v.getVulnerabilityCategory(),
                    v.getDaysSinceLastCamp(),
                    sv.needScore(),
                    venue,
                    targetAttendees,
                    suggestedDate,
                    urgency,
                    v.getLatitude(),
                    v.getLongitude()
            ));
        }

        return recommendations;
    }
}

