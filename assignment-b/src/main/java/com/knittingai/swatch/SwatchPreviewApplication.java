package com.knittingai.swatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

public class SwatchPreviewApplication {
    private record Entry(String label, Path path) {}

    public static void main(String[] args) throws IOException {
        boolean live = java.util.Arrays.asList(args).contains("--live");
        Path output = Path.of("artifacts");
        for (int index = 0; index < args.length - 1; index++) {
            if ("--output".equals(args[index])) output = Path.of(args[index + 1]);
        }
        Map<String, Object> report = runDemo(output, live);
        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(report));
    }

    public static Map<String, Object> runDemo(Path output, boolean live) throws IOException {
        Files.createDirectories(output);
        SwatchGenerator generator = new SwatchGenerator(
            live ? new OpenAiImageProvider() : new FailingProvider(), output.resolve("cache"), output.resolve("work"));
        List<Entry> colourEntries = new ArrayList<>();
        List<Double> timings = new ArrayList<>();
        String[][] colours = {{"royal purple", "#5B3E96"}, {"ocean blue", "#167D9A"}, {"warm coral", "#D65A4A"}};
        SwatchInput first = null;
        boolean fallbackUsed = false;
        for (int index = 0; index < colours.length; index++) {
            SwatchInput value = new SwatchInput("cable", colours[index][0], colours[index][1]);
            if (first == null) first = value;
            var result = generator.generate(value);
            fallbackUsed |= result.usedFallback();
            timings.add(round6(result.elapsedSeconds()));
            Path copied = output.resolve("colour-" + (index + 1) + ".png");
            Files.copy(result.imagePath(), copied, StandardCopyOption.REPLACE_EXISTING);
            colourEntries.add(new Entry(colours[index][0] + " " + colours[index][1], copied));
        }
        contactSheet(colourEntries, output.resolve("colour-switching.png"), "One cable structure, three colours");
        List<Entry> stitchEntries = new ArrayList<>();
        for (String stitch : List.of("stockinette", "rib", "cable")) {
            var result = generator.generate(new SwatchInput(stitch, "royal purple", "#5B3E96"));
            Path copied = output.resolve("stitch-" + stitch + ".png");
            Files.copy(result.imagePath(), copied, StandardCopyOption.REPLACE_EXISTING);
            stitchEntries.add(new Entry(stitch, copied));
        }
        contactSheet(stitchEntries, output.resolve("stitch-variation.png"), "Three structures, one colour #5B3E96");
        var cacheResult = generator.generate(first);
        var ravelry = new RavelryClient().findReference("cable", output);
        List<Entry> comparison = new ArrayList<>(List.of(new Entry("generated cable", colourEntries.getFirst().path())));
        if (ravelry.imagePath() != null) comparison.add(new Entry(ravelry.patternName(), ravelry.imagePath()));
        contactSheet(comparison, output.resolve("ravelry-comparison.png"), ravelry.message());
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("mode", live ? "live OpenAI-compatible provider" : "offline simulated failure");
        report.put("simulated_provider_failure", !live);
        report.put("fallback_used", fallbackUsed);
        report.put("cache_hit_demonstrated", cacheResult.cacheHit());
        report.put("cache_hit_seconds", round6(cacheResult.elapsedSeconds()));
        report.put("colour_generation_seconds", timings);
        report.put("interaction_note", "Provider generation is too slow for direct colour taps; show the exact-colour "
            + "placeholder immediately, pre-generate common combinations, and replace it asynchronously with the cached AI image.");
        report.put("ravelry", Map.of("matched", ravelry.matched(), "pattern_name",
            ravelry.patternName() == null ? "" : ravelry.patternName(), "message", ravelry.message()));
        report.put("artifacts", List.of("gallery.html", "colour-switching.png", "stitch-variation.png",
            "ravelry-comparison.png", "demo-report.json"));
        report.put("gallery", output.resolve("gallery.html").toString());
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.resolve("demo-report.json").toFile(), report);
        writeGallery(output, report);
        return report;
    }

    private static void writeGallery(Path output, Map<String, Object> report) throws IOException {
        List<String> images = new ArrayList<>();
        try (var files = Files.list(output)) {
            files.map(path -> path.getFileName().toString())
                .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".png"))
                .sorted()
                .forEach(images::add);
        }
        StringBuilder heroes = new StringBuilder();
        StringBuilder grid = new StringBuilder();
        for (String name : images) {
            boolean hero = name.contains("switching") || name.contains("variation") || name.contains("comparison");
            String figure = "<figure><img src=\"" + escapeHtml(name) + "\" alt=\"" + escapeHtml(name)
                + "\"><figcaption>" + escapeHtml(name) + "</figcaption></figure>";
            (hero ? heroes : grid).append(figure);
        }
        String html = "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
            + "<title>Knit Swatch Preview Gallery</title><style>"
            + "body{margin:0;padding:24px;background:#12121a;color:#eee;font-family:system-ui,Segoe UI,Arial,sans-serif}"
            + "h1{font-size:20px}h2{font-size:16px;color:#bbb;margin-top:28px}"
            + "figure{margin:0 0 24px}figcaption{margin-top:8px;color:#9a9aa8;font-size:13px}"
            + "img{max-width:100%;height:auto;border-radius:8px;background:#fff;cursor:zoom-in}"
            + ".meta{background:#1d1d2b;padding:12px 16px;border-radius:8px;margin-bottom:8px;font-size:14px;line-height:1.7}"
            + ".grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:16px}"
            + "</style></head><body>"
            + "<h1>Knit Swatch Preview</h1>"
            + "<div class=\"meta\">Mode: " + escapeHtml(String.valueOf(report.get("mode")))
            + "<br>Provider failure simulated: " + report.get("simulated_provider_failure")
            + "<br>Fallback used: " + report.get("fallback_used")
            + "<br>Cache hit demonstrated: " + report.get("cache_hit_demonstrated")
            + " (" + report.get("cache_hit_seconds") + " s)</div>"
            + "<h2>Hero previews (side by side)</h2>" + heroes
            + "<h2>Individual swatches</h2><div class=\"grid\">" + grid + "</div>"
            + "</body></html>";
        Files.writeString(output.resolve("gallery.html"), html, StandardCharsets.UTF_8);
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static void contactSheet(List<Entry> entries, Path destination, String title) throws IOException {
        int cell = 320, heading = 58;
        BufferedImage canvas = new BufferedImage(cell * entries.size(), cell + heading, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        graphics.setColor(Color.BLACK); graphics.drawString(title, 10, 18);
        for (int index = 0; index < entries.size(); index++) {
            Entry entry = entries.get(index);
            BufferedImage source = ImageIO.read(entry.path().toFile());
            int imageSize = cell - 12;
            graphics.drawImage(source, index * cell + 6, heading, imageSize, imageSize, null);
            graphics.drawString(entry.label(), index * cell + 10, 42);
        }
        graphics.dispose();
        ImageIO.write(canvas, "PNG", destination.toFile());
    }

    private static double round6(double value) {
        return Math.round(value * 1_000_000.0) / 1_000_000.0;
    }
}
