package com.knittingai.swatch;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public record SwatchInput(
    String stitchType, String colourName, String colourHex, String weightCategory, String fibreContent
) {
    private static final Set<String> SUPPORTED_STITCHES =
        Set.of("stockinette", "garter", "rib", "seed", "cable", "lace");
    private static final Map<String, String> ALIASES = Map.of(
        "moss", "seed", "seed / moss", "seed", "1x1 rib", "rib", "2x2 rib", "rib", "ribbed", "rib"
    );
    private static final Pattern HEX = Pattern.compile("^#[0-9A-F]{6}$");

    public SwatchInput(String stitchType, String colourName, String colourHex) {
        this(stitchType, colourName, colourHex, "worsted", "100% wool");
    }

    public SwatchInput normalized() {
        String stitch = clean(stitchType);
        stitch = ALIASES.getOrDefault(stitch, stitch);
        if (!SUPPORTED_STITCHES.contains(stitch)) {
            throw new IllegalArgumentException("Unsupported stitch_type '" + stitchType
                + "'; choose one of " + String.join(", ", SUPPORTED_STITCHES));
        }
        String hex = colourHex.strip().toUpperCase(Locale.ROOT);
        if (!HEX.matcher(hex).matches()) throw new IllegalArgumentException("colour_hex must have the form #RRGGBB");
        String name = clean(colourName);
        String weight = clean(weightCategory);
        String fibre = clean(fibreContent);
        if (name.isEmpty() || weight.isEmpty() || fibre.isEmpty()) {
            throw new IllegalArgumentException("colour_name, weight_category and fibre_content cannot be blank");
        }
        return new SwatchInput(stitch, name, hex, weight, fibre);
    }

    public Map<String, String> cacheFields() {
        SwatchInput value = normalized();
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("stitch_type", value.stitchType());
        fields.put("colour_name", value.colourName());
        fields.put("colour_hex", value.colourHex());
        fields.put("weight_category", value.weightCategory());
        fields.put("fibre_content", value.fibreContent());
        return fields;
    }

    private static String clean(String value) {
        return value.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
