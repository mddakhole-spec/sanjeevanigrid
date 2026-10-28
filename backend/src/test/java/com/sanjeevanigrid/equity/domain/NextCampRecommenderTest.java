package com.sanjeevanigrid.equity.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NextCampRecommenderTest {

    private NextCampRecommender recommender;

    @BeforeEach
    void setUp() {
        recommender = new NextCampRecommender();
    }

    @Test
    @DisplayName("Recommender prioritizes remote high-population village with low access score")
    void testNextCampRecommendationRanking() {
        Village goodAccess = new Village("Lohara", "Yavatmal", "Yavatmal", 5000, 20.4, 78.1, "DH", 4.0, 15, 0.9, null);
        goodAccess.setId(UUID.randomUUID());
        goodAccess.setAccessScore(88.0);
        goodAccess.setDaysSinceLastCamp(30);

        Village remoteHighPop = new Village("Mukutban", "Yavatmal", "Zari", 9500, 19.8, 78.8, "Wani SDH", 58.0, 115, 0.22, null);
        remoteHighPop.setId(UUID.randomUUID());
        remoteHighPop.setAccessScore(22.0);
        remoteHighPop.setDaysSinceLastCamp(300);

        Village remoteLowPop = new Village("SmallVillage", "Yavatmal", "Ner", 1200, 20.4, 77.8, "Ner RH", 40.0, 80, 0.3, null);
        remoteLowPop.setId(UUID.randomUUID());
        remoteLowPop.setAccessScore(28.0);
        remoteLowPop.setDaysSinceLastCamp(200);

        List<CampRecommendationDto> recs = recommender.recommendNextCamps(List.of(goodAccess, remoteHighPop, remoteLowPop), 3);

        assertFalse(recs.isEmpty());
        CampRecommendationDto topRank = recs.get(0);
        assertEquals("Mukutban", topRank.villageName(), "Highest population x access gap village should be rank 1");
        assertEquals(1, topRank.rank());
        assertNotNull(topRank.suggestedVenue());
        assertTrue(topRank.suggestedTargetAttendees() >= 100);
        assertNotNull(topRank.suggestedCampDate());
        assertNotNull(topRank.urgencyReason());
    }
}

