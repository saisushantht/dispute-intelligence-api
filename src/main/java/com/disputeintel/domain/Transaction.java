package com.disputeintel.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** A base transaction. Stored separately so chargeback RATE has a denominator. */
@Entity
@Table(name = "transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {
    @Id
    private String id;

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

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}