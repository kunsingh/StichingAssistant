package com.knittingai.swatch;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SwatchPreviewTest {
    @TempDir Path root;

    @Test void normalizationAndCacheKeyAreDeterministic() {
        var first = new SwatchInput(" 1x1 RIB ", " Royal   Purple ", "#5b3e96", " WORSTED ", " Wool ");
        var second = new SwatchInput("rib", "royal purple", "#5B3E96", "worsted", "wool");
        assertEquals(first.normalized(), second.normalized());
        assertEquals(DiskCache.key(first), DiskCache.key(second));
        assertNotEquals(DiskCache.key(first), DiskCache.key(
            new SwatchInput("rib", "royal purple", "#5B3E96", "dk", "wool")));
    }

    @Test void promptContainsExactColourAndStitchStructure() {
        String cable = PromptBuilder.buildPrompt(new SwatchInput("cable", "purple", "#5B3E96"));
        assertTrue(cable.contains("#5B3E96"));
        assertTrue(cable.contains("rope-like"));
        assertTrue(cable.contains("twisting and crossing"));
        String stockinette = PromptBuilder.buildPrompt(new SwatchInput("stockinette", "purple", "#5B3E96"));
        assertTrue(stockinette.contains("\"V\" shapes"));
        assertNotEquals(cable, stockinette);
    }

    @Test void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class,
            () -> new SwatchInput("unknown", "purple", "#5B3E96").normalized());
        assertThrows(IllegalArgumentException.class,
            () -> new SwatchInput("rib", "purple", "purple").normalized());
    }

    @Test void providerFailureFallsBackThenHitsCache() throws Exception {
        FailingProvider provider = new FailingProvider();
        SwatchGenerator generator = new SwatchGenerator(provider, root.resolve("cache"), root.resolve("work"));
        SwatchInput value = new SwatchInput("cable", "purple", "#5B3E96");
        var first = generator.generate(value);
        var second = generator.generate(value);
        assertTrue(Files.isRegularFile(first.imagePath()));
        assertTrue(first.usedFallback());
        assertFalse(first.cacheHit());
        assertTrue(second.cacheHit());
        assertEquals(1, provider.callCount());
        assertEquals(new Color(91, 62, 150).getRGB(), ImageIO.read(first.imagePath().toFile()).getRGB(0, 0));
    }

    @Test void allStitchesProduceValidDifferentPlaceholders() throws Exception {
        SwatchGenerator generator = new SwatchGenerator(new FailingProvider(), root.resolve("cache"), root.resolve("work"));
        java.util.Set<Integer> hashes = new java.util.HashSet<>();
        for (String stitch : java.util.List.of("stockinette", "garter", "rib", "seed", "cable", "lace")) {
            var result = generator.generate(new SwatchInput(stitch, "blue", "#167D9A"));
            assertNotNull(ImageIO.read(result.imagePath().toFile()));
            hashes.add(java.util.Arrays.hashCode(Files.readAllBytes(result.imagePath())));
        }
        assertEquals(6, hashes.size());
    }

    @Test void ravelryWithoutCredentialsIsGraceful() {
        var result = new RavelryClient("", "").findReference("cable", root);
        assertFalse(result.matched());
        assertTrue(result.message().contains("not configured"));
    }

    @Test void offlineDemoPersistsEvidence() throws Exception {
        var report = SwatchPreviewApplication.runDemo(root.resolve("artifacts"), false);
        assertEquals(true, report.get("cache_hit_demonstrated"));
        assertEquals(true, report.get("simulated_provider_failure"));
        for (String filename : java.util.List.of(
            "gallery.html", "colour-switching.png", "stitch-variation.png", "ravelry-comparison.png", "demo-report.json")) {
            assertTrue(Files.isRegularFile(root.resolve("artifacts").resolve(filename)));
        }
    }
}
