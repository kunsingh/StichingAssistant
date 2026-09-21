package com.knittingai.swatch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SwatchGenerator {
    public record GenerationResult(
        Path imagePath, String cacheKey, boolean cacheHit, boolean usedFallback,
        double elapsedSeconds, String prompt, String error
    ) {}

    private final ImageProvider provider;
    private final DiskCache cache;
    private final Path workDirectory;

    public SwatchGenerator(ImageProvider provider, Path cacheDirectory, Path workDirectory) throws IOException {
        this.provider = provider;
        this.cache = new DiskCache(cacheDirectory);
        this.workDirectory = workDirectory;
        Files.createDirectories(workDirectory);
    }

    public GenerationResult generate(SwatchInput value) throws IOException {
        long started = System.nanoTime();
        SwatchInput item = value.normalized();
        String prompt = PromptBuilder.buildPrompt(item);
        var cached = cache.get(item);
        if (cached.isPresent()) {
            Map<String, Object> metadata = cached.get().metadata();
            return new GenerationResult(cached.get().imagePath(), DiskCache.key(item), true,
                Boolean.TRUE.equals(metadata.get("used_fallback")), elapsed(started), prompt,
                metadata.get("error") == null ? null : metadata.get("error").toString());
        }
        String key = DiskCache.key(item);
        Path temporary = workDirectory.resolve(key + ".png");
        boolean usedFallback = false;
        String error = null;
        try {
            provider.generate(prompt, temporary);
        } catch (Exception exception) {
            usedFallback = true;
            error = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            PlaceholderRenderer.create(item, temporary);
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("used_fallback", usedFallback);
        metadata.put("error", error);
        metadata.put("prompt", prompt);
        DiskCache.CachedValue stored = cache.put(item, temporary, metadata);
        Files.deleteIfExists(temporary);
        return new GenerationResult(stored.imagePath(), key, false, usedFallback, elapsed(started), prompt, error);
    }

    private static double elapsed(long started) {
        return (System.nanoTime() - started) / 1_000_000_000.0;
    }
}
