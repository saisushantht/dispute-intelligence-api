package com.disputeintel.web.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

/** Inbound payload for updating a dispute's status/outcome. */
public record StatusUpdateRequest(
        @NotBlank String status,        // open | won | lost | expired
        Boolean responded,              // optional
        OffsetDateTime resolvedAt       // optional
) {}