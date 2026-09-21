package com.knittingai.assistant;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class KnittingAssistant {
    public record AssistantResponse(
        String answer, String calculation, Map<String, Object> values, boolean offline, String providerStatus
    ) {
        public boolean providerCalled() {
            return Set.of("accepted", "rejected_numbers", "unavailable").contains(providerStatus);
        }
    }

    public interface LanguageProvider {
        String phrase(String question, String calculation, Map<String, Object> values);

        default Map<String, Object> understand(String question) {
            return null;
        }
    }

    private record Calculation(String name, Map<String, Object> values) {}

    private static final String WEIGHTS = "lace|fingering|sport|dk|worsted|aran|bulky|super bulky";
    private static final String STITCHES = "stockinette|garter|rib|seed|moss|cable|lace";
    private static final Pattern DIMENSIONS = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:x|by)\\s*(\\d+(?:\\.\\d+)?)\\s*cm");
    private static final Pattern WEIGHT = Pattern.compile("\\b(" + WEIGHTS + ")\\b");
    private static final Pattern STITCH = Pattern.compile("\\b(" + STITCHES + ")\\b");
    private static final Pattern GAUGE = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*stitches?(?:\\s+per|\\s*/)\\s*10\\s*cm");
    private static final Pattern FABRIC = Pattern.compile("\\b(firm|balanced|drapey)\\b");
    private static final Pattern PROJECT = Pattern.compile("\\b(socks?|hat|scarf|blanket|sweater)\\b");
    private static final Pattern TENSION = Pattern.compile(
        "(?:swatch|actual)(?:\\s+gauge)?(?:\\s+is)?\\s*(\\d+(?:\\.\\d+)?)\\s*stitches?.*?"
            + "(?:pattern|target)(?:\\s+(?:says|is))?\\s*(\\d+(?:\\.\\d+)?)"
    );
    private static final Pattern NUMBER = Pattern.compile("(?<![A-Za-z])\\d+(?:\\.\\d+)?");
    private final LanguageProvider provider;

    public KnittingAssistant() {
        this(null);
    }

    public KnittingAssistant(LanguageProvider provider) {
        this.provider = provider;
    }

    public AssistantResponse answer(String rawQuestion) {
        String question = rawQuestion == null ? "" : rawQuestion.trim();
        if (question.isEmpty()) return decline("Please ask a knitting calculation question.");
        final Calculation result;
        try {
            result = understand(question);
        } catch (IllegalArgumentException exception) {
            return decline("I can't calculate that safely: " + exception.getMessage() + ".");
        }
        if (result == null) return decline(
            "I can't answer that reliably with the calculations I have. "
                + "I can estimate yarn quantity, recommend needle sizes, or troubleshoot stitch gauge.");
        String deterministic = offlinePhrase(result);
        if (provider == null) return new AssistantResponse(deterministic, result.name(), result.values(), true, "disabled");
        for (int attempt = 0; attempt < 2; attempt++) {
            final String candidate;
            try {
                candidate = provider.phrase(question, result.name(), result.values());
            } catch (RuntimeException exception) {
                return new AssistantResponse(deterministic
                    + " (The language service is unavailable, so this is the offline answer.)",
                    result.name(), result.values(), true, "unavailable");
            }
            if (numbersAreTrusted(candidate, deterministic, question)) {
                return new AssistantResponse(sanitizeForDisplay(candidate), result.name(), result.values(), false, "accepted");
            }
        }
        return new AssistantResponse(deterministic, result.name(), result.values(), true, "rejected_numbers");
    }

    private Calculation understand(String question) {
        if (provider != null) {
            Map<String, Object> intent = provider.understand(question);
            if (intent != null) {
                Calculation result = executeIntent(intent, question);
                if (result != null) return result;
            }
        }
        return calculate(question);
    }

    private Calculation executeIntent(Map<String, Object> intent, String question) {
        String calculation = String.valueOf(intent.get("calculation"));
        if ("yarn_quantity".equals(calculation)) {
            Double width = checkedNumber(intent.get("width_cm"), question);
            Double height = checkedNumber(intent.get("height_cm"), question);
            Double gauge = checkedNumber(intent.get("gauge"), question);
            if (width == null || height == null) return null;
            var result = Calculators.calculateYarnQuantity(width, height,
                String.valueOf(intent.getOrDefault("yarn_weight", "")),
                String.valueOf(intent.getOrDefault("stitch_type", "stockinette")), gauge, null);
            return new Calculation(calculation, Calculators.values(result));
        }
        if ("needle_size".equals(calculation)) {
            var result = Calculators.recommendNeedleSize(
                String.valueOf(intent.getOrDefault("yarn_weight", "")),
                String.valueOf(intent.getOrDefault("project_type", "")),
                String.valueOf(intent.getOrDefault("desired_fabric", "balanced")));
            return new Calculation(calculation, Calculators.values(result));
        }
        if ("tension".equals(calculation)) {
            Double target = checkedNumber(intent.get("target"), question);
            Double actual = checkedNumber(intent.get("actual"), question);
            if (target == null || actual == null) return null;
            return new Calculation(calculation, Calculators.values(Calculators.diagnoseTension(target, actual)));
        }
        return null;
    }

    private static Double checkedNumber(Object rawValue, String question) {
        if (rawValue == null) return null;
        double value = rawValue instanceof Number number ? number.doubleValue() : Double.parseDouble(rawValue.toString());
        if (!extractNumbers(question).contains(value)) {
            throw new IllegalArgumentException("the model referenced a measurement that is not in the question");
        }
        return value;
    }

    private Calculation calculate(String question) {
        String lowered = question.toLowerCase().replace('×', 'x').trim().replaceAll("\\s+", " ");
        Matcher dimensions = DIMENSIONS.matcher(lowered);
        Matcher weight = WEIGHT.matcher(lowered);
        Matcher stitch = STITCH.matcher(lowered);
        Matcher gauge = GAUGE.matcher(lowered);
        if (dimensions.find() && weight.find()
            && (lowered.contains("yarn") || lowered.contains("metres") || lowered.contains("meters"))) {
            String stitchName = stitch.find() ? stitch.group(1) : "stockinette";
            Double gaugeValue = gauge.find() ? Double.valueOf(gauge.group(1)) : null;
            var estimate = Calculators.calculateYarnQuantity(
                Double.parseDouble(dimensions.group(1)), Double.parseDouble(dimensions.group(2)),
                weight.group(1), stitchName, gaugeValue, null);
            return new Calculation("yarn_quantity", Calculators.values(estimate));
        }
        Matcher fabric = FABRIC.matcher(lowered);
        Matcher project = PROJECT.matcher(lowered);
        if (weight.reset().find() && project.find() && (lowered.contains("needle") || lowered.contains("needles"))) {
            String projectName = project.group(1).startsWith("sock") ? "socks" : project.group(1);
            var recommendation = Calculators.recommendNeedleSize(
                weight.group(1), projectName, fabric.find() ? fabric.group(1) : "balanced");
            return new Calculation("needle_size", Calculators.values(recommendation));
        }
        Matcher tension = TENSION.matcher(lowered);
        if (tension.find()) {
            var diagnosis = Calculators.diagnoseTension(
                Double.parseDouble(tension.group(2)), Double.parseDouble(tension.group(1)));
            return new Calculation("tension", Calculators.values(diagnosis));
        }
        return null;
    }

    private static String offlinePhrase(Calculation calculation) {
        Map<String, Object> values = calculation.values();
        return switch (calculation.name()) {
            case "yarn_quantity" -> "My calculator estimates " + values.get("metres") + " metres, or "
                + values.get("balls") + " balls/skeins at " + values.get("ball_metres")
                + " metres each. It used a gauge of " + values.get("gauge_used")
                + " stitches per 10 cm and includes 10% extra.";
            case "needle_size" -> "Start with " + values.get("metric_mm") + " mm needles (UK "
                + values.get("uk_size") + ", US " + values.get("us_size") + "). " + values.get("rationale");
            default -> "Your tension is " + values.get("direction") + " by " + values.get("difference_percent")
                + "% (" + values.get("severity") + " difference). "
                + ((java.util.List<?>) values.get("suggestions")).stream().map(Object::toString).collect(Collectors.joining(" "));
        };
    }

    private static boolean numbersAreTrusted(String candidate, String deterministic, String question) {
        Set<Double> resultNumbers = extractNumbers(deterministic);
        Set<Double> allowed = new HashSet<>(resultNumbers);
        allowed.addAll(extractNumbers(question));
        Set<Double> candidateNumbers = extractNumbers(candidate);
        return candidateNumbers.containsAll(resultNumbers) && allowed.containsAll(candidateNumbers);
    }

    private static Set<Double> extractNumbers(String text) {
        Set<Double> values = new HashSet<>();
        Matcher matcher = NUMBER.matcher(text);
        while (matcher.find()) values.add(Double.valueOf(matcher.group()));
        return values;
    }

    private static AssistantResponse decline(String message) {
        return new AssistantResponse(message, null, new LinkedHashMap<>(), true, "disabled");
    }

    private static String sanitizeForDisplay(String text) {
        return text
            .replace("\u2018", "'").replace("\u2019", "'")
            .replace("\u201C", "\"").replace("\u201D", "\"")
            .replace("\u2013", "-").replace("\u2014", "-")
            .replace("\u2026", "...")
            .replace("\u00D7", "x")
            .replace("\u00A0", " ");
    }
}
