package io.repositree.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.repositree.audit.chain.HashChainService;
import io.repositree.audit.domain.AuditEvent;
import io.repositree.audit.pii.PiiRedactor;
import io.repositree.audit.sink.EnrichedAuditRecord;
import io.repositree.audit.sink.HumanAuditSink;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AuditSinkService {

    private final HumanAuditSink sink;
    private final HashChainService hashChain;
    private final PiiRedactor piiRedactor;
    private final ObjectMapper objectMapper;
    private final AtomicReference<String> lastHash = new AtomicReference<>(HashChainService.GENESIS_HASH);

    public AuditSinkService(HumanAuditSink sink, HashChainService hashChain,
                            PiiRedactor piiRedactor, ObjectMapper objectMapper) {
        this.sink = sink;
        this.hashChain = hashChain;
        this.piiRedactor = piiRedactor;
        this.objectMapper = objectMapper;
    }

    public synchronized void process(AuditEvent event) {
        try {
            String payloadJson = objectMapper.writeValueAsString(event.payload());
            String redactedPayload = piiRedactor.redact(payloadJson);
            String prevHash = lastHash.get();
            String hash = hashChain.computeHash(prevHash, event.eventId(), redactedPayload);
            lastHash.set(hash);

            EnrichedAuditRecord record = new EnrichedAuditRecord(
                    event.eventId(),
                    event.eventType(),
                    event.tenantId(),
                    event.actorId(),
                    event.actorType(),
                    event.occurredAt(),
                    redactedPayload,
                    event.correlationId(),
                    event.sourceService(),
                    prevHash,
                    hash,
                    Instant.now()
            );

            sink.write(record);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
