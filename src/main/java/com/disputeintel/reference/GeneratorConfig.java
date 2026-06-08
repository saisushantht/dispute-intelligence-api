package com.disputeintel.reference;

import java.util.List;
import java.util.Map;

/**
 * Tunable distributions for synthetic data generation. Centralised here so the
 * generator stays readable and the "realistic patterns" are explicit/auditable.
 */
public final class GeneratorConfig {
    private GeneratorConfig() {}

    public static final int TARGET_TRANSACTIONS = 12000; // ~2.7% become disputes
    public static final double BASE_CB_RATE = 0.020;
    public static final int WINDOW_DAYS = 365;

    public static final List<String> CURRENCIES = List.of("USD", "EUR", "GBP");
    public static final List<Integer> CURRENCY_WEIGHTS = List.of(6, 3, 2);

    public static final List<String> COUNTRIES =
        List.of("US", "DE", "GB", "FR", "NL", "CA", "AU", "SG", "JP", "SE");

    public static final List<String> MERCHANTS =
        List.of("SELLER_BALI_001", "SELLER_HANOI_002",
                "SELLER_CHIANGMAI_003", "SELLER_MANILA_004");

    public static final List<String> CATEGORIES =
        List.of("Handwoven Textiles", "Wood Carvings", "Ceramics & Pottery",
                "Silver Jewelry", "Spices & Tea", "Leather Goods");

    /** Handwoven Textiles ~3.5x the dispute propensity of the rest. */
    public static final Map<String, Double> CATEGORY_CB_MULTIPLIER = Map.of(
        "Handwoven Textiles", 3.5,
        "Wood Carvings", 1.0,
        "Ceramics & Pottery", 0.9,
        "Silver Jewelry", 1.1,
        "Spices & Tea", 0.8,
        "Leather Goods", 1.0
    );

    /** Reason code -> sampling weight (fraud common, processing rare). */
    public static final Map<String, Integer> REASON_WEIGHTS = Map.of(
        "10.4", 26, "83", 22, "10.1", 7,
        "13.1", 14, "13.3", 10, "4853", 9, "13.7", 5,
        "12.6", 3, "4834", 2, "12.5", 2
    );

    /** True win probability per code (drives won/lost labelling only). */
    public static final Map<String, Double> REASON_WIN_PROB = Map.of(
        "10.4", 0.12,  // low-win (<20%) requirement
        "83", 0.15, "10.1", 0.20,
        "13.1", 0.78,  // high-win (>70%) requirement
        "13.3", 0.45, "4853", 0.42, "13.7", 0.38,
        "12.6", 0.82, "4834", 0.80, "12.5", 0.75
    );
}
