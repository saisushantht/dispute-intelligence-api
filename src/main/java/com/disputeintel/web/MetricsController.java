package com.disputeintel.web;

import com.disputeintel.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Metrics & Analysis", description = "Global, segmented, and anomaly metrics")
public class MetricsController {

    private final MetricsService metrics;

    public MetricsController(MetricsService metrics) {
        this.metrics = metrics;
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
}
