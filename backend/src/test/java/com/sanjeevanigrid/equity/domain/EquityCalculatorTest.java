package com.sanjeevanigrid.equity.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EquityCalculatorTest {

    private EquityCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new EquityCalculator();
    }

    @Test
    @DisplayName("Village with short travel time, high doctor availability, and recent camp gets GREEN category")
    void testGoodAccessVillage() {
        // 15 min travel time, 0.90 doctor score, camp 25 days ago
        double score = calculator.calculateAccessScore(15, 0.90, 25);
        String category = calculator.determineVulnerabilityCategory(score);

        assertTrue(score >= 70.0, "Score should be >= 70.0 for village near hospital with recent camp, got: " + score);
        assertEquals("GREEN", category);
    }

    @Test
    @DisplayName("Remote village with 110 min travel time, low doctor availability, and no camp in 300 days gets RED category")
    void testCriticalAccessGapVillage() {
        // 110 min travel time, 0.20 doctor score, no camp in 300 days
        double score = calculator.calculateAccessScore(110, 0.20, 300);
        String category = calculator.determineVulnerabilityCategory(score);

        assertTrue(score < 40.0, "Score should be < 40.0 for critical gap village, got: " + score);
        assertEquals("RED", category);
    }

    @Test
    @DisplayName("Recalculate updates village properties")
    void testRecalculateVillageEquity() {
        Village village = new Village("Test Village", "Yavatmal", "Ghatanji", 5000,
                20.1, 78.2, "Ghatanji RH", 45.0, 90, 0.25, null);
        village.setDaysSinceLastCamp(250);

        calculator.recalculateVillageEquity(village);

        assertTrue(village.getAccessScore() < 40.0);
        assertEquals("RED", village.getVulnerabilityCategory());
    }
}

