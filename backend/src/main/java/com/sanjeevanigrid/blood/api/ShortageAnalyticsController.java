package com.sanjeevanigrid.blood.api;

import com.sanjeevanigrid.blood.domain.BloodGroup;
import com.sanjeevanigrid.blood.domain.BloodStock;
import com.sanjeevanigrid.blood.infra.BloodStockRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/blood/analytics")
public class ShortageAnalyticsController {

    private static final UUID DEFAULT_HOSPITAL_ID = UUID.fromString("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d");

    private final BloodStockRepository stockRepo;

    public ShortageAnalyticsController(BloodStockRepository stockRepo) {
        this.stockRepo = stockRepo;
    }

    public record GroupAnalytics(
            String bloodGroup,
            String label,
            int unitsAvailable,
            int unitsReserved,
            int safeBuffer,
            int netDeficitOrSurplus,
            int expiringNext7Days,
            int expiringNext14Days,
            String status, // CRITICAL_SHORTAGE, LOW_STOCK, ADEQUATE, SURPLUS
            double estimatedDaysOfSupply
    ) {}

    public record ShortageReport(
            UUID hospitalId,
            int totalUnitsInStock,
            int totalExpiringSoon,
            int criticalShortageCount,
            double overallDistrictHealthScore,
            List<GroupAnalytics> groupBreakdown,
            List<String> recommendations
    ) {}

    @GetMapping("/shortage")
    public ResponseEntity<ShortageReport> getShortageAnalytics(@RequestParam(required = false) UUID hospitalId) {
        UUID targetId = (hospitalId != null) ? hospitalId : DEFAULT_HOSPITAL_ID;
        List<BloodStock> stocks = stockRepo.findByHospitalId(targetId);

        Map<BloodGroup, Integer> availableMap = new EnumMap<>(BloodGroup.class);
        Map<BloodGroup, Integer> reservedMap = new EnumMap<>(BloodGroup.class);
        Map<BloodGroup, Integer> exp7Map = new EnumMap<>(BloodGroup.class);
        Map<BloodGroup, Integer> exp14Map = new EnumMap<>(BloodGroup.class);

        LocalDate today = LocalDate.now();
        LocalDate next7 = today.plusDays(7);
        LocalDate next14 = today.plusDays(14);

        int totalUnits = 0;
        int totalExpiringSoon = 0;

        for (BloodStock s : stocks) {
            BloodGroup bg = s.getBloodGroup();
            availableMap.put(bg, availableMap.getOrDefault(bg, 0) + s.getUnitsAvailable());
            reservedMap.put(bg, reservedMap.getOrDefault(bg, 0) + s.getUnitsReserved());
            totalUnits += s.getUnitsAvailable();

            if (s.getExpiryDate() != null) {
                if (!s.getExpiryDate().isBefore(today) && !s.getExpiryDate().isAfter(next7)) {
                    exp7Map.put(bg, exp7Map.getOrDefault(bg, 0) + s.getUnitsAvailable());
                    totalExpiringSoon += s.getUnitsAvailable();
                } else if (!s.getExpiryDate().isBefore(today) && !s.getExpiryDate().isAfter(next14)) {
                    exp14Map.put(bg, exp14Map.getOrDefault(bg, 0) + s.getUnitsAvailable());
                    totalExpiringSoon += s.getUnitsAvailable();
                }
            }
        }

        List<GroupAnalytics> breakdown = new ArrayList<>();
        int criticalCount = 0;
        int healthyPoints = 0;
        List<String> recommendations = new ArrayList<>();

        for (BloodGroup bg : BloodGroup.values()) {
            int available = availableMap.getOrDefault(bg, 0);
            int reserved = reservedMap.getOrDefault(bg, 0);
            int safeBuffer = 10; // Target safe buffer units
            int net = available - safeBuffer;
            int exp7 = exp7Map.getOrDefault(bg, 0);
            int exp14 = exp14Map.getOrDefault(bg, 0);

            String status;
            double daysOfSupply;

            if (available <= 2) {
                status = "CRITICAL_SHORTAGE";
                criticalCount++;
                daysOfSupply = Math.round((available / 1.5) * 10.0) / 10.0;
                recommendations.add(String.format("Critical deficit in %s: Schedule emergency donor drive or mobilize voluntary donors immediately.", bg.getLabel()));
            } else if (available <= 5) {
                status = "LOW_STOCK";
                criticalCount++;
                daysOfSupply = Math.round((available / 1.5) * 10.0) / 10.0;
                healthyPoints += 1;
            } else if (available <= 15) {
                status = "ADEQUATE";
                daysOfSupply = Math.round((available / 1.2) * 10.0) / 10.0;
                healthyPoints += 2;
            } else {
                status = "SURPLUS";
                daysOfSupply = Math.round((available / 1.2) * 10.0) / 10.0;
                healthyPoints += 3;
            }

            breakdown.add(new GroupAnalytics(
                    bg.name(), bg.getLabel(), available, reserved, safeBuffer, net, exp7, exp14, status, daysOfSupply
            ));
        }

        double districtHealthScore = Math.min(100.0, Math.round(((double) healthyPoints / (BloodGroup.values().length * 3)) * 1000.0) / 10.0);

        if (totalExpiringSoon > 0) {
            recommendations.add(String.format("%d unit(s) expiring within 14 days. Prioritize these for non-emergency transfusions or elective surgeries.", totalExpiringSoon));
        }

        return ResponseEntity.ok(new ShortageReport(
                targetId, totalUnits, totalExpiringSoon, criticalCount, districtHealthScore, breakdown, recommendations
        ));
    }
}
