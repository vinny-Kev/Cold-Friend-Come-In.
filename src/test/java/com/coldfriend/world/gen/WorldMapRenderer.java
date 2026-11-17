package com.coldfriend.world.gen;

import com.coldfriend.assets.PropKind;
import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.LocationKind;
import com.coldfriend.world.model.Placement;
import com.coldfriend.world.model.Region;
import com.coldfriend.world.model.VillageBuilding;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.model.WorldLocation;
import com.coldfriend.world.model.WorldPath;
import com.coldfriend.world.model.WorldPath.PathPoint;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Draws a top-down map of a generated world, for checking worlds by eye. One pixel is one terrain cell. */
final class WorldMapRenderer {

    private static final float SUN_X = -0.5f;
    private static final float SUN_Z = -0.6f;
    private static final float SHADE_SCALE = 1.4f;
    private static final int WATER = rgb(0.15f, 0.35f, 0.75f);
    private static final int PATH = rgb(0.55f, 0.40f, 0.25f);
    private static final int CABIN = rgb(0.95f, 0.1f, 0.1f);
    private static final int BUILDING = rgb(1f, 0.85f, 0.1f);
    private static final int WATER_ACCESS = rgb(0.1f, 0.9f, 0.9f);
    private static final int TREE = rgb(0.05f, 0.18f, 0.08f);
    private static final int ROCK = rgb(0.35f, 0.35f, 0.38f);

    private WorldMapRenderer() {
    }

    static void write(WorldData world, Path file) throws IOException {
        int n = world.terrain().cells();
        float cell = world.terrain().cellSize();
        BufferedImage image = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
        float low = world.terrain().min();
        float range = world.terrain().max() - low;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                float[] base = regionColor(world.regions().regionAt(i * cell, j * cell));
                float elevation = (world.terrain().get(i, j) - low) / range;
                float dx = world.terrain().get(Math.min(n, i + 1), j) - world.terrain().get(Math.max(0, i - 1), j);
                float dz = world.terrain().get(i, Math.min(n, j + 1)) - world.terrain().get(i, Math.max(0, j - 1));
                float shade = 0.75f + SHADE_SCALE * 0.1f * (dx * SUN_X + dz * SUN_Z) / cell;
                shade = Math.max(0.35f, Math.min(1.25f, shade)) * (0.8f + 0.3f * elevation);
                image.setRGB(i, j, rgb(base[0] * shade, base[1] * shade, base[2] * shade));
            }
        }
        for (Placement p : world.placements()) {
            if (p.kind() == PropKind.CONIFER || p.kind() == PropKind.BARE_TREE || p.kind() == PropKind.DEAD_TREE) {
                plot(image, world, p.x(), p.z(), TREE, 0);
            } else if (p.kind() == PropKind.ROCK) {
                plot(image, world, p.x(), p.z(), ROCK, 0);
            }
        }
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                if (world.water().surface(i, j) > Float.NEGATIVE_INFINITY) {
                    image.setRGB(i, j, WATER);
                }
            }
        }
        for (WorldPath path : world.paths()) {
            for (int k = 0; k + 1 < path.points().size(); k++) {
                line(image, world, path.points().get(k), path.points().get(k + 1));
            }
        }
        for (VillageBuilding b : world.village().buildings()) {
            box(image, world, b.footprint(), BUILDING);
        }
        box(image, world, world.cabin().footprint(), CABIN);
        for (WorldLocation location : world.locations()) {
            if (location.kind() == LocationKind.WATER_ACCESS) {
                plot(image, world, location.x(), location.z(), WATER_ACCESS, 2);
            }
        }
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "png", file.toFile());
    }

    private static void box(BufferedImage image, WorldData world, Footprint f, int color) {
        for (float s = -f.halfWidth(); s <= f.halfWidth(); s += 1f) {
            for (float t = -f.halfDepth(); t <= f.halfDepth(); t += 1f) {
                float[] p = f.toWorld(s, t);
                plot(image, world, p[0], p[1], color, 0);
            }
        }
    }

    private static void line(BufferedImage image, WorldData world, PathPoint a, PathPoint b) {
        int steps = Math.max(1, (int) (Math.hypot(b.x() - a.x(), b.z() - a.z()) / world.terrain().cellSize()));
        for (int s = 0; s <= steps; s++) {
            float t = (float) s / steps;
            plot(image, world, a.x() + (b.x() - a.x()) * t, a.z() + (b.z() - a.z()) * t, PATH, 0);
        }
    }

    private static void plot(BufferedImage image, WorldData world, float x, float z, int color, int radius) {
        int ci = Math.round(x / world.terrain().cellSize());
        int cj = Math.round(z / world.terrain().cellSize());
        for (int dj = -radius; dj <= radius; dj++) {
            for (int di = -radius; di <= radius; di++) {
                int i = ci + di;
                int j = cj + dj;
                if (i >= 0 && j >= 0 && i < image.getWidth() && j < image.getHeight()) {
                    image.setRGB(i, j, color);
                }
            }
        }
    }

    private static float[] regionColor(Region region) {
        return switch (region) {
            case CONIFER_FOREST -> new float[] {0.30f, 0.50f, 0.36f};
            case OPEN_WOODLAND -> new float[] {0.62f, 0.72f, 0.62f};
            case MOUNTAIN_FOREST -> new float[] {0.52f, 0.52f, 0.55f};
            case RIVER_VALLEY -> new float[] {0.45f, 0.66f, 0.66f};
            case CLEARING -> new float[] {0.92f, 0.94f, 0.96f};
        };
    }

    private static int rgb(float r, float g, float b) {
        return (channel(r) << 16) | (channel(g) << 8) | channel(b);
    }

    private static int channel(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255f)));
    }
}
