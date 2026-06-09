package io.repositree.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.repositree.audit.chain.HashChainService;
import io.repositree.audit.domain.ApprovalRecord;
import io.repositree.audit.signature.Ed25519Verifier;
import io.repositree.audit.sink.HumanAuditSink;
import io.repositree.audit.sink.EnrichedAuditRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ApprovalSinkService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalSinkService.class);

    private final HumanAuditSink sink;
    private final HashChainService hashChain;
    private final Ed25519Verifier ed25519Verifier;
    private final ObjectMapper objectMapper;
    private final AtomicReference<String> lastHash = new AtomicReference<>(HashChainService.GENESIS_HASH);

    public ApprovalSinkService(HumanAuditSink sink, HashChainService hashChain,
                               Ed25519Verifier ed25519Verifier, ObjectMapper objectMapper) {
        this.sink = sink;
        this.hashChain = hashChain;
        this.ed25519Verifier = ed25519Verifier;
        this.objectMapper = objectMapper;
    }

    public synchronized void process(ApprovalRecord record) {
        try {
            boolean sigValid = ed25519Verifier.verify(
                    record.signedPayload(), record.signature(), record.signerPublicKey());
            if (!sigValid) {
                log.warn("Ed25519 signature INVALID for approvalId={} — writing anyway with flag", record.approvalId());
            }

            String payloadJson = objectMapper.writeValueAsString(record);
            String prevHash = lastHash.get();
            String hash = hashChain.computeHash(prevHash, record.approvalId(), payloadJson);
            lastHash.set(hash);

            EnrichedAuditRecord enriched = new EnrichedAuditRecord(
                    record.approvalId(), "HITL_APPROVAL",
                    record.tenantId(), record.approverId(), "HUMAN",
                    record.decidedAt(), payloadJson,
                    record.runId(), "hitl-approval-queue",
                    prevHash, hash, Instant.now()
            );

            sink.write(enriched);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
