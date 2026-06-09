package io.repositree.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.repositree.audit.chain.HashChainService;
import io.repositree.audit.domain.AuditEvent;
import io.repositree.audit.pii.PiiRedactor;
import io.repositree.audit.sink.HumanAuditSink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditSinkServiceTest {

    @Mock
    private HumanAuditSink humanAuditSink;

    @Mock
    private HashChainService hashChainService;

    @Mock
    private PiiRedactor piiRedactor;

    private AuditSinkService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        service = new AuditSinkService(humanAuditSink, hashChainService, piiRedactor, objectMapper);
    }

    @Test
    void processAuditEvent_redactsAndHashChains() throws Exception {
        when(hashChainService.computeHash(any(), any(), any())).thenReturn("computed-hash");
        when(piiRedactor.redact(any())).thenReturn("{\"key\":\"[EMAIL]\"}");

        AuditEvent event = new AuditEvent(
                "evt-001", "USER_LOGIN", "tenant-1", "user-42", "HUMAN",
                Instant.now(), objectMapper.readTree("{\"email\":\"user@example.com\"}"),
                "corr-1", "auth-service", null, null
        );

        service.process(event);

        verify(piiRedactor).redact(any());
        verify(hashChainService).computeHash(any(), eq("evt-001"), any());
        verify(humanAuditSink).write(any());
    }

    @Test
    void processAuditEvent_sinkFailure_doesNotSilentlySwallow() {
        when(hashChainService.computeHash(any(), any(), any())).thenReturn("hash");
        when(piiRedactor.redact(any())).thenReturn("{}");
        doThrow(new RuntimeException("S3 write failed")).when(humanAuditSink).write(any());

        AuditEvent event = new AuditEvent(
                "evt-002", "PAYMENT", "tenant-1", "user-1", "HUMAN",
                Instant.now(), objectMapper.createObjectNode(),
                null, "payment-service", null, null
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.process(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("S3 write failed");
    }
}
