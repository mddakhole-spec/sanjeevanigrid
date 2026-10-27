package com.sanjeevanigrid.scheduler.api;

import com.sanjeevanigrid.scheduler.domain.Appointment;
import com.sanjeevanigrid.scheduler.facade.SchedulerFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/queue")
@Tag(name = "Live OPD Queue Screen", description = "Endpoints for OPD waiting room screens, live tokens, and queue progression")
public class QueueController {

    private final SchedulerFacade schedulerFacade;

    public QueueController(SchedulerFacade schedulerFacade) {
        this.schedulerFacade = schedulerFacade;
    }

    @GetMapping("/{doctorId}")
    @Operation(summary = "Get live OPD queue screen state: Now Serving, Next in Line, and wait time predictions")
    public ResponseEntity<Map<String, Object>> getLiveQueue(
            @PathVariable UUID doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(schedulerFacade.getLiveQueue(doctorId, queryDate));
    }

    @PostMapping("/appointments/{id}/action")
    @Operation(summary = "Advance queue: CHECK_IN, CALL_NEXT, COMPLETE (with duration for rolling avg), NO_SHOW (with waitlist auto-release), CANCEL")
    public ResponseEntity<Appointment> advanceQueue(
            @PathVariable UUID id,
            @RequestParam String action,
            @RequestParam(required = false) Double duration) {
        return ResponseEntity.ok(schedulerFacade.advanceQueue(id, action, duration));
    }
}
