package com.disputeintel.service;

import com.disputeintel.domain.Chargeback;
import com.disputeintel.domain.Transaction;
import com.disputeintel.reference.GeneratorConfig;
import com.disputeintel.reference.ReasonCatalog;
import com.disputeintel.repository.ChargebackRepository;
import com.disputeintel.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Seeds the database with realistic synthetic data on startup, ONLY if empty.
 * Data is generated (seeded RNG for reproducibility), never hard-coded rows.
 * Guarantees: 300+ disputes, all 4 statuses, 10 reason codes, 3 currencies,
 * Handwoven Textiles ~3.5x dispute rate, a <20% and a >70% win-rate code,
 * several expired disputes, and one injected fraud ring (shared email + IP).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final Random RNG = new Random(42); // reproducible demo

    private final TransactionRepository transactions;
    private final ChargebackRepository chargebacks;

    public DataSeeder(TransactionRepository transactions, ChargebackRepository chargebacks) {
        this.transactions = transactions;
        this.chargebacks = chargebacks;
    }

    @Override
    public void run(String... args) {
        if (chargebacks.count() > 0) {
            log.info("Data already present ({} disputes) - skipping seed.", chargebacks.count());
            return;
        }
        log.info("Empty DB detected - generating synthetic dataset...");

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<Transaction> txns = new ArrayList<>();
        List<Chargeback> cbs = new ArrayList<>();

        // Reusable customer pool so the same buyer can recur (and rings can form).
        Map<String, List<String[]>> pool = new HashMap<>();
        for (String c : GeneratorConfig.COUNTRIES) {
            List<String[]> people = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                people.add(new String[]{
                    "buyer" + c + i + "@example.com",
                    "198.51." + RNG.nextInt(255) + "." + RNG.nextInt(255)
                });
            }
            pool.put(c, people);
        }

        for (int i = 0; i < GeneratorConfig.TARGET_TRANSACTIONS; i++) {
            String category = pick(GeneratorConfig.CATEGORIES);
            String country = pick(GeneratorConfig.COUNTRIES);
            String[] person = pick(pool.get(country));
            OffsetDateTime created = now.minusDays(RNG.nextInt(GeneratorConfig.WINDOW_DAYS))
                                        .minusHours(RNG.nextInt(24));

            Transaction t = new Transaction();
            t.setId("txn_" + UUID.randomUUID().toString().substring(0, 12));
            t.setMerchantId(pick(GeneratorConfig.MERCHANTS));
            t.setProductCategory(category);
            t.setCustomerCountry(country);
            t.setCustomerEmail(person[0]);
            t.setCustomerIp(person[1]);
            t.setAmount(sampleAmount());
            t.setCurrency(weightedPick(GeneratorConfig.CURRENCIES, GeneratorConfig.CURRENCY_WEIGHTS));
            t.setCreatedAt(created);
            txns.add(t);

            double pCb = GeneratorConfig.BASE_CB_RATE
                       * GeneratorConfig.CATEGORY_CB_MULTIPLIER.get(category);
            if (RNG.nextDouble() >= pCb) continue;

            cbs.add(buildChargeback(t, weightedReason(), created, now, false));
        }

        injectFraudRing(txns, cbs, now);

        transactions.saveAll(txns);
        chargebacks.saveAll(cbs);

        Map<String, Long> byStatus = new TreeMap<>();
        for (Chargeback cb : cbs) byStatus.merge(cb.getStatus(), 1L, Long::sum);
        log.info("Seed complete: {} transactions, {} disputes, by status = {}",
                 txns.size(), cbs.size(), byStatus);
    }

    private Chargeback buildChargeback(Transaction t, String code,
                                       OffsetDateTime txTime, OffsetDateTime now,
                                       boolean forceOpen) {
        OffsetDateTime opened = txTime.plusDays(2 + RNG.nextInt(24));
        if (opened.isAfter(now)) opened = now.minusDays(RNG.nextInt(6));
        OffsetDateTime deadline = opened.plusDays(7 + RNG.nextInt(15));

        ReasonCatalog.Reason reason = ReasonCatalog.lookup(code);
        String status; boolean responded; OffsetDateTime resolved;

        boolean pastDeadline = deadline.isBefore(now) && !forceOpen;
        if (pastDeadline) {
            if (RNG.nextDouble() < 0.30) {           // missed the window
                status = "expired"; responded = false; resolved = deadline;
            } else {                                  // fought it; win per true prob
                boolean won = RNG.nextDouble() < GeneratorConfig.REASON_WIN_PROB.get(code);
                status = won ? "won" : "lost";
                responded = true;
                resolved = deadline.minusDays(RNG.nextInt(4));
            }
        } else {
            status = "open"; responded = false; resolved = null;
        }

        Chargeback cb = new Chargeback();
        cb.setId("cb_" + UUID.randomUUID().toString().substring(0, 12));
        cb.setTransactionId(t.getId());
        cb.setMerchantId(t.getMerchantId());
        cb.setProductCategory(t.getProductCategory());
        cb.setCustomerCountry(t.getCustomerCountry());
        cb.setCustomerEmail(t.getCustomerEmail());
        cb.setCustomerIp(t.getCustomerIp());
        cb.setAmount(t.getAmount());
        cb.setCurrency(t.getCurrency());
        cb.setReasonCode(code);
        cb.setReasonDescription(reason.description());
        cb.setReasonCategory(reason.category());
        cb.setStatus(status);
        cb.setOpenedAt(opened);
        cb.setDeadlineAt(deadline);
        cb.setResponded(responded);
        cb.setResolvedAt(resolved);
        return cb;
    }

    /** 6 disputes sharing one email + IP within a short window (fraud ring). */
    private void injectFraudRing(List<Transaction> txns, List<Chargeback> cbs, OffsetDateTime now) {
        String email = "quick.refunds.collective@example.com";
        String ip = "203.0.113.77";
        for (int i = 0; i < 6; i++) {
            OffsetDateTime created = now.minusDays(20).plusDays(i);
            Transaction t = new Transaction();
            t.setId("txn_ring_" + UUID.randomUUID().toString().substring(0, 8));
            t.setMerchantId(pick(GeneratorConfig.MERCHANTS));
            t.setProductCategory("Silver Jewelry");
            t.setCustomerCountry("US");
            t.setCustomerEmail(email);
            t.setCustomerIp(ip);
            t.setAmount(BigDecimal.valueOf(180 + RNG.nextInt(240)).setScale(2, RoundingMode.HALF_UP));
            t.setCurrency("USD");
            t.setCreatedAt(created);
            txns.add(t);

            Chargeback cb = buildChargeback(t, "10.4", created, now, true);
            cb.setDeadlineAt(now.plusDays(2 + RNG.nextInt(8))); // live, surfaces in prioritized
            cbs.add(cb);
        }
    }

    private BigDecimal sampleAmount() {
        double r = RNG.nextDouble(), v;
        if (r < 0.80)       v = 30 + RNG.nextDouble() * 120;   // $30-150
        else if (r < 0.95)  v = 15 + RNG.nextDouble() * 285;   // $15-300
        else                v = 300 + RNG.nextDouble() * 500;  // $300-800
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private String weightedReason() {
        return weightedPick(
            new ArrayList<>(GeneratorConfig.REASON_WEIGHTS.keySet()),
            new ArrayList<>(GeneratorConfig.REASON_WEIGHTS.values()));
    }

    private <T> T pick(List<T> list) { return list.get(RNG.nextInt(list.size())); }

    private <T> T weightedPick(List<T> items, List<Integer> weights) {
        int total = weights.stream().mapToInt(Integer::intValue).sum();
        int r = RNG.nextInt(total);
        for (int i = 0; i < items.size(); i++) {
            r -= weights.get(i);
            if (r < 0) return items.get(i);
        }
        return items.get(items.size() - 1);
    }
}
