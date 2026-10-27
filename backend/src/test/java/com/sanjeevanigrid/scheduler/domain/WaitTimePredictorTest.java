package com.sanjeevanigrid.scheduler.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class WaitTimePredictorTest {

    private WaitTimePredictor predictor;

    @BeforeEach
    void setUp() {
        predictor = new WaitTimePredictor();
    }

    @Test
    @DisplayName("Should predict wait time based on queue position x rolling avg")
    void testWaitTimeCalculation() {
        // 3rd in queue with 14.5 min rolling average
        // 3 * 14.5 = 43.5 -> rounded to 44
        int waitMins = predictor.predictWaitTimeMins(3, 14.5);
        assertThat(waitMins).isEqualTo(44);

        // 1st in queue with 15.0 min rolling average -> 15 mins
        assertThat(predictor.predictWaitTimeMins(1, 15.0)).isEqualTo(15);

        // Position 0 (at head or in consultation) -> 0 mins
        assertThat(predictor.predictWaitTimeMins(0, 15.0)).isEqualTo(0);
        assertThat(predictor.predictWaitTimeMins(-1, 15.0)).isEqualTo(0);
    }

    @Test
    @DisplayName("Should correctly update rolling average consultation duration")
    void testRollingAverageUpdate() {
        // Initially 10 completed consultations averaging 15.0 mins
        // Next consultation takes 20.0 mins
        // New avg = (10 * 15.0 + 20.0) / 11 = 170 / 11 = 15.4545...
        double newAvg = predictor.updateRollingAverage(15.0, 10, 20.0);
        assertThat(newAvg).isCloseTo(15.45, within(0.01));

        // First completed consultation (count 0) taking 12.5 mins -> new avg is 12.5
        double firstAvg = predictor.updateRollingAverage(15.0, 0, 12.5);
        assertThat(firstAvg).isEqualTo(12.5);
    }
}
