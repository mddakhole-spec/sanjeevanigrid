package com.sanjeevanigrid.integration.domain;

import com.sanjeevanigrid.blood.domain.BloodDonor;
import com.sanjeevanigrid.blood.domain.BloodGroup;
import com.sanjeevanigrid.blood.domain.BloodStock;
import com.sanjeevanigrid.blood.infra.BloodDonorRepository;
import com.sanjeevanigrid.blood.infra.BloodStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * Validates and previews CSV imports for Blood Stocks, Donors, and Bed Availability.
 * Implements column auto-detection, schema verification, and row-by-row error reporting.
 */
@Component
public class CsvValidationEngine {

    private static final Logger log = LoggerFactory.getLogger(CsvValidationEngine.class);

    public record RowError(
            int rowNumber,
            String column,
            String message,
            String rawValue
    ) {}

    public record PreviewRow(
            int rowNumber,
            String status, // VALID or INVALID
            Map<String, String> data,
            List<String> rowErrors
    ) {}

    public record CsvPreviewResult(
            String entityType,
            int totalRows,
            int validRows,
            int invalidRows,
            List<String> detectedColumns,
            Map<String, String> columnMapping,
            List<RowError> errors,
            List<PreviewRow> previewRows
    ) {}

    private final BloodStockRepository stockRepo;
    private final BloodDonorRepository donorRepo;

    public CsvValidationEngine(BloodStockRepository stockRepo, BloodDonorRepository donorRepo) {
        this.stockRepo = stockRepo;
        this.donorRepo = donorRepo;
    }

    public CsvPreviewResult previewCsv(String entityType, String csvContent, Map<String, String> customMapping) {
        if (csvContent == null || csvContent.isBlank()) {
            return new CsvPreviewResult(entityType, 0, 0, 0, List.of(), Map.of(),
                    List.of(new RowError(0, "file", "CSV content is empty", "")), List.of());
        }

        String[] rawLines = csvContent.split("\\r?\\n");
        List<String> lines = Arrays.stream(rawLines)
                .map(String::trim)
                .filter(l -> !l.isEmpty())
                .toList();

        if (lines.isEmpty()) {
            return new CsvPreviewResult(entityType, 0, 0, 0, List.of(), Map.of(),
                    List.of(new RowError(0, "file", "CSV has no data lines", "")), List.of());
        }

        // Header parsing
        List<String> headers = parseCsvRow(lines.get(0));
        Map<String, String> effectiveMapping = (customMapping != null && !customMapping.isEmpty())
                ? customMapping
                : detectColumnMapping(entityType, headers);

        List<RowError> allErrors = new ArrayList<>();
        List<PreviewRow> previewRows = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;

        for (int i = 1; i < lines.size(); i++) {
            int rowNum = i + 1;
            List<String> rowValues = parseCsvRow(lines.get(i));
            Map<String, String> rowMap = new LinkedHashMap<>();

            for (int h = 0; h < headers.size(); h++) {
                String val = (h < rowValues.size()) ? rowValues.get(h) : "";
                rowMap.put(headers.get(h), val);
            }

            List<RowError> rowErrors = validateRow(entityType, rowNum, rowMap, effectiveMapping);

            if (rowErrors.isEmpty()) {
                validCount++;
                previewRows.add(new PreviewRow(rowNum, "VALID", rowMap, List.of()));
            } else {
                invalidCount++;
                allErrors.addAll(rowErrors);
                List<String> errorMsgs = rowErrors.stream().map(e -> e.column() + ": " + e.message()).toList();
                previewRows.add(new PreviewRow(rowNum, "INVALID", rowMap, errorMsgs));
            }
        }

        return new CsvPreviewResult(
                entityType,
                lines.size() - 1,
                validCount,
                invalidCount,
                headers,
                effectiveMapping,
                allErrors,
                previewRows
        );
    }

