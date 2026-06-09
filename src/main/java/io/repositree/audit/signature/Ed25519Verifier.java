package io.repositree.audit.signature;

import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class Ed25519Verifier {

    public boolean verify(String payload, String signatureBase64, String publicKeyBase64) {
        try {
            byte[] sigBytes = Base64.getDecoder().decode(signatureBase64);
            byte[] pubKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            byte[] payloadBytes = payload.getBytes();

            Ed25519PublicKeyParameters publicKey = new Ed25519PublicKeyParameters(pubKeyBytes, 0);
            Ed25519Signer verifier = new Ed25519Signer();
            verifier.init(false, publicKey);
            verifier.update(payloadBytes, 0, payloadBytes.length);
            return verifier.verifySignature(sigBytes);
        } catch (Exception e) {
            return false;
        }
    }
}
