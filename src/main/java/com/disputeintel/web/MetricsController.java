package com.disputeintel.web;

import com.disputeintel.service.FraudService;
import com.disputeintel.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Metrics & Analysis", description = "Global, segmented, anomaly, and fraud metrics")
public class MetricsController {

    private final MetricsService metrics;
    private final FraudService fraud;

    public MetricsController(MetricsService metrics, FraudService fraud) {
        this.metrics = metrics;
        this.fraud = fraud;
    }

    @Operation(summary = "Global headline metrics")
    @GetMapping
    public Map<String, Object> global() {
        return metrics.global();
    }

    @Operation(summary = "Metrics segmented by a dimension",
               description = "dimension = reasonCode | productCategory | customerCountry | merchantId | reasonCategory")
    @GetMapping("/segments")
    public List<Map<String, Object>> segments(
            @RequestParam(defaultValue = "productCategory") String dimension) {
        return metrics.segmented(dimension);
    }

    @Operation(summary = "Anomalous segments with abnormally high chargeback rates",
               description = "dimension = productCategory | customerCountry | merchantId")
    @GetMapping("/anomalies")
    public Map<String, Object> anomalies(
            @RequestParam(defaultValue = "productCategory") String dimension,
            @RequestParam(defaultValue = "2.0") double threshold) {
        return metrics.anomalies(dimension, threshold);
    }

    @Operation(summary = "Potential fraud rings (shared customer email + IP)",
               description = "Flags email+IP pairs with at least minCount disputes")
    @GetMapping("/fraud-rings")
    public List<Map<String, Object>> fraudRings(
            @RequestParam(defaultValue = "5") long minCount) {
        return fraud.detectRings(minCount);
    }
}
