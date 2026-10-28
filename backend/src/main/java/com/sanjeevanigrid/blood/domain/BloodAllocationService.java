package com.sanjeevanigrid.blood.domain;

import com.sanjeevanigrid.blood.infra.*;
import com.sanjeevanigrid.common.event.BloodRequestCreated;
import com.sanjeevanigrid.common.event.DonorAlerted;
import com.sanjeevanigrid.common.event.StockLow;
import com.sanjeevanigrid.integration.domain.Hospital;
import com.sanjeevanigrid.integration.infra.HospitalRepository;
import com.sanjeevanigrid.notification.infra.SmsSandboxAdapter;
import com.sanjeevanigrid.notification.infra.TelegramAdapter;
import com.sanjeevanigrid.notification.infra.WhatsAppLinkAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Orchestrates blood fulfillment using the "Stock First, Donors Second" rule.
 * 1. Checks hospital inventory first for matching/compatible blood stock.
 * 2. If stock is insufficient, triggers PostGIS spatial search for eligible nearby donors,
 *    ranks them by reliability score, and dispatches alerts via WhatsApp Link, SMS, and Telegram.
 */
@Service
public class BloodAllocationService {

    private static final Logger log = LoggerFactory.getLogger(BloodAllocationService.class);

    public record AllocationResult(
            BloodRequest request,
            int unitsRequested,
            int unitsFromStock,
            int unitsFromDonorsAlerted,
            String allocationSummary,
            List<PostGisDonorGeoService.RankedDonor> alertedDonors,
            List<BloodDonorAlert> alerts
    ) {}

