package com.disputeintel.web;

import com.disputeintel.service.PrioritizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/disputes")
@Tag(name = "Prioritization", description = "Fight-score ranking of open disputes")
public class PrioritizationController {

    private final PrioritizationService prioritization;

    public PrioritizationController(PrioritizationService prioritization) {
        this.prioritization = prioritization;
    }

    @Operation(summary = "Top disputes to fight this week, ranked by fight score",
               description = "fightScore = winProbability(reasonCode) * amount * urgencyWeight. "
                           + "Only open disputes within their deadline are included.")
    @GetMapping("/prioritized")
    public List<Map<String, Object>> prioritized(
            @RequestParam(defaultValue = "10") int limit) {
        return prioritization.prioritized(limit);
    }
}
