package com.disputeintel.web.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Inbound payload for creating a dispute. */
public record ChargebackCreateRequest(
        String transactionId,                 // optional; generated if absent
        @NotBlank String merchantId,
        @NotBlank String productCategory,
        @NotBlank String customerCountry,
        @NotBlank @Email String customerEmail,
        @NotBlank String customerIp,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String reasonCode,
        OffsetDateTime openedAt,              // optional; defaults to now
        OffsetDateTime deadlineAt,            // optional; defaults to now + 14d
        String status                         // optional; defaults to "open"
) {}