    public int commitCsv(UUID hospitalId, String entityType, String csvContent, Map<String, String> customMapping) {
        CsvPreviewResult preview = previewCsv(entityType, csvContent, customMapping);
        int committed = 0;

        for (PreviewRow row : preview.previewRows()) {
            if (!"VALID".equals(row.status())) continue;

            Map<String, String> d = row.data();
            Map<String, String> m = preview.columnMapping();

            try {
                if ("BLOOD_STOCK".equalsIgnoreCase(entityType)) {
                    BloodGroup bg = BloodGroup.fromString(getMappedValue(d, m, "bloodGroup"));
                    int units = Integer.parseInt(getMappedValue(d, m, "unitsAvailable"));
                    String expStr = getMappedValue(d, m, "expiryDate");
                    LocalDate expiry = (expStr != null && !expStr.isBlank()) ? LocalDate.parse(expStr) : LocalDate.now().plusDays(30);
                    int threshold = 5;
                    String threshStr = getMappedValue(d, m, "lowStockThreshold");
                    if (threshStr != null && !threshStr.isBlank()) {
                        threshold = Integer.parseInt(threshStr);
                    }

                    Optional<BloodStock> existing = stockRepo.findByHospitalIdAndBloodGroupAndExpiryDate(hospitalId, bg, expiry);
                    BloodStock stock = existing.orElseGet(() -> new BloodStock(hospitalId, bg, 0, expiry, 5));
                    stock.setUnitsAvailable(units);
                    stock.setLowStockThreshold(threshold);
                    stockRepo.save(stock);
                    committed++;

                } else if ("DONORS".equalsIgnoreCase(entityType)) {
                    String name = getMappedValue(d, m, "fullName");
                    String phone = getMappedValue(d, m, "phone");
                    BloodGroup bg = BloodGroup.fromString(getMappedValue(d, m, "bloodGroup"));
                    double lat = Double.parseDouble(getMappedValue(d, m, "latitude"));
                    double lon = Double.parseDouble(getMappedValue(d, m, "longitude"));
                    String address = getMappedValue(d, m, "address");
                    String district = getMappedValue(d, m, "district");

                    Optional<BloodDonor> existing = donorRepo.findByPhone(phone);
                    BloodDonor donor = existing.orElseGet(() -> new BloodDonor(name, phone, bg, lat, lon, address, district));
                    donor.setFullName(name);
                    donor.setBloodGroup(bg);
                    donor.setLatitude(lat);
                    donor.setLongitude(lon);
                    donorRepo.save(donor);
                    committed++;
                }
            } catch (Exception ex) {
                log.warn("Failed to commit CSV row {}: {}", row.rowNumber(), ex.getMessage());
            }
        }

        return committed;
    }

