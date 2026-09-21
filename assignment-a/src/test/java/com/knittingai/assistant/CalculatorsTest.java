package com.knittingai.assistant;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CalculatorsTest {
    @Test void calculatesKnownDkBlanketEstimate() {
        var result = Calculators.calculateYarnQuantity(50, 60, "DK", "stockinette", null, null);
        assertEquals(364, result.metres());
        assertEquals(2, result.balls());
    }

    @Test void cablesUseMoreThanStockinette() {
        var plain = Calculators.calculateYarnQuantity(50, 60, "DK", "stockinette", null, null);
        var cable = Calculators.calculateYarnQuantity(50, 60, "DK", "cable", null, null);
        assertTrue(cable.metres() > plain.metres());
    }

    @Test void tighterGaugeIncreasesEstimate() {
        var typical = Calculators.calculateYarnQuantity(50, 60, "DK", "stockinette", 22.0, null);
        var tight = Calculators.calculateYarnQuantity(50, 60, "DK", "stockinette", 26.0, null);
        assertTrue(tight.metres() > typical.metres());
    }

    @Test void rejectsInvalidDimension() {
        var error = assertThrows(IllegalArgumentException.class,
            () -> Calculators.calculateYarnQuantity(0, 60, "DK", "stockinette", null, null));
        assertTrue(error.getMessage().contains("width_cm"));
    }

    @Test void recommendsExpectedNeedle() {
        var result = Calculators.recommendNeedleSize("worsted", "scarf", "balanced");
        assertEquals(5.5, result.metricMm());
        assertEquals("9", result.usSize());
    }

    @Test void diagnosesTension() {
        assertEquals("too tight", Calculators.diagnoseTension(22, 24).direction());
        assertEquals("moderate", Calculators.diagnoseTension(22, 24).severity());
        assertEquals("too loose", Calculators.diagnoseTension(22, 19).direction());
    }
}
