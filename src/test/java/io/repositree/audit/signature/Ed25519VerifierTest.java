package io.repositree.audit.signature;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class Ed25519VerifierTest {

    private Ed25519Verifier verifier;
    private Ed25519PrivateKeyParameters privateKey;
    private Ed25519PublicKeyParameters publicKey;

    @BeforeEach
    void setUp() {
        verifier = new Ed25519Verifier();
        SecureRandom random = new SecureRandom();
        privateKey = new Ed25519PrivateKeyParameters(random);
        publicKey = privateKey.generatePublicKey();
    }

    private String sign(String payload) throws Exception {
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, privateKey);
        byte[] payloadBytes = payload.getBytes();
        signer.update(payloadBytes, 0, payloadBytes.length);
        byte[] sig = signer.generateSignature();
        return Base64.getEncoder().encodeToString(sig);
    }

    private String publicKeyBase64() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    @Test
    void verifiesValidSignature() throws Exception {
        String payload = "{\"approvalId\":\"apr-1\",\"decision\":\"APPROVED\"}";
        String signature = sign(payload);
        assertThat(verifier.verify(payload, signature, publicKeyBase64())).isTrue();
    }

    @Test
    void rejectsTamperedPayload() throws Exception {
        String payload = "{\"approvalId\":\"apr-1\",\"decision\":\"APPROVED\"}";
        String signature = sign(payload);
        String tampered = "{\"approvalId\":\"apr-1\",\"decision\":\"REJECTED\"}";
        assertThat(verifier.verify(tampered, signature, publicKeyBase64())).isFalse();
    }

    @Test
    void rejectsInvalidBase64Signature() {
        assertThat(verifier.verify("payload", "not-valid-base64!!!", publicKeyBase64())).isFalse();
    }

    @Test
    void rejectsWrongPublicKey() throws Exception {
        String payload = "approval-payload";
        String signature = sign(payload);

        SecureRandom random = new SecureRandom();
        Ed25519PrivateKeyParameters otherPrivKey = new Ed25519PrivateKeyParameters(random);
        String otherPublicKey = Base64.getEncoder().encodeToString(
                otherPrivKey.generatePublicKey().getEncoded()
        );
        assertThat(verifier.verify(payload, signature, otherPublicKey)).isFalse();
    }
}
