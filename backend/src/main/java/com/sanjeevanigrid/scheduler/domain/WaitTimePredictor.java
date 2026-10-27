package com.sanjeevanigrid.scheduler.domain;

import org.springframework.stereotype.Component;

/**
 * Wait-time prediction service.
 * Formula: wait time = queue position x rolling average consultation time.
 * Automatically updates rolling average when a doctor completes a consultation.
 */
@Component
public class WaitTimePredictor {

    public static final double DEFAULT_CONSULTATION_MINS = 15.0;

    /**
     * Compute predicted wait time in minutes.
     * @param queuePosition 0-indexed position in line (0 = currently being seen or next up with 0 wait)
     * @param rollingAvgMins doctor's rolling average consultation duration in minutes
     * @return predicted wait time in minutes (0 if at head of queue)
     */
    public int predictWaitTimeMins(int queuePosition, double rollingAvgMins) {
        if (queuePosition <= 0) return 0;
        double avg = rollingAvgMins > 0 ? rollingAvgMins : DEFAULT_CONSULTATION_MINS;
        return (int) Math.round(queuePosition * avg);
    }

    /**
     * Update rolling average consultation time.
     * @param currentAvg current average duration
     * @param completedCount number of completed consultations before this one
     * @param actualDuration actual duration in minutes for the newly completed consultation
     * @return updated rolling average duration in minutes
     */
    public double updateRollingAverage(double currentAvg, int completedCount, double actualDuration) {
        if (completedCount < 0) completedCount = 0;
        if (actualDuration <= 0) actualDuration = DEFAULT_CONSULTATION_MINS;
        if (completedCount == 0) return actualDuration;

        return ((currentAvg * completedCount) + actualDuration) / (completedCount + 1);
    }
}
