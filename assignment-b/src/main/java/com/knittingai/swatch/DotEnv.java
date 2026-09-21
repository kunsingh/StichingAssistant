package com.knittingai.swatch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

final class DotEnv {
    private DotEnv() {}

    static Map<String, String> load() {
        Map<String, String> values = new LinkedHashMap<>(System.getenv());
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(".env");
            if (Files.isRegularFile(candidate)) {
                try {
                    for (String rawLine : Files.readAllLines(candidate)) {
                        String line = rawLine.strip();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        if (line.startsWith("export ")) line = line.substring(7);
                        int separator = line.indexOf('=');
                        if (separator < 1) continue;
                        String key = line.substring(0, separator).strip();
                        String value = line.substring(separator + 1).strip();
                        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'")))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        values.putIfAbsent(key, value);
                    }
                } catch (IOException exception) {
                    throw new IllegalStateException("Unable to read " + candidate, exception);
                }
                break;
            }
            current = current.getParent();
        }
        return values;
    }
}
