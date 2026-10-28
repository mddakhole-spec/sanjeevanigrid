package com.sanjeevanigrid.equity.api;

import com.sanjeevanigrid.equity.domain.CampRecommendationDto;
import com.sanjeevanigrid.equity.domain.Village;
import com.sanjeevanigrid.equity.facade.EquityFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/equity")
@Tag(name = "Equity Map & Recommender", description = "GIS access scores, vulnerability heatmaps, and next-camp recommendation engine")
public class EquityMapController {

    private final EquityFacade equityFacade;
    private final JdbcTemplate jdbcTemplate;

    public EquityMapController(EquityFacade equityFacade, JdbcTemplate jdbcTemplate) {
        this.equityFacade = equityFacade;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/villages")
    @Operation(summary = "List all villages with access scores and vulnerability categories")
    public ResponseEntity<List<Village>> getAllVillages() {
        return ResponseEntity.ok(equityFacade.getAllVillages());
    }

    @GetMapping("/villages/{id}")
    @Operation(summary = "Get detailed village access profile")
    public ResponseEntity<Village> getVillage(@PathVariable UUID id) {
        return equityFacade.getVillageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Get top ranked villages where the next medical camp should be deployed")
    public ResponseEntity<List<CampRecommendationDto>> getRecommendations(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(equityFacade.getRecommendations(limit));
    }

    @GetMapping("/map-data")
    @Operation(summary = "Aggregated GIS data for Leaflet map: villages and hospitals with coordinates and capacity")
    public ResponseEntity<Map<String, Object>> getMapData() {
        List<Village> villages = equityFacade.getAllVillages();

        // Query hospitals with coordinates extracted from PostGIS geometry
        String sql = "SELECT id, name, district, address, phone, " +
                     "ST_Y(location) as latitude, ST_X(location) as longitude " +
                     "FROM hospitals WHERE location IS NOT NULL";

        List<Map<String, Object>> hospitalMaps = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> h = new HashMap<>();
            h.put("id", rs.getString("id"));
            h.put("name", rs.getString("name"));
            h.put("district", rs.getString("district"));
            h.put("address", rs.getString("address"));
            h.put("phone", rs.getString("phone"));
            h.put("latitude", rs.getDouble("latitude"));
            h.put("longitude", rs.getDouble("longitude"));
            return h;
        });

        // Query resources for each hospital from wards if present
        String resSql = "SELECT hospital_id, SUM(total_beds) as beds, SUM(total_icu_beds) as icu, SUM(total_ventilators) as vents FROM wards GROUP BY hospital_id";
        Map<String, Map<String, Integer>> resMap = new HashMap<>();
        try {
            jdbcTemplate.query(resSql, rs -> {
                String hid = rs.getString("hospital_id");
                Map<String, Integer> map = new HashMap<>();
                map.put("GENERAL_BEDS", rs.getInt("beds"));
                map.put("ICU_BEDS", rs.getInt("icu"));
                map.put("VENTILATORS", rs.getInt("vents"));
                resMap.put(hid, map);
            });
        } catch (Exception ignored) {}

        for (Map<String, Object> h : hospitalMaps) {
            String hid = (String) h.get("id");
            Map<String, Integer> resources = resMap.getOrDefault(hid, Map.of());
            h.put("availableBeds", resources.getOrDefault("GENERAL_BEDS", 35));
            h.put("availableIcu", resources.getOrDefault("ICU_BEDS", 6));
            h.put("availableVentilators", resources.getOrDefault("VENTILATORS", 3));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("villages", villages);
        response.put("hospitals", hospitalMaps);
        response.put("totalVillages", villages.size());
        response.put("redVillagesCount", villages.stream().filter(v -> "RED".equalsIgnoreCase(v.getVulnerabilityCategory())).count());
        response.put("yellowVillagesCount", villages.stream().filter(v -> "YELLOW".equalsIgnoreCase(v.getVulnerabilityCategory())).count());
        response.put("greenVillagesCount", villages.stream().filter(v -> "GREEN".equalsIgnoreCase(v.getVulnerabilityCategory())).count());

        return ResponseEntity.ok(response);
    }
}

