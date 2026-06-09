package io.repositree.audit.controller;

import io.repositree.audit.service.ApprovalVerificationService;
import io.repositree.audit.service.InternalLagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuditController.class, InternalController.class})
class AuditVerifyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApprovalVerificationService verificationService;

    @MockBean
    private InternalLagService lagService;

    @MockBean
    private io.repositree.audit.service.AuditQueryService auditQueryService;

    @MockBean
    private io.repositree.audit.service.AgentTraceService agentTraceService;

    @MockBean
    private io.repositree.audit.service.LegalHoldService legalHoldService;

    @MockBean
    private io.repositree.audit.service.DataExportService dataExportService;

    @Test
    @WithMockUser(roles = "COMPLIANCE_OFFICER")
    void verifyApproval_valid_returns200() throws Exception {
        when(verificationService.verify("apr-123"))
                .thenReturn(new ApprovalVerificationService.VerificationResult(true, "chain-valid", "sig-valid", null));

        mockMvc.perform(get("/v1/audit/approvals/apr-123/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @WithMockUser(roles = "COMPLIANCE_OFFICER")
    void verifyApproval_invalid_returns200WithFalse() throws Exception {
        when(verificationService.verify("apr-bad"))
                .thenReturn(new ApprovalVerificationService.VerificationResult(false, "chain-invalid", null, "Hash mismatch"));

        mockMvc.perform(get("/v1/audit/approvals/apr-bad/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.reason").value("Hash mismatch"));
    }

    @Test
    void verifyApproval_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/v1/audit/approvals/apr-123/verify"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "INTERNAL")
    void lagEndpoint_returnsLagPerStream() throws Exception {
        when(lagService.getLag()).thenReturn(Map.of(
                "audit.log", 0L,
                "agent.runs", 12L
        ));

        mockMvc.perform(get("/internal/audit/lag"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['audit.log']").value(0))
                .andExpect(jsonPath("$['agent.runs']").value(12));
    }
}
