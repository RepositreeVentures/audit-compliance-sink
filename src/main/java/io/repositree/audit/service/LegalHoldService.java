package io.repositree.audit.service;

import io.repositree.audit.dto.LegalHoldRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Service
public class LegalHoldService {

    private static final Logger log = LoggerFactory.getLogger(LegalHoldService.class);

    private final S3Client s3;
    private final String humanAuditBucket;

    public LegalHoldService(S3Client s3,
                            @Value("${sink.human-audit.bucket}") String humanAuditBucket) {
        this.s3 = s3;
        this.humanAuditBucket = humanAuditBucket;
    }

    public void placeLegalHold(LegalHoldRequest request) {
        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(humanAuditBucket)
                .prefix(prefixForRange(request))
                .build();

        s3.listObjectsV2Paginator(listReq).forEach(page ->
            page.contents().forEach(obj -> {
                s3.putObjectLegalHold(PutObjectLegalHoldRequest.builder()
                        .bucket(humanAuditBucket)
                        .key(obj.key())
                        .legalHold(ObjectLockLegalHold.builder()
                                .status(ObjectLockLegalHoldStatus.ON).build())
                        .build());
                log.info("Legal hold placed on: {}", obj.key());
            })
        );
    }

    private String prefixForRange(LegalHoldRequest req) {
        if (req.dateFrom() != null) {
            return "date=" + req.dateFrom().toString().substring(0, 10);
        }
        return "";
    }
}
