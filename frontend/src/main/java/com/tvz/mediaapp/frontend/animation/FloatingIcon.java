package com.tvz.mediaapp.frontend.animation;

import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FloatingIcon {
    private static final Logger logger = LoggerFactory.getLogger(FloatingIcon.class);
    private static final Random rand = new Random();

    private static final double VIEWBOX_SIZE = 24.0;

    private static final List<Color> PALETTE = List.of(
            Color.web("#FFFFFF"),
            Color.web("#AECBFA"),
            Color.web("#8AB4F8"),
            Color.web("#BB86FC"),
            Color.web("#03DAC6"),
            Color.web("#F1F3F4")
    );

    private static final Map<String, String> ICON_PATHS = Map.of(
            "facebook", "/icons/facebook.svg",
            "instagram", "/icons/instagram.svg",
            "tiktok", "/icons/tiktok.svg",
            "x", "/icons/x.svg",
            "youtube", "/icons/youtube.svg"
    );

    private double x;
    private double y;
    private final double fallSpeed;
    private double driftSpeed;
    private final double size;
    private final double opacity;
    private double rotation;
    private final double rotationSpeed;

    private final WritableImage cachedImage;

    public FloatingIcon(double startX, double startY) {
        this.x = startX;
        this.y = startY;
        this.fallSpeed = rand.nextDouble() * 1.5 + 0.5;
        this.driftSpeed = rand.nextDouble() - 0.5;
        this.size = rand.nextDouble() * 15 + 25;
        this.opacity = rand.nextDouble() * 0.25 + 0.65;
        this.rotation = rand.nextDouble() * 360;
        this.rotationSpeed = rand.nextDouble() * 1.2 - 0.6;

        List<String> keys = new ArrayList<>(ICON_PATHS.keySet());
        String key = keys.get(rand.nextInt(keys.size()));
        String svgFile = ICON_PATHS.get(key);

        List<String> paths = loadAllSvgPaths(svgFile);
        List<String> foregroundPaths = filterBackgroundPaths(paths);

        Color tintBase = PALETTE.get(rand.nextInt(PALETTE.size()));
        Color tint = tintBase.deriveColor(0, 1, 1, 1.0);
        Color glowColor = tintBase.deriveColor(0, 1, 1, 0.25);

        this.cachedImage = renderSnapshot(foregroundPaths, size, tint, glowColor);
    }

    public void update() {
        y += fallSpeed;
        x += driftSpeed;
        rotation += rotationSpeed;

        if (rotation > 360) rotation -= 360;
        if (rotation < 0) rotation += 360;
    }

    public void draw(GraphicsContext gc) {
        if (cachedImage == null) return;

        gc.save();
        double centerX = x + size / 2.0;
        double centerY = y + size / 2.0;

        gc.translate(centerX, centerY);
        gc.rotate(rotation);
        gc.setGlobalAlpha(opacity);

        gc.drawImage(cachedImage, -cachedImage.getWidth() / 2.0, -cachedImage.getHeight() / 2.0);
        gc.restore();
    }

    public boolean isOffScreen(int canvasWidth, int canvasHeight) {
        if (x <= 0 || x >= canvasWidth - size) {
            driftSpeed = -driftSpeed;
            x = Math.max(0, Math.min(x, canvasWidth - size));
        }
        return y > canvasHeight + size;
    }

    private List<String> loadAllSvgPaths(String svgFilePath) {
        try (InputStream in = getClass().getResourceAsStream(svgFilePath)) {
            if (in == null) {
                logger.warn("Could not find SVG file: {}", svgFilePath);
                return List.of(createFallbackPath());
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            StringBuilder svg = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) svg.append(line);

            return extractAllPaths(svg.toString());
        } catch (Exception e) {
            logger.error("Error loading SVG file: {}", svgFilePath, e);
            return List.of(createFallbackPath());
        }
    }

    private static List<String> extractAllPaths(String svgContent) {
        Pattern p = Pattern.compile("<path[^>]*\\sd=[\"']([^\"']+)[\"'][^>]*>");
        Matcher m = p.matcher(svgContent);
        List<String> list = new ArrayList<>();
        while (m.find()) list.add(m.group(1));
        return list;
    }

    private List<String> filterBackgroundPaths(List<String> paths) {
        if (paths.isEmpty()) return List.of(createFallbackPath());

        List<String> filtered = new ArrayList<>();
        for (String d : paths) {
            if (!isLikelyBackgroundCircle(d)) {
                filtered.add(d);
            }
        }
        if (filtered.isEmpty()) {
            paths.sort(Comparator.comparingInt(String::length).reversed());
            filtered.add(paths.get(0));
        }
        return filtered;
    }

    private boolean isLikelyBackgroundCircle(String d) {
        String normalized = d.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        boolean hasClose = normalized.contains("Z");
        boolean hasArc = normalized.contains("A");
        boolean bigRadius = false;

        Matcher m = Pattern.compile("A\\s*([0-9]*\\.?[0-9]+)\\s*,?\\s*([0-9]*\\.?[0-9]+)").matcher(normalized);
        while (m.find()) {
            try {
                double rx = Double.parseDouble(m.group(1));
                double ry = Double.parseDouble(m.group(2));
                if (rx >= 10.0 || ry >= 10.0) {
                    bigRadius = true;
                    break;
                }
            } catch (NumberFormatException ignore) {
            }
        }

        if (hasArc && hasClose && bigRadius) return true;

        return normalized.length() < 80 && hasArc;
    }

    private WritableImage renderSnapshot(List<String> pathList, double targetSize, Color tint, Color glowColor) {
        try {
            Group group = new Group();
            for (String d : pathList) {
                SVGPath p = new SVGPath();
                p.setContent(d);
                p.setFill(tint);
                p.setStroke(null);
                group.getChildren().add(p);
            }

            double scale = targetSize / VIEWBOX_SIZE;
            group.setScaleX(scale);
            group.setScaleY(scale);

            DropShadow ds = new DropShadow();
            ds.setRadius(Math.max(2.0, targetSize * 0.08));
            ds.setSpread(0.08);
            ds.setColor(glowColor);
            group.setEffect(ds);

            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);

            return group.snapshot(params, null);
        } catch (Exception e) {
            logger.debug("Vector snapshot failed, using fallback circle", e);
            int sz = (int) Math.ceil(targetSize);
            WritableImage img = new WritableImage(sz, sz);
            javafx.scene.canvas.Canvas c = new javafx.scene.canvas.Canvas(sz, sz);
            var g = c.getGraphicsContext2D();
            g.setFill(tint);
            g.fillOval(0, 0, sz, sz);
            SnapshotParameters p = new SnapshotParameters();
            p.setFill(Color.TRANSPARENT);
            return c.snapshot(p, img);
        }
    }

    private String createFallbackPath() {
        return "M12 2A10 10 0 1 1 2 12A10 10 0 0 1 12 2Z";
    }
}