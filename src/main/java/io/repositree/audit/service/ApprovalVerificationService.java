package io.repositree.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.repositree.audit.chain.HashChainService;
import io.repositree.audit.domain.ApprovalRecord;
import io.repositree.audit.signature.Ed25519Verifier;
import org.springframework.stereotype.Service;

@Service
public class ApprovalVerificationService {

    private final Ed25519Verifier ed25519Verifier;
    private final HashChainService hashChain;
    private final ApprovalStore approvalStore;
    private final ObjectMapper objectMapper;

    public ApprovalVerificationService(Ed25519Verifier ed25519Verifier, HashChainService hashChain,
                                       ApprovalStore approvalStore, ObjectMapper objectMapper) {
        this.ed25519Verifier = ed25519Verifier;
        this.hashChain = hashChain;
        this.approvalStore = approvalStore;
        this.objectMapper = objectMapper;
    }

    public VerificationResult verify(String approvalId) {
        ApprovalRecord record = approvalStore.find(approvalId);
        if (record == null) {
            return new VerificationResult(false, null, null, "Approval not found: " + approvalId);
        }

        String chainStatus;
        boolean chainValid = hashChain.verify(
                record.prevHash(), record.approvalId(), record.signedPayload(), record.hash());
        chainStatus = chainValid ? "chain-valid" : "chain-invalid";

        if (!chainValid) {
            return new VerificationResult(false, chainStatus, null, "Hash chain mismatch");
        }

        boolean sigValid = ed25519Verifier.verify(record.signedPayload(), record.signature(), record.signerPublicKey());
        String sigStatus = sigValid ? "sig-valid" : "sig-invalid";

        if (!sigValid) {
            return new VerificationResult(false, chainStatus, sigStatus, "Signature verification failed");
        }

        return new VerificationResult(true, chainStatus, sigStatus, null);
    }

    public record VerificationResult(boolean valid, String chainStatus, String signatureStatus, String reason) {}
}
