package com.knittingai.swatch;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public final class PlaceholderRenderer {
    private PlaceholderRenderer() {}

    public static void create(SwatchInput input, Path outputPath) throws IOException {
        create(input, outputPath, 512);
    }

    public static void create(SwatchInput input, Path outputPath, int size) throws IOException {
        SwatchInput item = input.normalized();
        Color base = Color.decode(item.colourHex());
        Color dark = shade(base, .58);
        Color light = shade(base, 1.35);
        int step = Math.max(18, size / 16);
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(base);
        graphics.fillRect(0, 0, size, size);
        switch (item.stitchType()) {
            case "stockinette" -> drawStockinette(graphics, size, step, dark, light);
            case "garter" -> drawGarter(graphics, size, step, dark, light);
            case "rib" -> drawRib(graphics, size, step, dark, light);
            case "seed" -> drawSeed(graphics, size, step, dark, light);
            case "cable" -> drawCable(graphics, size, step, dark, light);
            case "lace" -> drawLace(graphics, size, step, dark, light);
            default -> throw new IllegalStateException("Unexpected stitch " + item.stitchType());
        }
        graphics.dispose();
        ImageIO.write(image, "PNG", outputPath.toFile());
    }

    private static Color shade(Color colour, double factor) {
        return new Color((int) Math.max(0, Math.min(255, Math.round(colour.getRed() * factor))),
            (int) Math.max(0, Math.min(255, Math.round(colour.getGreen() * factor))),
            (int) Math.max(0, Math.min(255, Math.round(colour.getBlue() * factor))));
    }

    private static void drawStockinette(Graphics2D g, int size, int step, Color dark, Color light) {
        g.setStroke(new BasicStroke(Math.max(2, step / 7f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int y = -step; y < size + step; y += step) for (int x = 0; x < size + step; x += step) {
            g.setColor(light); g.drawPolyline(new int[]{x-step/3, x, x+step/3}, new int[]{y, y+step/2, y}, 3);
            g.setColor(dark); g.drawLine(x, y+step/2, x, y+step);
        }
    }

    private static void drawGarter(Graphics2D g, int size, int step, Color dark, Color light) {
        for (int y = step/2; y < size; y += step) {
            g.setColor(dark); g.setStroke(new BasicStroke(Math.max(3, step/4f))); g.drawLine(0, y, size, y);
            g.setColor(light); g.setStroke(new BasicStroke(Math.max(2, step/7f))); g.drawLine(0, y-4, size, y-4);
        }
    }

    private static void drawRib(Graphics2D g, int size, int step, Color dark, Color light) {
        for (int x = 0; x < size; x += step*2) {
            g.setColor(light); g.fill(new RoundRectangle2D.Double(x, 0, step, size, step, step));
            g.setColor(dark); g.setStroke(new BasicStroke(step/3f)); g.drawLine(x+step+3, 0, x+step+3, size);
        }
    }

    private static void drawSeed(Graphics2D g, int size, int step, Color dark, Color light) {
        int radius = step/4, row = 0;
        for (int y = step/2; y < size; y += step, row++) {
            int col = 0;
            for (int x = step/2; x < size; x += step, col++) {
                g.setColor((row+col)%2 == 0 ? dark : light);
                g.fill(new Ellipse2D.Double(x-radius, y-radius, radius*2, radius*2));
            }
        }
    }

    private static void drawCable(Graphics2D g, int size, int step, Color dark, Color light) {
        for (int center = step*2; center < size; center += step*5) {
            for (int side : new int[]{-1, 1}) {
                java.awt.geom.Path2D path = new java.awt.geom.Path2D.Double();
                boolean first = true;
                for (int y = -step; y < size+step; y += 4) {
                    double x = center + side * step * .8 * Math.sin((double)y / step);
                    if (first) { path.moveTo(x, y); first = false; } else path.lineTo(x, y);
                }
                g.setColor(dark); g.setStroke(new BasicStroke(step, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(path);
                g.setColor(light); g.setStroke(new BasicStroke(Math.max(4, step/2f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(path);
            }
        }
    }

    private static void drawLace(Graphics2D g, int size, int step, Color dark, Color light) {
        int radius = step/4, row = 0;
        for (int y = step; y < size; y += step*2, row++) for (int x = step+(row%2)*step; x < size; x += step*2) {
            g.setColor(dark); g.fillOval(x-radius, y-radius, radius*2, radius*2);
            g.setColor(light); g.setStroke(new BasicStroke(3)); g.drawArc(x-step, y-step, step*2, step*2, 200, 140);
        }
    }
}
