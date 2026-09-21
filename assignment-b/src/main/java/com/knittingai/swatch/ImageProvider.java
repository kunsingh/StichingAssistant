package com.knittingai.swatch;

import java.nio.file.Path;

@FunctionalInterface
public interface ImageProvider {
    void generate(String prompt, Path outputPath) throws Exception;
}
