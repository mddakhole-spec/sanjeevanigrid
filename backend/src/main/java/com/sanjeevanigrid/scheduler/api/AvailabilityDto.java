package com.sanjeevanigrid.scheduler.api;

import java.time.LocalTime;

public record AvailabilityDto(
        int dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        LocalTime breakStartTime,
        LocalTime breakEndTime,
        int slotDurationMins,
        boolean isActive
) {}
