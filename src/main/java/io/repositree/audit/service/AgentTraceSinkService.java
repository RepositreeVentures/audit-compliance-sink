package io.repositree.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.repositree.audit.chain.HashChainService;
import io.repositree.audit.domain.AgentTrace;
import io.repositree.audit.pii.PiiRedactor;
import io.repositree.audit.sink.AgentTraceSink;
import io.repositree.audit.sink.EnrichedAgentTrace;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AgentTraceSinkService {

    private final AgentTraceSink sink;
    private final HashChainService hashChain;
    private final PiiRedactor piiRedactor;
    private final ObjectMapper objectMapper;
    private final AtomicReference<String> lastHash = new AtomicReference<>(HashChainService.GENESIS_HASH);

    public AgentTraceSinkService(AgentTraceSink sink, HashChainService hashChain,
                                 PiiRedactor piiRedactor, ObjectMapper objectMapper) {
        this.sink = sink;
        this.hashChain = hashChain;
        this.piiRedactor = piiRedactor;
        this.objectMapper = objectMapper;
    }

    public synchronized void process(AgentTrace trace) {
        try {
            List<EnrichedAgentTrace.RedactedLlmCall> redactedCalls = trace.llmCalls() == null ? List.of() :
                    trace.llmCalls().stream().map(c -> new EnrichedAgentTrace.RedactedLlmCall(
                            c.callId(), c.calledAt(),
                            piiRedactor.redact(c.prompt()),
                            piiRedactor.redact(c.redactedOutput()),
                            c.promptTokens(), c.completionTokens(), c.costUsd()
                    )).toList();

            List<EnrichedAgentTrace.RedactedToolInvocation> redactedTools = trace.toolInvocations() == null ? List.of() :
                    trace.toolInvocations().stream().map(t -> new EnrichedAgentTrace.RedactedToolInvocation(
                            t.invocationId(), t.invokedAt(), t.toolName(),
                            piiRedactor.redact(objectMapper.valueToTree(t.inputPayload()).toString()),
                            piiRedactor.redact(objectMapper.valueToTree(t.outputPayload()).toString()),
                            t.status()
                    )).toList();

            String traceJson = objectMapper.writeValueAsString(trace);
            String prevHash = lastHash.get();
            String hash = hashChain.computeHash(prevHash, trace.runId(), traceJson);
            lastHash.set(hash);

            EnrichedAgentTrace enriched = new EnrichedAgentTrace(
                    trace.runId(), trace.agentId(), trace.tenantId(),
                    trace.action(), trace.modelId(), trace.modelVersion(), trace.promptVersion(),
                    trace.startedAt(), trace.finishedAt(), trace.status(),
                    redactedCalls, redactedTools,
                    trace.approverId(), trace.approvalId(),
                    prevHash, hash, Instant.now()
            );

            sink.write(enriched);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
