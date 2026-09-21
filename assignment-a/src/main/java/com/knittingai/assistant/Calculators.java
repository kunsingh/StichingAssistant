package com.knittingai.assistant;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Calculators {
    public record YarnWeightSpec(double typicalGauge, int metresPer100g, double baseMetresPerSquareMetre) {}
    public record YarnEstimate(int metres, int balls, int ballMetres, double gaugeUsed, List<String> assumptions) {}
    public record NeedleRecommendation(double metricMm, String ukSize, String usSize, String rationale) {}
    public record TensionDiagnosis(String direction, String severity, double differencePercent, List<String> suggestions) {}
    private record NeedleSize(double metric, String uk, String us) {}

    private static final Map<String, YarnWeightSpec> YARN_WEIGHTS = Map.of(
        "lace", new YarnWeightSpec(30, 800, 650),
        "fingering", new YarnWeightSpec(28, 400, 800),
        "sport", new YarnWeightSpec(24, 300, 950),
        "dk", new YarnWeightSpec(22, 220, 1100),
        "worsted", new YarnWeightSpec(19, 180, 1250),
        "aran", new YarnWeightSpec(17, 160, 1400),
        "bulky", new YarnWeightSpec(14, 120, 1600),
        "super bulky", new YarnWeightSpec(10, 80, 1900)
    );
    private static final Map<String, Double> STITCH_MULTIPLIERS = Map.of(
        "stockinette", 1.0, "garter", 1.25, "rib", 1.15, "seed", 1.2,
        "moss", 1.2, "cable", 1.35, "lace", 0.8
    );
    private static final Map<String, Double> BASE_NEEDLE_MM = Map.of(
        "lace", 2.25, "fingering", 3.0, "sport", 3.5, "dk", 4.0,
        "worsted", 5.0, "aran", 5.5, "bulky", 6.5, "super bulky", 9.0
    );
    private static final Map<String, Double> FABRIC_ADJUSTMENT = Map.of("firm", -0.5, "balanced", 0.0, "drapey", 1.0);
    private static final Map<String, Double> PROJECT_ADJUSTMENT = Map.of(
        "socks", -0.25, "hat", 0.0, "scarf", 0.5, "blanket", 0.5, "sweater", 0.0
    );
    private static final List<NeedleSize> NEEDLES = List.of(
        new NeedleSize(1.75, "15", "00"), new NeedleSize(2.0, "14", "0"),
        new NeedleSize(2.25, "13", "1"), new NeedleSize(2.5, "-", "1.5"),
        new NeedleSize(2.75, "12", "2"), new NeedleSize(3.0, "11", "2.5"),
        new NeedleSize(3.25, "10", "3"), new NeedleSize(3.5, "-", "4"),
        new NeedleSize(3.75, "9", "5"), new NeedleSize(4.0, "8", "6"),
        new NeedleSize(4.5, "7", "7"), new NeedleSize(5.0, "6", "8"),
        new NeedleSize(5.5, "5", "9"), new NeedleSize(6.0, "4", "10"),
        new NeedleSize(6.5, "3", "10.5"), new NeedleSize(8.0, "0", "11"),
        new NeedleSize(9.0, "00", "13"), new NeedleSize(10.0, "000", "15")
    );

    private Calculators() {}

    public static YarnEstimate calculateYarnQuantity(
        double widthCm, double heightCm, String yarnWeight, String stitchType,
        Double gaugeStitchesPer10cm, Integer ballMetres
    ) {
        requirePositive("width_cm", widthCm);
        requirePositive("height_cm", heightCm);
        String weightKey = normalize(yarnWeight);
        String stitchKey = normalize(stitchType);
        YarnWeightSpec spec = YARN_WEIGHTS.get(weightKey);
        if (spec == null) throw new IllegalArgumentException("unsupported yarn weight: " + yarnWeight);
        Double multiplier = STITCH_MULTIPLIERS.get(stitchKey);
        if (multiplier == null) throw new IllegalArgumentException("unsupported stitch type: " + stitchType);
        double gauge = gaugeStitchesPer10cm == null ? spec.typicalGauge() : gaugeStitchesPer10cm;
        requirePositive("gauge_stitches_per_10cm", gauge);
        int metresPerBall = ballMetres == null ? spec.metresPer100g() : ballMetres;
        requirePositive("ball_metres", metresPerBall);
        double area = widthCm * heightCm / 10_000;
        double gaugeFactor = Math.pow(gauge / spec.typicalGauge(), 2);
        int metres = (int) Math.ceil(area * spec.baseMetresPerSquareMetre() * multiplier * gaugeFactor * 1.10);
        return new YarnEstimate(metres, (int) Math.ceil((double) metres / metresPerBall), metresPerBall, gauge, List.of(
            "Includes a 10% allowance for swatching, joining, and finishing.",
            "Assumes " + metresPerBall + " metres per ball/skein.",
            "Estimate excludes fringe, borders, colourwork, and pattern-specific shaping."
        ));
    }

    public static NeedleRecommendation recommendNeedleSize(String yarnWeight, String projectType, String desiredFabric) {
        String weight = normalize(yarnWeight);
        String project = normalize(projectType);
        String fabric = normalize(desiredFabric);
        if (!BASE_NEEDLE_MM.containsKey(weight)) throw new IllegalArgumentException("unsupported yarn weight: " + yarnWeight);
        if (!PROJECT_ADJUSTMENT.containsKey(project)) throw new IllegalArgumentException("unsupported project type: " + projectType);
        if (!FABRIC_ADJUSTMENT.containsKey(fabric)) throw new IllegalArgumentException("unsupported fabric: " + desiredFabric);
        double target = BASE_NEEDLE_MM.get(weight) + PROJECT_ADJUSTMENT.get(project) + FABRIC_ADJUSTMENT.get(fabric);
        NeedleSize size = NEEDLES.stream().min(Comparator.comparingDouble((NeedleSize n) -> Math.abs(n.metric() - target))
            .thenComparingDouble(NeedleSize::metric)).orElseThrow();
        return new NeedleRecommendation(size.metric(), size.uk(), size.us(),
            "Based on a " + weight + " starting size, adjusted for a " + fabric + " " + project
                + " fabric. Always confirm with a swatch.");
    }

    public static TensionDiagnosis diagnoseTension(double target, double actual) {
        requirePositive("target_stitches_per_10cm", target);
        requirePositive("actual_stitches_per_10cm", actual);
        double difference = (actual - target) / target * 100;
        double magnitude = Math.abs(difference);
        String severity = magnitude < 5 ? "small" : magnitude < 10 ? "moderate" : "large";
        double rounded = Math.round(magnitude * 10.0) / 10.0;
        if (magnitude < 2) return new TensionDiagnosis("on target", "none", rounded,
            List.of("Wash and block the swatch before making a final decision."));
        if (difference > 0) return new TensionDiagnosis("too tight", severity, rounded, List.of(
            "Try a larger needle size.", "Relax your grip and avoid pulling the working yarn after each stitch.",
            "Wash and block a larger swatch, then measure again."));
        return new TensionDiagnosis("too loose", severity, rounded, List.of(
            "Try a smaller needle size.", "Keep the working yarn consistently tensioned.",
            "Wash and block a larger swatch, then measure again."));
    }

    public static Map<String, Object> values(YarnEstimate value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metres", value.metres()); result.put("balls", value.balls());
        result.put("ball_metres", value.ballMetres()); result.put("gauge_used", value.gaugeUsed());
        result.put("assumptions", value.assumptions());
        return result;
    }

    public static Map<String, Object> values(NeedleRecommendation value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metric_mm", value.metricMm()); result.put("uk_size", value.ukSize());
        result.put("us_size", value.usSize()); result.put("rationale", value.rationale());
        return result;
    }

    public static Map<String, Object> values(TensionDiagnosis value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("direction", value.direction()); result.put("severity", value.severity());
        result.put("difference_percent", value.differencePercent()); result.put("suggestions", value.suggestions());
        return result;
    }

    private static String normalize(String value) {
        return value.toLowerCase().replace("-", " ").trim().replaceAll("\\s+", " ");
    }

    private static void requirePositive(String name, double value) {
        if (value <= 0) throw new IllegalArgumentException(name + " must be greater than zero");
    }
}