    private List<RowError> validateRow(String entityType, int rowNum, Map<String, String> row, Map<String, String> mapping) {
        List<RowError> errors = new ArrayList<>();

        if ("BLOOD_STOCK".equalsIgnoreCase(entityType)) {
            // Validate bloodGroup
            String bgVal = getMappedValue(row, mapping, "bloodGroup");
            if (bgVal == null || bgVal.isBlank()) {
                errors.add(new RowError(rowNum, "bloodGroup", "Blood group is required", ""));
            } else {
                try {
                    BloodGroup.fromString(bgVal);
                } catch (Exception e) {
                    errors.add(new RowError(rowNum, "bloodGroup", "Invalid blood group: " + bgVal + " (e.g. A+, B-, O+)", bgVal));
                }
            }

            // Validate units
            String unitsVal = getMappedValue(row, mapping, "unitsAvailable");
            if (unitsVal == null || unitsVal.isBlank()) {
                errors.add(new RowError(rowNum, "unitsAvailable", "Units available is required", ""));
            } else {
                try {
                    int u = Integer.parseInt(unitsVal.trim());
                    if (u < 0) errors.add(new RowError(rowNum, "unitsAvailable", "Units must be non-negative", unitsVal));
                } catch (NumberFormatException e) {
                    errors.add(new RowError(rowNum, "unitsAvailable", "Must be a valid integer", unitsVal));
                }
            }

            // Validate expiry date if present
            String expVal = getMappedValue(row, mapping, "expiryDate");
            if (expVal != null && !expVal.isBlank()) {
                try {
                    LocalDate.parse(expVal.trim());
                } catch (Exception e) {
                    errors.add(new RowError(rowNum, "expiryDate", "Invalid date format, expected YYYY-MM-DD", expVal));
                }
            }

        } else if ("DONORS".equalsIgnoreCase(entityType)) {
            String name = getMappedValue(row, mapping, "fullName");
            if (name == null || name.isBlank()) errors.add(new RowError(rowNum, "fullName", "Full name is required", ""));

            String phone = getMappedValue(row, mapping, "phone");
            if (phone == null || phone.isBlank()) {
                errors.add(new RowError(rowNum, "phone", "Phone is required", ""));
            } else if (phone.replaceAll("[^0-9]", "").length() < 10) {
                errors.add(new RowError(rowNum, "phone", "Invalid phone number (minimum 10 digits)", phone));
            }

            String bgVal = getMappedValue(row, mapping, "bloodGroup");
            if (bgVal != null && !bgVal.isBlank()) {
                try {
                    BloodGroup.fromString(bgVal);
                } catch (Exception e) {
                    errors.add(new RowError(rowNum, "bloodGroup", "Invalid blood group", bgVal));
                }
            } else {
                errors.add(new RowError(rowNum, "bloodGroup", "Blood group is required", ""));
            }

            String latVal = getMappedValue(row, mapping, "latitude");
            if (latVal != null && !latVal.isBlank()) {
                try {
                    double lat = Double.parseDouble(latVal.trim());
                    if (lat < -90 || lat > 90) errors.add(new RowError(rowNum, "latitude", "Latitude must be between -90 and 90", latVal));
                } catch (NumberFormatException e) {
                    errors.add(new RowError(rowNum, "latitude", "Invalid decimal latitude", latVal));
                }
            }

            String lonVal = getMappedValue(row, mapping, "longitude");
            if (lonVal != null && !lonVal.isBlank()) {
                try {
                    double lon = Double.parseDouble(lonVal.trim());
                    if (lon < -180 || lon > 180) errors.add(new RowError(rowNum, "longitude", "Longitude must be between -180 and 180", lonVal));
                } catch (NumberFormatException e) {
                    errors.add(new RowError(rowNum, "longitude", "Invalid decimal longitude", lonVal));
                }
            }
        }

        return errors;
    }

    private String getMappedValue(Map<String, String> row, Map<String, String> mapping, String targetField) {
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(targetField)) {
                return row.get(entry.getKey());
            }
        }
        return row.get(targetField);
    }

    private Map<String, String> detectColumnMapping(String entityType, List<String> headers) {
        Map<String, String> mapping = new HashMap<>();

        for (String h : headers) {
            String norm = h.toLowerCase().replaceAll("[^a-z0-9]", "");
            if ("BLOOD_STOCK".equalsIgnoreCase(entityType)) {
                if (norm.contains("group") || norm.contains("type") || norm.contains("blood")) mapping.put(h, "bloodGroup");
                else if (norm.contains("unit") || norm.contains("avail") || norm.contains("count") || norm.contains("qty")) mapping.put(h, "unitsAvailable");
                else if (norm.contains("expir") || norm.contains("date")) mapping.put(h, "expiryDate");
                else if (norm.contains("thresh") || norm.contains("limit") || norm.contains("low")) mapping.put(h, "lowStockThreshold");
            } else if ("DONORS".equalsIgnoreCase(entityType)) {
                if (norm.contains("name")) mapping.put(h, "fullName");
                else if (norm.contains("phone") || norm.contains("mobile") || norm.contains("contact")) mapping.put(h, "phone");
                else if (norm.contains("group") || norm.contains("blood")) mapping.put(h, "bloodGroup");
                else if (norm.contains("lat")) mapping.put(h, "latitude");
                else if (norm.contains("lon") || norm.contains("lng")) mapping.put(h, "longitude");
                else if (norm.contains("addr")) mapping.put(h, "address");
                else if (norm.contains("dist")) mapping.put(h, "district");
            }
        }
        return mapping;
    }

    public static List<String> parseCsvRow(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens;
    }
}
