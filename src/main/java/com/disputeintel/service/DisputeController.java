package com.disputeintel.web;

import com.disputeintel.domain.Chargeback;
import com.disputeintel.service.DisputeService;
import com.disputeintel.web.dto.ChargebackCreateRequest;
import com.disputeintel.web.dto.StatusUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/disputes")
@Tag(name = "Disputes", description = "Ingest, query, and update chargeback disputes")
public class DisputeController {

    private final DisputeService service;

    public DisputeController(DisputeService service) {
        this.service = service;
    }

    @Operation(summary = "Create a single dispute")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Chargeback create(@Valid @RequestBody ChargebackCreateRequest req) {
        return service.create(req);
    }

    @Operation(summary = "Bulk-create disputes")
    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Chargeback> createBulk(@RequestBody List<ChargebackCreateRequest> reqs) {
        return service.createBulk(reqs);
    }

    @Operation(summary = "List disputes with optional filters")
    @GetMapping
    public List<Chargeback> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String reasonCode,
            @RequestParam(required = false) String productCategory,
            @RequestParam(required = false) String customerCountry,
            @RequestParam(required = false) String merchantId) {
        return service.search(status, reasonCode, productCategory, customerCountry, merchantId);
    }

    @Operation(summary = "Get a single dispute by id")
    @GetMapping("/{id}")
    public Chargeback get(@PathVariable String id) {
        Chargeback cb = service.get(id);
        if (cb == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispute not found: " + id);
        return cb;
    }

    @Operation(summary = "Update a dispute's status/outcome")
    @PatchMapping("/{id}/status")
    public Chargeback updateStatus(@PathVariable String id, @Valid @RequestBody StatusUpdateRequest req) {
        Chargeback cb = service.updateStatus(id, req);
        if (cb == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispute not found: " + id);
        return cb;
    }
}