    private final BloodStockRepository stockRepo;
    private final BloodDonorRepository donorRepo;
    private final BloodRequestRepository requestRepo;
    private final BloodDonorAlertRepository alertRepo;
    private final PostGisDonorGeoService geoService;
    private final HospitalRepository hospitalRepo;
    private final WhatsAppLinkAdapter whatsAppAdapter;
    private final SmsSandboxAdapter smsSandbox;
    private final TelegramAdapter telegramAdapter;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.blood.default-search-radius-km:35.0}")
    private double defaultSearchRadiusKm;

    public BloodAllocationService(BloodStockRepository stockRepo,
                                  BloodDonorRepository donorRepo,
                                  BloodRequestRepository requestRepo,
                                  BloodDonorAlertRepository alertRepo,
                                  PostGisDonorGeoService geoService,
                                  HospitalRepository hospitalRepo,
                                  WhatsAppLinkAdapter whatsAppAdapter,
                                  SmsSandboxAdapter smsSandbox,
                                  TelegramAdapter telegramAdapter,
                                  ApplicationEventPublisher eventPublisher) {
        this.stockRepo = stockRepo;
        this.donorRepo = donorRepo;
        this.requestRepo = requestRepo;
        this.alertRepo = alertRepo;
        this.geoService = geoService;
        this.hospitalRepo = hospitalRepo;
        this.whatsAppAdapter = whatsAppAdapter;
        this.smsSandbox = smsSandbox;
        this.telegramAdapter = telegramAdapter;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AllocationResult requestBlood(UUID hospitalId, String patientName, String patientPhone,
                                         BloodGroup bloodGroup, int unitsNeeded, String urgency,
                                         Double customRadiusKm) {
        Hospital hospital = hospitalRepo.findById(hospitalId).orElse(null);
        String hospitalName = (hospital != null) ? hospital.getName() : "Yavatmal District Hospital";
        double centerLat = (hospital != null) ? hospital.getLatitude() : 20.3888;
        double centerLon = (hospital != null) ? hospital.getLongitude() : 78.1204;
        double searchRadius = (customRadiusKm != null && customRadiusKm > 0) ? customRadiusKm : defaultSearchRadiusKm;

        String reqNum = "REQ-BLD-" + System.currentTimeMillis() % 1000000;
        BloodRequest request = new BloodRequest(reqNum, hospitalId, patientName, patientPhone, bloodGroup, unitsNeeded, urgency);

        int unitsAllocatedFromStock = 0;
        List<PostGisDonorGeoService.RankedDonor> alertedDonors = new ArrayList<>();
        List<BloodDonorAlert> alertsCreated = new ArrayList<>();

        // ============================================================
        // STEP 1: STOCK FIRST
        // ============================================================
        List<BloodStock> availableStocks = stockRepo.findAvailableStockEarliestExpiry(hospitalId, bloodGroup, LocalDate.now());
        int remainingNeeded = unitsNeeded;

        for (BloodStock stock : availableStocks) {
            if (remainingNeeded <= 0) break;
            int availableInBatch = stock.getUnitsAvailable() - stock.getUnitsReserved();
            if (availableInBatch > 0) {
                int toDeduct = Math.min(availableInBatch, remainingNeeded);
                stock.deductUnits(toDeduct);
                stockRepo.save(stock);
                unitsAllocatedFromStock += toDeduct;
                remainingNeeded -= toDeduct;

                // Check if stock dropped below low-stock threshold
                if (stock.isLowStock()) {
                    log.warn("STOCK LOW ALERT: Blood group {} at {} has only {} units left (threshold: {})",
                            bloodGroup, hospitalName, stock.getUnitsAvailable(), stock.getLowStockThreshold());
                    eventPublisher.publishEvent(new StockLow(hospitalId, hospitalName, bloodGroup.name(),
                            stock.getUnitsAvailable(), stock.getLowStockThreshold()));
                }
            }
        }

        request.setUnitsAllocatedFromStock(unitsAllocatedFromStock);

        // If stock fully satisfied the request:
        if (remainingNeeded == 0) {
            request.setStatus("STOCK_ALLOCATED");
            request.setFulfillmentSource("STOCK");
            request.setNotes("Fully allocated " + unitsAllocatedFromStock + " unit(s) from hospital blood bank stock.");
            requestRepo.save(request);

            eventPublisher.publishEvent(new BloodRequestCreated(request.getId(), request.getRequestNumber(),
                    hospitalId, hospitalName, bloodGroup.name(), unitsNeeded, urgency));

            String summary = String.format("Request #%s fulfilled: %d/%d units allocated immediately from blood bank stock.",
                    reqNum, unitsAllocatedFromStock, unitsNeeded);
            return new AllocationResult(request, unitsNeeded, unitsAllocatedFromStock, 0, summary, List.of(), List.of());
        }

        // ============================================================
        // STEP 2: DONORS SECOND (for remaining deficit)
        // ============================================================
        log.info("Stock deficit of {} unit(s) for group {} at {}. Initiating nearby donor search within {}km...",
                remainingNeeded, bloodGroup, hospitalName, searchRadius);

        List<BloodGroup> compatibleGroups = bloodGroup.getCompatibleDonors();
        List<PostGisDonorGeoService.RankedDonor> rankedDonors = geoService.searchAndRankDonors(
                centerLat, centerLon, searchRadius, compatibleGroups);

        // Pick top ranked donors (up to 5 donors to alert)
        int alertLimit = Math.min(5, rankedDonors.size());
        for (int i = 0; i < alertLimit; i++) {
            PostGisDonorGeoService.RankedDonor ranked = rankedDonors.get(i);
            BloodDonor donor = ranked.donor();

            // Construct emergency message
            String alertMsg = String.format("URGENT Blood Alert: %s urgently requires %d unit(s) of %s blood for patient %s. Distance: %.1f km. Can you donate? Reply YES or visit hospital.",
                    hospitalName, remainingNeeded, bloodGroup.getLabel(), patientName, ranked.distanceKm());

            // 1. Generate WhatsApp Link
            String waLink = whatsAppAdapter.sendWhatsApp(donor.getPhone(), alertMsg);

            // 2. Dispatch SMS Simulator Inbox notification
            smsSandbox.sendSms(donor.getPhone(), "[BLOOD_ALERT] " + alertMsg);

            // 3. Dispatch Telegram alert (if enabled by config flag)
            telegramAdapter.sendTelegram(null, "[BLOOD_ALERT] " + alertMsg + " (Donor: " + donor.getFullName() + ")");

            // Record donor alert
            BloodDonorAlert alert = new BloodDonorAlert(
                    request.getId(), donor.getId(), "WHATSAPP", i + 1,
                    ranked.reliabilityScore(), ranked.distanceKm(), waLink
            );
            alert.setSmsStatus("SENT");
            alert.setTelegramStatus(telegramAdapter.isEnabled() ? "SENT" : "DISABLED_BY_FLAG");
            alertRepo.save(alert);

            // Update donor stats
            donor.recordAlertSent();
            donorRepo.save(donor);

            alertedDonors.add(ranked);
            alertsCreated.add(alert);

            eventPublisher.publishEvent(new DonorAlerted(
                    request.getId(), donor.getId(), donor.getFullName(), donor.getPhone(),
                    donor.getBloodGroup().name(), "WHATSAPP", ranked.reliabilityScore(), ranked.distanceKm()
            ));
        }

        if (unitsAllocatedFromStock > 0) {
            request.setStatus("PARTIALLY_FULFILLED");
            request.setFulfillmentSource("MIXED");
            request.setNotes(String.format("Partially allocated %d unit(s) from stock; alerted %d nearby donor(s) for remaining %d unit(s).",
                    unitsAllocatedFromStock, alertedDonors.size(), remainingNeeded));
        } else {
            request.setStatus("ALERTING_DONORS");
            request.setFulfillmentSource("DONORS");
            request.setNotes(String.format("No stock available. Alerted %d nearby verified donor(s) ranked by reliability score.",
                    alertedDonors.size()));
        }

        requestRepo.save(request);

        eventPublisher.publishEvent(new BloodRequestCreated(request.getId(), request.getRequestNumber(),
                hospitalId, hospitalName, bloodGroup.name(), unitsNeeded, urgency));

        String summary = String.format("Request #%s: %d unit(s) allocated from stock, %d donor(s) alerted via WhatsApp/SMS (deficit: %d units).",
                reqNum, unitsAllocatedFromStock, alertedDonors.size(), remainingNeeded);

        return new AllocationResult(request, unitsNeeded, unitsAllocatedFromStock, alertedDonors.size(), summary, alertedDonors, alertsCreated);
    }
}
