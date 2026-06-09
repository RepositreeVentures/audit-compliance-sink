package io.repositree.audit.sink;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EnrichedAgentTrace(
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
        List<RedactedLlmCall> llmCalls,
        List<RedactedToolInvocation> toolInvocations,
        String approverId,
        String approvalId,
        String prevHash,
        String hash,
        Instant ingestedAt
) {
    public record RedactedLlmCall(
            String callId,
            Instant calledAt,
            String redactedPrompt,
            String redactedOutput,
            int promptTokens,
            int completionTokens,
            double costUsd
    ) {}

    public record RedactedToolInvocation(
            String invocationId,
            Instant invokedAt,
            String toolName,
            String redactedInput,
            String redactedOutput,
            String status
    ) {}
}
