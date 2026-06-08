package com.disputeintel.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** A dispute filed against a transaction. */
@Entity
@Table(name = "chargebacks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Chargeback {
    @Id
    private String id;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "product_category", nullable = false)
    private String productCategory;

    @Column(name = "customer_country", nullable = false)
    private String customerCountry;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "customer_ip", nullable = false)
    private String customerIp;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(name = "reason_code", nullable = false)
    private String reasonCode;

    @Column(name = "reason_description", nullable = false)
    private String reasonDescription;

    @Column(name = "reason_category", nullable = false)
    private String reasonCategory;

    @Column(nullable = false)
    private String status;

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "deadline_at", nullable = false)
    private OffsetDateTime deadlineAt;

    @Column(nullable = false)
    private boolean responded;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;
}