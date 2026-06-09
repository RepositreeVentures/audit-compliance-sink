package io.repositree.audit.chain;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class HashChainService {

    public static final String GENESIS_HASH = "0".repeat(64);

    public String computeHash(String prevHash, String eventId, String payloadJson) {
        if (prevHash == null) throw new IllegalArgumentException("prevHash must not be null");
        if (eventId == null) throw new IllegalArgumentException("eventId must not be null");
        String input = prevHash + "|" + eventId + "|" + (payloadJson != null ? payloadJson : "");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public boolean verify(String prevHash, String eventId, String payloadJson, String expectedHash) {
        String computed = computeHash(prevHash, eventId, payloadJson);
        return computed.equals(expectedHash);
    }
}
