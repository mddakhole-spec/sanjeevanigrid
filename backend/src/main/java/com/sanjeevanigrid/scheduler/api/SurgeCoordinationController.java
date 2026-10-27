package com.sanjeevanigrid.scheduler.api;

import com.sanjeevanigrid.notification.infra.SmsSandboxAdapter;
import com.sanjeevanigrid.scheduler.domain.SurgeEventListener;
import com.sanjeevanigrid.scheduler.facade.SchedulerFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduler")
@Tag(name = "Surge Coordination & Sandbox Controls", description = "Endpoints for surge protocol triggers and SMS sandbox inspection")
public class SurgeCoordinationController {

    private final SchedulerFacade schedulerFacade;
    private final SurgeEventListener surgeEventListener;
    private final SmsSandboxAdapter smsSandboxAdapter;

    public SurgeCoordinationController(SchedulerFacade schedulerFacade,
                                       SurgeEventListener surgeEventListener,
                                       SmsSandboxAdapter smsSandboxAdapter) {
        this.schedulerFacade = schedulerFacade;
        this.surgeEventListener = surgeEventListener;
        this.smsSandboxAdapter = smsSandboxAdapter;
    }

    @PostMapping("/surge/trigger")
    @Operation(summary = "Trigger surge protocol slot reservation manually or test simulator event reaction")
    public ResponseEntity<Map<String, Object>> triggerSurge(
            @RequestParam(defaultValue = "Emergency & General Medicine") String wardName,
            @RequestParam(defaultValue = "9") int hoursUntilDepletion) {
        int reservedCount = schedulerFacade.triggerSurgeProtocol(wardName, hoursUntilDepletion);
        return ResponseEntity.ok(Map.of(
                "status", "SURGE_ACTIVE",
                "wardName", wardName,
                "hoursUntilDepletion", hoursUntilDepletion,
                "reservedSlotsCount", reservedCount,
                "message", "Surge protocol activated: emergency buffer slots reserved for crisis intake."
        ));
    }

    @PostMapping("/surge/deactivate")
    @Operation(summary = "Deactivate surge protocol and release reserved slots")
    public ResponseEntity<Map<String, Object>> deactivateSurge() {
        int releasedCount = schedulerFacade.deactivateSurgeProtocol();
        return ResponseEntity.ok(Map.of(
                "status", "NORMAL",
                "releasedSlotsCount", releasedCount,
                "message", "Surge protocol deactivated: buffer slots restored to normal availability."
        ));
    }

    @GetMapping("/surge/status")
    @Operation(summary = "Get current surge protocol status")
    public ResponseEntity<Map<String, Object>> getSurgeStatus() {
        return ResponseEntity.ok(Map.of(
                "isSurgeActive", surgeEventListener.isSurgeActive(),
                "lastSurgeEvent", surgeEventListener.getLastSurgeEvent() != null ? surgeEventListener.getLastSurgeEvent() : "None"
        ));
    }

    @GetMapping("/sandbox-inbox")
    @Operation(summary = "Get all SMS messages sent to the sandbox inbox (shows bumped notifications, confirmations, reminders)")
    public ResponseEntity<List<SmsSandboxAdapter.SmsMessage>> getSandboxInbox() {
        return ResponseEntity.ok(smsSandboxAdapter.getInbox());
    }

    @DeleteMapping("/sandbox-inbox")
    @Operation(summary = "Clear SMS sandbox inbox")
    public ResponseEntity<Map<String, String>> clearSandboxInbox() {
        smsSandboxAdapter.clearInbox();
        return ResponseEntity.ok(Map.of("message", "Sandbox inbox cleared"));
    }
}
