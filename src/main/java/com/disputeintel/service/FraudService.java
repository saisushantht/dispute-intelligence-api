package com.disputeintel.service;

import com.disputeintel.repository.ChargebackRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Detects potential fraud rings: clusters of disputes sharing the same
 * customer email + IP address, which often indicates coordinated abuse
 * (e.g. friendly-fraud refund schemes).
 */
@Service
public class FraudService {

    private final ChargebackRepository chargebacks;

    public FraudService(ChargebackRepository chargebacks) {
        this.chargebacks = chargebacks;
    }

    public List<Map<String, Object>> detectRings(long minCount) {
        List<Map<String, Object>> rings = new ArrayList<>();
        for (Object[] row : chargebacks.findFraudClusters(minCount)) {
            Map<String, Object> ring = new LinkedHashMap<>();
            ring.put("customerEmail", row[0]);
            ring.put("customerIp", row[1]);
            ring.put("disputeCount", row[2]);
            ring.put("totalAmount", (BigDecimal) row[3]);
            ring.put("flag", "Multiple disputes from identical email + IP — possible coordinated fraud");
            rings.add(ring);
        }
        return rings;
    }
}
