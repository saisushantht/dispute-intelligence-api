package com.disputeintel.service;

import com.disputeintel.repository.ChargebackRepository;
import com.disputeintel.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Computes global metrics, segmented metrics, and anomaly detection.
 * Chargeback rate = disputes / total transactions (transactions are the
 * denominator), so rates are real ratios, computed per segment.
 */
@Service
public class MetricsService {

    // Dimensions that have a transaction-side denominator (=> true CB rate).
    private static final Set<String> RATE_DIMENSIONS =
        Set.of("productCategory", "customerCountry", "merchantId");

    private final ChargebackRepository chargebacks;
    private final TransactionRepository transactions;

    public MetricsService(ChargebackRepository chargebacks, TransactionRepository transactions) {
        this.chargebacks = chargebacks;
        this.transactions = transactions;
    }

    /** Overall headline metrics. */
    public Map<String, Object> global() {
        long totalTxns = transactions.count();
        long totalCb = chargebacks.count();
        long won = chargebacks.countWon();
        long resolved = chargebacks.countResolved();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalTransactions", totalTxns);
        m.put("totalChargebacks", totalCb);
        m.put("chargebackRatePct", pct(totalCb, totalTxns));
        m.put("winRatePct", pct(won, resolved));               // won / resolved
        m.put("responseRatePct", pct(resolved, totalCb));      // responded / total
        m.put("totalAmountInDispute", chargebacks.totalAmount());
        m.put("avgDisputeAmount", chargebacks.avgAmount().setScale(2, RoundingMode.HALF_UP));
        m.put("byStatus", Map.of(
            "open", chargebacks.countByStatus("open"),
            "won", won,
            "lost", chargebacks.countByStatus("lost"),
            "expired", chargebacks.countByStatus("expired")
        ));
        return m;
    }

    /** Metrics broken down by a dimension. CB rate included when a denominator exists. */
    public List<Map<String, Object>> segmented(String dimension) {
        Map<String, Long> txnCounts = new HashMap<>();
        boolean hasRate = RATE_DIMENSIONS.contains(dimension);
        if (hasRate) {
            for (Object[] row : transactions.countsByDimension(dimension)) {
                txnCounts.put((String) row[0], (Long) row[1]);
            }
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] row : chargebacks.groupedMetrics(dimension)) {
            String segment = (String) row[0];
            long disputes = (Long) row[1];
            long won = (Long) row[2];
            long resolved = (Long) row[3];
            BigDecimal amount = (BigDecimal) row[4];

            Map<String, Object> seg = new LinkedHashMap<>();
            seg.put("segment", segment);
            seg.put("disputes", disputes);
            seg.put("winRatePct", pct(won, resolved));
            seg.put("totalAmount", amount);
            if (hasRate) {
                long txns = txnCounts.getOrDefault(segment, 0L);
                seg.put("transactions", txns);
                seg.put("chargebackRatePct", pct(disputes, txns));
            }
            out.add(seg);
        }
        return out;
    }

    /**
     * Anomaly detection: segments whose chargeback rate is much higher than the
     * overall rate. Flags any segment >= 2x overall (configurable threshold).
     */
    public Map<String, Object> anomalies(String dimension, double thresholdMultiple) {
        if (!RATE_DIMENSIONS.contains(dimension)) {
            return Map.of("error",
                "Anomaly detection needs a rate dimension: productCategory, customerCountry, or merchantId");
        }
        double overall = pct(chargebacks.count(), transactions.count());

        List<Map<String, Object>> flagged = new ArrayList<>();
        for (Map<String, Object> seg : segmented(dimension)) {
            double rate = ((Number) seg.get("chargebackRatePct")).doubleValue();
            double multiple = overall == 0 ? 0 : rate / overall;
            if (multiple >= thresholdMultiple) {
                Map<String, Object> a = new LinkedHashMap<>(seg);
                a.put("overallRatePct", round(overall));
                a.put("timesAboveAverage", round(multiple));
                flagged.add(a);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dimension", dimension);
        result.put("overallChargebackRatePct", round(overall));
        result.put("thresholdMultiple", thresholdMultiple);
        result.put("anomalies", flagged);
        return result;
    }

    private double pct(long numerator, long denominator) {
        if (denominator == 0) return 0.0;
        return round(100.0 * numerator / denominator);
    }

    private double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
