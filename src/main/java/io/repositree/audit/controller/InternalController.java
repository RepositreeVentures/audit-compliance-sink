package io.repositree.audit.controller;

import io.repositree.audit.service.InternalLagService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/audit")
public class InternalController {

    private final InternalLagService lagService;

    public InternalController(InternalLagService lagService) {
        this.lagService = lagService;
    }

    @GetMapping("/lag")
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<Map<String, Long>> getLag() {
        return ResponseEntity.ok(lagService.getLag());
    }
}
