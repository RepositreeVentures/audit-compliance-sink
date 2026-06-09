package io.repositree.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class AuditComplianceSinkApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditComplianceSinkApplication.class, args);
    }
}
