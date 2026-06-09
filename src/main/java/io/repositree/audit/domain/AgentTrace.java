package io.repositree.audit.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentTrace(
        String runId,
        String agentId,
        String tenantId,
        String action,
        String modelId,
        String modelVersion,
        String promptVersion,
        Instant startedAt,
        Instant finishedAt,
        String status,
        List<LlmCall> llmCalls,
        List<ToolInvocation> toolInvocations,
        String approverId,
        String approvalId,
        String prevHash,
        String hash
) {
    public record LlmCall(
            String callId,
            Instant calledAt,
            String prompt,
            String redactedOutput,
            int promptTokens,
            int completionTokens,
            double costUsd
    ) {}

    public record ToolInvocation(
            String invocationId,
            Instant invokedAt,
            String toolName,
            JsonNode inputPayload,
            JsonNode outputPayload,
            String status
    ) {}
}
