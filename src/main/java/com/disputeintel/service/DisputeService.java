package com.disputeintel.service;

import com.disputeintel.domain.Chargeback;
import com.disputeintel.domain.Transaction;
import com.disputeintel.reference.ReasonCatalog;
import com.disputeintel.repository.ChargebackRepository;
import com.disputeintel.repository.TransactionRepository;
import com.disputeintel.web.dto.ChargebackCreateRequest;
import com.disputeintel.web.dto.StatusUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Ingestion + storage logic. Every chargeback is backed by a transaction row
 * (the denominator for chargeback-rate metrics), so create() upserts both.
 */
@Service
public class DisputeService {

    private final ChargebackRepository chargebacks;
    private final TransactionRepository transactions;

    public DisputeService(ChargebackRepository chargebacks, TransactionRepository transactions) {
        this.chargebacks = chargebacks;
        this.transactions = transactions;
    }

    @Transactional
    public Chargeback create(ChargebackCreateRequest req) {
        OffsetDateTime now = OffsetDateTime.now();
        String txnId = (req.transactionId() == null || req.transactionId().isBlank())
                ? "txn_" + UUID.randomUUID().toString().substring(0, 12)
                : req.transactionId();

        // Upsert the backing transaction (idempotent on id).
        Transaction txn = transactions.findById(txnId).orElseGet(Transaction::new);
        txn.setId(txnId);
        txn.setMerchantId(req.merchantId());
        txn.setProductCategory(req.productCategory());
        txn.setCustomerCountry(req.customerCountry());
        txn.setCustomerEmail(req.customerEmail());
        txn.setCustomerIp(req.customerIp());
        txn.setAmount(req.amount());
        txn.setCurrency(req.currency());
        txn.setCreatedAt(req.openedAt() != null ? req.openedAt() : now);
        transactions.save(txn);

        ReasonCatalog.Reason reason = ReasonCatalog.lookup(req.reasonCode());

        Chargeback cb = new Chargeback();
        cb.setId("cb_" + UUID.randomUUID().toString().substring(0, 12));
        cb.setTransactionId(txnId);
        cb.setMerchantId(req.merchantId());
        cb.setProductCategory(req.productCategory());
        cb.setCustomerCountry(req.customerCountry());
        cb.setCustomerEmail(req.customerEmail());
        cb.setCustomerIp(req.customerIp());
        cb.setAmount(req.amount());
        cb.setCurrency(req.currency());
        cb.setReasonCode(req.reasonCode());
        cb.setReasonDescription(reason.description());
        cb.setReasonCategory(reason.category());
        cb.setStatus(req.status() != null && !req.status().isBlank() ? req.status() : "open");
        cb.setOpenedAt(req.openedAt() != null ? req.openedAt() : now);
        cb.setDeadlineAt(req.deadlineAt() != null ? req.deadlineAt() : now.plusDays(14));
        cb.setResponded(false);
        cb.setResolvedAt(null);
        return chargebacks.save(cb);
    }

    @Transactional
    public List<Chargeback> createBulk(List<ChargebackCreateRequest> reqs) {
        return reqs.stream().map(this::create).toList();
    }

    public List<Chargeback> search(String status, String reasonCode,
                                   String productCategory, String customerCountry,
                                   String merchantId) {
        return chargebacks.search(status, reasonCode, productCategory, customerCountry, merchantId);
    }

    public Chargeback get(String id) {
        return chargebacks.findById(id).orElse(null);
    }

    @Transactional
    public Chargeback updateStatus(String id, StatusUpdateRequest req) {
        Chargeback cb = chargebacks.findById(id).orElse(null);
        if (cb == null) return null;
        cb.setStatus(req.status());
        if (req.responded() != null) cb.setResponded(req.responded());
        if (req.resolvedAt() != null) {
            cb.setResolvedAt(req.resolvedAt());
        } else if (("won".equals(req.status()) || "lost".equals(req.status()))
                && cb.getResolvedAt() == null) {
            cb.setResolvedAt(OffsetDateTime.now());
        }
        return chargebacks.save(cb);
    }
}