package com.sanjeevanigrid.blood.domain;

import com.sanjeevanigrid.blood.infra.*;
import com.sanjeevanigrid.integration.domain.Hospital;
import com.sanjeevanigrid.integration.infra.HospitalRepository;
import com.sanjeevanigrid.notification.infra.SmsSandboxAdapter;
import com.sanjeevanigrid.notification.infra.TelegramAdapter;
import com.sanjeevanigrid.notification.infra.WhatsAppLinkAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BloodAllocationServiceTest {

    private BloodStockRepository stockRepo;
    private BloodDonorRepository donorRepo;
    private BloodRequestRepository requestRepo;
    private BloodDonorAlertRepository alertRepo;
    private PostGisDonorGeoService geoService;
    private HospitalRepository hospitalRepo;
    private WhatsAppLinkAdapter whatsAppAdapter;
    private SmsSandboxAdapter smsSandbox;
    private TelegramAdapter telegramAdapter;
    private ApplicationEventPublisher eventPublisher;

    private BloodAllocationService allocationService;

    private final UUID hospitalId = UUID.randomUUID();
    private Hospital hospital;

    @BeforeEach
    void setUp() {
        stockRepo = mock(BloodStockRepository.class);
        donorRepo = mock(BloodDonorRepository.class);
        requestRepo = mock(BloodRequestRepository.class);
        alertRepo = mock(BloodDonorAlertRepository.class);
        geoService = mock(PostGisDonorGeoService.class);
        hospitalRepo = mock(HospitalRepository.class);
        whatsAppAdapter = mock(WhatsAppLinkAdapter.class);
        smsSandbox = mock(SmsSandboxAdapter.class);
        telegramAdapter = mock(TelegramAdapter.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        allocationService = new BloodAllocationService(
                stockRepo, donorRepo, requestRepo, alertRepo, geoService, hospitalRepo,
                whatsAppAdapter, smsSandbox, telegramAdapter, eventPublisher
        );

        hospital = new Hospital("Yavatmal Hospital", "LIC-01", "Civil Lines", "Yavatmal", "+919822100000", "test@hospital.in");
        hospital.setId(hospitalId);
        hospital.setLatitude(20.3888);
        hospital.setLongitude(78.1204);
        when(hospitalRepo.findById(hospitalId)).thenReturn(Optional.of(hospital));

        when(requestRepo.save(any(BloodRequest.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Stock First: Fulfills immediately from inventory when stock is sufficient, alerting zero donors")
    void testStockFirstRule() {
        BloodStock stock = new BloodStock(hospitalId, BloodGroup.B_POSITIVE, 10, LocalDate.now().plusDays(20), 5);
        when(stockRepo.findAvailableStockEarliestExpiry(eq(hospitalId), eq(BloodGroup.B_POSITIVE), any(LocalDate.class)))
                .thenReturn(List.of(stock));

        BloodAllocationService.AllocationResult result = allocationService.requestBlood(
                hospitalId, "Patient A", "+919822111111", BloodGroup.B_POSITIVE, 3, "URGENT", 25.0
        );

        assertThat(result.unitsRequested()).isEqualTo(3);
        assertThat(result.unitsFromStock()).isEqualTo(3);
        assertThat(result.unitsFromDonorsAlerted()).isEqualTo(0);
        assertThat(result.request().getStatus()).isEqualTo("STOCK_ALLOCATED");
        assertThat(result.request().getFulfillmentSource()).isEqualTo("STOCK");
        assertThat(stock.getUnitsAvailable()).isEqualTo(7);

        // Verify NO donor alerts were dispatched
        verify(geoService, never()).searchAndRankDonors(anyDouble(), anyDouble(), anyDouble(), anyList());
        verify(whatsAppAdapter, never()).sendWhatsApp(anyString(), anyString());
        verify(smsSandbox, never()).sendSms(anyString(), anyString());
    }

    @Test
    @DisplayName("Donors Second: When stock is empty, alerts nearby eligible donors ranked by reliability")
    void testDonorsSecondRule() {
        when(stockRepo.findAvailableStockEarliestExpiry(eq(hospitalId), eq(BloodGroup.O_NEGATIVE), any(LocalDate.class)))
                .thenReturn(List.of());

        BloodDonor donor1 = new BloodDonor("Anita Deshmukh", "+919822100001", BloodGroup.O_NEGATIVE, 20.39, 78.13, "Yavatmal", "Yavatmal");
        donor1.setId(UUID.randomUUID());
        BloodDonor donor2 = new BloodDonor("Ganesh Rathod", "+919822100002", BloodGroup.O_NEGATIVE, 20.08, 77.93, "Arni", "Yavatmal");
        donor2.setId(UUID.randomUUID());

        PostGisDonorGeoService.RankedDonor ranked1 = new PostGisDonorGeoService.RankedDonor(
                donor1, 2.1, 0.95, null, null);
        PostGisDonorGeoService.RankedDonor ranked2 = new PostGisDonorGeoService.RankedDonor(
                donor2, 28.5, 0.88, null, null);

        when(geoService.searchAndRankDonors(anyDouble(), anyDouble(), anyDouble(), anyList()))
                .thenReturn(List.of(ranked1, ranked2));

        when(whatsAppAdapter.sendWhatsApp(anyString(), anyString())).thenReturn("https://wa.me/919822100001?text=Alert");

        BloodAllocationService.AllocationResult result = allocationService.requestBlood(
                hospitalId, "Emergency Patient", "+919822222222", BloodGroup.O_NEGATIVE, 2, "CRITICAL_EMERGENCY", 35.0
        );

        assertThat(result.unitsFromStock()).isEqualTo(0);
        assertThat(result.unitsFromDonorsAlerted()).isEqualTo(2);
        assertThat(result.request().getStatus()).isEqualTo("ALERTING_DONORS");
        assertThat(result.request().getFulfillmentSource()).isEqualTo("DONORS");

        // Verify alert adapters were invoked
        verify(whatsAppAdapter, times(2)).sendWhatsApp(anyString(), anyString());
        verify(smsSandbox, times(2)).sendSms(anyString(), anyString());
        verify(alertRepo, times(2)).save(any(BloodDonorAlert.class));
    }
}
