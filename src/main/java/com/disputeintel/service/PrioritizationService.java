package com.disputeintel.service;

import com.disputeintel.domain.Chargeback;
import com.disputeintel.repository.ChargebackRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Ranks open disputes by a Fight Score so the ops team knows where to spend
 * their limited response capacity.
 *
 *   fightScore = expectedRecovery * urgencyWeight
 *   expectedRecovery = winProbability(reasonCode) * amount
 *   winProbability   = learned from historical won/resolved per reason code
 *   urgencyWeight    = deadline pressure (sooner = higher), see weight()
 *
 * Only open disputes still within their deadline are scored; expired/closed
 * disputes cannot be represented and are excluded.
 */
@Service
public class PrioritizationService {

    private static final double PRIOR_WIN_PROB = 0.30; // fallback for sparse codes

    private final ChargebackRepository chargebacks;

    public PrioritizationService(ChargebackRepository chargebacks) {
        this.chargebacks = chargebacks;
    }

    public List<Map<String, Object>> prioritized(int limit) {
        Map<String, Double> winProb = learnWinProbabilities();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        List<Map<String, Object>> scored = new ArrayList<>();
        for (Chargeback c : chargebacks.findFightable(now)) {
            double prob = winProb.getOrDefault(c.getReasonCode(), PRIOR_WIN_PROB);
            double amount = c.getAmount().doubleValue();
            long daysLeft = Math.max(0, Duration.between(now, c.getDeadlineAt()).toDays());

            double expectedRecovery = prob * amount;
            double urgency = weight(daysLeft);
            double fightScore = expectedRecovery * urgency;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("disputeId", c.getId());
            row.put("reasonCode", c.getReasonCode());
            row.put("amount", c.getAmount());
            row.put("currency", c.getCurrency());
            row.put("productCategory", c.getProductCategory());
            row.put("daysUntilDeadline", daysLeft);
            row.put("winProbability", round(prob));
            row.put("expectedRecovery", round(expectedRecovery));
            row.put("urgencyWeight", urgency);
            row.put("fightScore", round(fightScore));
            scored.add(row);
        }

        scored.sort((a, b) -> Double.compare(
            (double) b.get("fightScore"), (double) a.get("fightScore")));

        return scored.size() > limit ? scored.subList(0, limit) : scored;
    }

    /** won / resolved per reason code, from stored history. */
    private Map<String, Double> learnWinProbabilities() {
        Map<String, Double> probs = new HashMap<>();
        for (Object[] row : chargebacks.winStatsByReasonCode()) {
            String code = (String) row[0];
            long won = ((Number) row[1]).longValue();
            long resolved = ((Number) row[2]).longValue();
            if (resolved > 0) probs.put(code, (double) won / resolved);
        }
        return probs;
    }

    /** Deadline pressure: closer deadline => higher weight. */
    private double weight(long daysLeft) {
        if (daysLeft <= 2) return 1.5;
        if (daysLeft <= 5) return 1.3;
        if (daysLeft <= 10) return 1.15;
        return 1.0;
    }

    private double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
