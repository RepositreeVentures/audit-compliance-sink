package io.repositree.audit.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApprovalRecord(
        String approvalId,
        String runId,
        String approverId,
        String tenantId,
        String decision,
        String signedPayload,
        String signature,
        String signerPublicKey,
        Instant decidedAt,
        String prevHash,
        String hash
) {}
