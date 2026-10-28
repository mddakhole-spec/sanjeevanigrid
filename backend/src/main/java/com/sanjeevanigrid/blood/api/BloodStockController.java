package com.sanjeevanigrid.blood.api;

import com.sanjeevanigrid.blood.domain.BloodGroup;
import com.sanjeevanigrid.blood.domain.BloodStock;
import com.sanjeevanigrid.blood.infra.BloodStockRepository;
import com.sanjeevanigrid.common.event.StockLow;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/blood/stocks")
public class BloodStockController {

    private static final UUID DEFAULT_HOSPITAL_ID = UUID.fromString("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d");

    private final BloodStockRepository stockRepo;
    private final ApplicationEventPublisher eventPublisher;

    public BloodStockController(BloodStockRepository stockRepo, ApplicationEventPublisher eventPublisher) {
        this.stockRepo = stockRepo;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping
    public ResponseEntity<List<BloodStock>> getStocks(@RequestParam(required = false) UUID hospitalId) {
        UUID targetId = (hospitalId != null) ? hospitalId : DEFAULT_HOSPITAL_ID;
        return ResponseEntity.ok(stockRepo.findByHospitalId(targetId));
    }

    @GetMapping("/low")
    public ResponseEntity<List<BloodStock>> getLowStock(@RequestParam(required = false) UUID hospitalId) {
        if (hospitalId != null) {
            return ResponseEntity.ok(stockRepo.findLowStockForHospital(hospitalId));
        }
        return ResponseEntity.ok(stockRepo.findAllLowStock());
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<BloodStock>> getExpiringStock(
            @RequestParam(required = false) UUID hospitalId,
            @RequestParam(defaultValue = "14") int days) {
        UUID targetId = (hospitalId != null) ? hospitalId : DEFAULT_HOSPITAL_ID;
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(days);
        return ResponseEntity.ok(stockRepo.findExpiringSoon(targetId, today, cutoff));
    }

    @PostMapping("/update")
    public ResponseEntity<BloodStock> updateStock(@RequestBody Map<String, Object> body) {
        UUID hospitalId = body.containsKey("hospitalId") ? UUID.fromString(body.get("hospitalId").toString()) : DEFAULT_HOSPITAL_ID;
        BloodGroup bloodGroup = BloodGroup.fromString(body.get("bloodGroup").toString());
        int units = Integer.parseInt(body.get("units").toString());
        LocalDate expiryDate = body.containsKey("expiryDate")
                ? LocalDate.parse(body.get("expiryDate").toString())
                : LocalDate.now().plusDays(30);

        Optional<BloodStock> existingOpt = stockRepo.findByHospitalIdAndBloodGroupAndExpiryDate(hospitalId, bloodGroup, expiryDate);
        BloodStock stock;
        if (existingOpt.isPresent()) {
            stock = existingOpt.get();
            stock.setUnitsAvailable(units);
        } else {
            stock = new BloodStock(hospitalId, bloodGroup, units, expiryDate, 5);
        }

        BloodStock saved = stockRepo.save(stock);
        if (saved.isLowStock()) {
            eventPublisher.publishEvent(new StockLow(hospitalId, "Hospital", bloodGroup.name(), saved.getUnitsAvailable(), saved.getLowStockThreshold()));
        }

        return ResponseEntity.ok(saved);
    }
}
