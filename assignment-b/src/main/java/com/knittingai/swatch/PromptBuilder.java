package com.knittingai.swatch;

import java.util.Map;

public final class PromptBuilder {
    private static final Map<String, String> STRUCTURES = Map.of(
        "stockinette", "smooth uniform interlocking \"V\" shapes stacked in neat vertical columns; no horizontal ridges or cable crossings",
        "garter", "pronounced horizontal rows of rounded purl bumps and ridges; no visible \"V\" columns",
        "rib", "alternating vertical columns of raised knit stitches and deeply recessed purl stitches, forming regular corrugated ridges",
        "seed", "dense all-over alternating knit and purl bumps, an irregular checkerboard-like surface with no directional lines",
        "cable", "thick raised rope-like knitted braids visibly twisting and crossing over a flatter stockinette background, with strong three-dimensional relief and shadows",
        "lace", "small regularly spaced open eyelet holes formed by yarn overs, arranged in a delicate airy single-colour knitted fabric"
    );
    private static final Map<String, String> WEIGHTS = Map.of(
        "lace", "very fine yarn and tiny delicate stitches",
        "fingering", "fine yarn and small stitches",
        "sport", "light yarn and moderately small stitches",
        "dk", "medium-light yarn and clearly defined stitches",
        "worsted", "medium yarn and well-defined moderately chunky stitches",
        "aran", "thick yarn and chunky stitches",
        "bulky", "very thick yarn and large chunky stitches",
        "super bulky", "extremely thick yarn and oversized stitches"
    );

    private PromptBuilder() {}

    public static String buildPrompt(SwatchInput input) {
        SwatchInput item = input.normalized();
        String weight = WEIGHTS.getOrDefault(item.weightCategory(),
            item.weightCategory() + " yarn with realistic stitch scale");
        return "Create a square, close-up, photorealistic hero photograph of a hand-knitted "
            + "10 x 10 cm fabric swatch filling the frame. STITCH (highest priority): "
            + item.stitchType() + "; show " + STRUCTURES.get(item.stitchType()) + ". "
            + "COLOUR (exact, highest priority): use uniform " + item.colourHex()
            + " throughout the yarn; the label '" + item.colourName()
            + "' is secondary and must not override the hex value. YARN: " + weight
            + "; fibre is " + item.fibreContent()
            + ", with physically accurate twist, sheen and fuzz. Soft neutral studio lighting may create "
            + "natural shadows but must not shift the base yarn hue. Macro textile photography, sharp stitch "
            + "definition, seamless edge-to-edge fabric, no needles, hands, labels, text, garments, objects, "
            + "multiple colours, borders or watermark.";
    }
}
