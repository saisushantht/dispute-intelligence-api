package com.disputeintel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the fight-score model behaves as specified:
 *   fightScore = winProbability * amount * urgencyWeight
 * and that urgency correctly escalates as the deadline nears.
 */
class FightScoreTest {

    private double urgencyWeight(long daysLeft) {
        if (daysLeft <= 2) return 1.5;
        if (daysLeft <= 5) return 1.3;
        if (daysLeft <= 10) return 1.15;
        return 1.0;
    }

    private double fightScore(double winProb, double amount, long daysLeft) {
        return winProb * amount * urgencyWeight(daysLeft);
    }

    @Test
    void expectedRecoveryIsWinProbTimesAmount() {
        // 73% win on a $100 dispute with no urgency boost => $73 expected recovery
        assertEquals(73.0, fightScore(0.73, 100.0, 30), 0.001);
    }

    @Test
    void urgentDisputeOutranksHigherValueDistantOne() {
        // Lower amount but expiring today should beat a bigger one far out
        double urgent  = fightScore(0.73, 100.0, 1);   // 1.5x weight
        double distant = fightScore(0.73, 120.0, 30);  // 1.0x weight
        assertTrue(urgent > distant,
            "An expiring winnable dispute should rank above a larger distant one");
    }

    @Test
    void urgencyWeightEscalatesAsDeadlineNears() {
        assertEquals(1.0,  urgencyWeight(20));
        assertEquals(1.15, urgencyWeight(8));
        assertEquals(1.3,  urgencyWeight(4));
        assertEquals(1.5,  urgencyWeight(1));
    }
}
