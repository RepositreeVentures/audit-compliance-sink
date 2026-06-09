package io.repositree.audit.controller;

import io.repositree.audit.dto.*;
import io.repositree.audit.exception.NotFoundException;
import io.repositree.audit.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/audit")
public class AuditController {

    private final AuditQueryService auditQueryService;
    private final LegalHoldService legalHoldService;
    private final DataExportService dataExportService;
    private final AgentTraceService agentTraceService;
    private final ApprovalVerificationService approvalVerificationService;

    public AuditController(AuditQueryService auditQueryService,
                           LegalHoldService legalHoldService,
                           DataExportService dataExportService,
                           AgentTraceService agentTraceService,
                           ApprovalVerificationService approvalVerificationService) {
        this.auditQueryService = auditQueryService;
        this.legalHoldService = legalHoldService;
        this.dataExportService = dataExportService;
        this.agentTraceService = agentTraceService;
        this.approvalVerificationService = approvalVerificationService;
    }

    @PostMapping("/query")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<AuditQueryResponse> query(@Valid @RequestBody AuditQueryRequest request) {
        return ResponseEntity.ok(auditQueryService.query(request));
    }

    @PostMapping("/legal-hold")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<Void> placeLegalHold(@Valid @RequestBody LegalHoldRequest request) {
        legalHoldService.placeLegalHold(request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<DataExportResponse> export(
            @RequestParam String subjectId,
            @RequestParam String tenantId) {
        return ResponseEntity.ok(dataExportService.exportForSubject(subjectId, tenantId));
    }

    @GetMapping("/agent-runs/{runId}")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<Map<String, Object>> getAgentRun(@PathVariable String runId) {
        return ResponseEntity.ok(agentTraceService.getRunTrace(runId));
    }

    @GetMapping("/agent-runs")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<?> searchAgentRuns(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "100") int limit) {
        var req = new AgentRunSearchRequest(tenantId, agentId, action, from, to, limit);
        return ResponseEntity.ok(agentTraceService.searchRuns(req));
    }

    @GetMapping("/approvals/{approvalId}/verify")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<ApprovalVerificationService.VerificationResult> verifyApproval(
            @PathVariable String approvalId) {
        return ResponseEntity.ok(approvalVerificationService.verify(approvalId));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }
}
