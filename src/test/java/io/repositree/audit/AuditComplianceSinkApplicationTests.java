package io.repositree.audit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9999",
        "spring.kafka.consumer.group-id=test",
        "sink.human-audit.bucket=test-bucket",
        "sink.agent-trace.bucket=test-trace-bucket",
        "security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/.well-known/jwks.json",
        "aws.endpoint-override=http://localhost:4566"
})
class AuditComplianceSinkApplicationTests {

    @Test
    void contextLoads() {
    }
}
