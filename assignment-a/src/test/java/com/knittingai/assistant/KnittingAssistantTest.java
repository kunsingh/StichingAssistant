package com.knittingai.assistant;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class KnittingAssistantTest {
    @Test void routesYarnQuestion() {
        var response = new KnittingAssistant().answer("How much DK yarn for a 50 x 60cm blanket in stockinette?");
        assertEquals("yarn_quantity", response.calculation());
        assertTrue(response.answer().contains("364 metres"));
    }

    @Test void routesNeedleAndTensionQuestions() {
        var needle = new KnittingAssistant().answer("What needle size for worsted yarn for a balanced scarf?");
        assertEquals("needle_size", needle.calculation());
        assertTrue(needle.answer().contains("5.5 mm"));
        var tension = new KnittingAssistant().answer(
            "My swatch is 24 stitches per 10cm but the pattern says 22. What's wrong?");
        assertEquals("tension", tension.calculation());
        assertTrue(tension.answer().contains("too tight"));
    }

    @Test void declinesUnsupportedQuestion() {
        var response = new KnittingAssistant().answer("Which sweater pattern is prettiest?");
        assertNull(response.calculation());
        assertTrue(response.answer().contains("can't answer"));
    }

    @Test void providerFailureUsesOfflineAnswer() {
        var response = new KnittingAssistant(new KnittingAssistant.LanguageProvider() {
            @Override public String phrase(String q, String c, java.util.Map<String, Object> v) {
                throw new RuntimeException("outage");
            }
        })
            .answer("How much DK yarn for a 50 x 60cm blanket in stockinette?");
        assertTrue(response.offline());
        assertTrue(response.answer().contains("364 metres"));
    }

    @Test void inventedProviderNumberIsRejected() {
        var response = new KnittingAssistant(new KnittingAssistant.LanguageProvider() {
            @Override public String phrase(String q, String c, java.util.Map<String, Object> v) {
                return "Buy 999 balls.";
            }
        })
            .answer("How much DK yarn for a 50 x 60cm blanket in stockinette?");
        assertTrue(response.offline());
        assertFalse(response.answer().contains("999"));
    }
}
