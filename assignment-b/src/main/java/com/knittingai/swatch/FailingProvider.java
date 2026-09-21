package com.knittingai.swatch;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public final class FailingProvider implements ImageProvider {
    private final String message;
    private final AtomicInteger callCount = new AtomicInteger();

    public FailingProvider() {
        this("simulated provider failure");
    }

    public FailingProvider(String message) {
        this.message = message;
    }

    @Override
    public void generate(String prompt, Path outputPath) {
        callCount.incrementAndGet();
        throw new IllegalStateException(message);
    }

    public int callCount() {
        return callCount.get();
    }
}
