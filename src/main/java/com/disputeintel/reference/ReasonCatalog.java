package com.disputeintel.reference;

import java.util.Map;
import java.util.Set;

/** Static catalog mapping a reason code to its description and category. */
public final class ReasonCatalog {
    public record Reason(String description, String category) {}

    private static final Map<String, Reason> CODES = Map.ofEntries(
            Map.entry("10.4", new Reason("Fraud - Card absent environment (Visa)", "fraud")),
            Map.entry("83",   new Reason("Fraud - Card not present (Mastercard)", "fraud")),
            Map.entry("10.1", new Reason("EMV liability shift counterfeit fraud (Visa)", "fraud")),
            Map.entry("13.1", new Reason("Merchandise/services not received (Visa)", "product")),
            Map.entry("13.3", new Reason("Not as described or defective (Visa)", "product")),
            Map.entry("4853", new Reason("Cardholder dispute - defective/not as described (MC)", "cardholder")),
            Map.entry("13.7", new Reason("Cancelled merchandise/services (Visa)", "cardholder")),
            Map.entry("12.6", new Reason("Duplicate processing/paid by other means (Visa)", "processing")),
            Map.entry("4834", new Reason("Duplicate transaction / incorrect amount (MC)", "processing")),
            Map.entry("12.5", new Reason("Incorrect amount charged (Visa)", "processing"))
    );

    private ReasonCatalog() {}

    public static Reason lookup(String code) {
        return CODES.getOrDefault(code, new Reason("Unknown reason code", "other"));
    }

    public static Set<String> allCodes() {
        return CODES.keySet();
    }
}