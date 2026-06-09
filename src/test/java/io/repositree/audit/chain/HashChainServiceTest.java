package io.repositree.audit.chain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HashChainServiceTest {

    private final HashChainService chainService = new HashChainService();

    @Test
    void computesHashForFirstRecord() {
        String hash = chainService.computeHash(HashChainService.GENESIS_HASH, "event-1", "{\"key\":\"value\"}");
        assertThat(hash).isNotBlank();
        assertThat(hash).hasSize(64); // SHA-256 hex = 64 chars
    }

    @Test
    void genesisHashIsAllZeros() {
        assertThat(HashChainService.GENESIS_HASH).isEqualTo("0".repeat(64));
    }

    @Test
    void sameInputProducesSameHash() {
        String hash1 = chainService.computeHash("prevHash", "event-1", "payload");
        String hash2 = chainService.computeHash("prevHash", "event-1", "payload");
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void differentPrevHashProducesDifferentHash() {
        String hash1 = chainService.computeHash("prev1", "event-1", "payload");
        String hash2 = chainService.computeHash("prev2", "event-1", "payload");
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void verifyChain_validChain() {
        String h0 = HashChainService.GENESIS_HASH;
        String h1 = chainService.computeHash(h0, "event-1", "payload1");
        String h2 = chainService.computeHash(h1, "event-2", "payload2");

        assertThat(chainService.verify(h0, "event-1", "payload1", h1)).isTrue();
        assertThat(chainService.verify(h1, "event-2", "payload2", h2)).isTrue();
    }

    @Test
    void verifyChain_tamperedPayload_fails() {
        String h0 = HashChainService.GENESIS_HASH;
        String h1 = chainService.computeHash(h0, "event-1", "payload1");

        assertThat(chainService.verify(h0, "event-1", "TAMPERED", h1)).isFalse();
    }

    @Test
    void computeHash_nullPrevHash_throws() {
        assertThatThrownBy(() -> chainService.computeHash(null, "event-1", "payload"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
