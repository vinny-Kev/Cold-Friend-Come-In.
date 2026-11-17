package com.coldfriend.world.gen;

import com.coldfriend.world.gen.FlowNetwork.ChainNode;
import com.coldfriend.world.model.HeightField;
import com.coldfriend.world.model.River;
import com.coldfriend.world.model.River.RiverPoint;
import com.coldfriend.world.model.WaterMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generates rivers from the terrain. Water is routed downhill over a coarse grid, the busiest paths become
 * rivers, and each is smoothed into bends, given a width that grows with the area it drains, and cut into the
 * terrain as a channel with banks and a valley.
 */
final class HydrologyStage {

    /** The result: river reaches and the water map. The terrain is carved in place. */
    record Result(List<River> rivers, WaterMap water) {
    }

    private static final int SMOOTHING_ROUNDS = 2;
    private static final float CHAIKIN_NEAR = 0.25f;
    private static final float CHAIKIN_FAR = 0.75f;
    private static final float WIDTH_VARIATION = 0.25f;
    private static final float WIDTH_WAVELENGTH = 45f;
    private static final float BANK_WAVELENGTH = 90f;
    private static final float DEPTH_BASE = 0.35f;
    private static final float DEPTH_PER_HALF_WIDTH = 0.1f;
    private static final float DEPTH_MAX = 1.8f;
    private static final float VALLEY_FADE_START = 0.55f;
    private static final float DISTANCE_CAP = 200f;
    private static final float HALF = 0.5f;
    private static final float MEANDER_AMPLITUDE = 14f;
    private static final float MEANDER_WAVELENGTH = 70f;
    private static final float MEANDER_SHIFT = 53.1f;
    private static final int MEANDER_OCTAVES = 2;

    private HydrologyStage() {
    }

    static Result generate(HeightField terrain, Geography geography, WorldGenParameters p, RandomStreams streams) {
        FlowNetwork network = FlowNetwork.build(terrain, p.hydrologyStride());
        int threshold = network.chooseThreshold(geography.riverTargetCells());
        Noise2D widthNoise = new Noise2D(streams.seedFor(RandomStreams.HYDROLOGY + ".width"));
        Noise2D bankNoise = new Noise2D(streams.seedFor(RandomStreams.HYDROLOGY + ".banks"));
        Noise2D meanderNoise = new Noise2D(streams.seedFor(RandomStreams.HYDROLOGY + ".meander"));

        List<River> rivers = new ArrayList<>();
        for (List<ChainNode> chain : network.chains(threshold)) {
            List<ChainNode> smooth = chain;
            for (int round = 0; round < SMOOTHING_ROUNDS; round++) {
                smooth = chaikin(smooth);
            }
            List<RiverPoint> points = new ArrayList<>(smooth.size());
            for (ChainNode node : smooth) {
                ChainNode bent = meander(node, terrain.sizeMeters(), p.hydrologyStride() * terrain.cellSize(),
                        meanderNoise);
                points.add(new RiverPoint(bent.x(), bent.z(), bent.level(), halfWidth(bent, p, widthNoise)));
            }
            rivers.add(new River(points));
        }
        WaterMap water = carve(terrain, rivers, p, bankNoise);
        return new Result(rivers, water);
    }

    /**
     * Sideways wobble that depends only on position, so reaches that meet at a confluence stay joined. It
     * fades out near the map edge so rivers still reach it.
     */
    private static ChainNode meander(ChainNode node, float size, float edgeFade, Noise2D noise) {
        float edge = Math.min(Math.min(node.x(), size - node.x()), Math.min(node.z(), size - node.z()));
        float strength = MEANDER_AMPLITUDE * Maths.smoothstep(0f, edgeFade * 2f, edge);
        float dx = noise.fbm(node.x() / MEANDER_WAVELENGTH, node.z() / MEANDER_WAVELENGTH, MEANDER_OCTAVES);
        float dz = noise.fbm(node.x() / MEANDER_WAVELENGTH + MEANDER_SHIFT, node.z() / MEANDER_WAVELENGTH,
                MEANDER_OCTAVES);
        return new ChainNode(node.x() + dx * strength, node.z() + dz * strength, node.accumulation(), node.level());
    }

    private static float halfWidth(ChainNode node, WorldGenParameters p, Noise2D widthNoise) {
        float base = p.riverHalfWidthMin() + p.riverHalfWidthScale() * (float) Math.sqrt(node.accumulation());
        float variation = 1f + WIDTH_VARIATION * widthNoise.noise(node.x() / WIDTH_WAVELENGTH, node.z() / WIDTH_WAVELENGTH);
        return Maths.clamp(base * variation, p.riverHalfWidthMin(), p.riverHalfWidthMax());
    }

    /** Corner-cutting subdivision: rounds the jagged grid path into bends and keeps both end points fixed. */
    private static List<ChainNode> chaikin(List<ChainNode> in) {
        List<ChainNode> out = new ArrayList<>(in.size() * 2);
        out.add(in.get(0));
        for (int k = 0; k + 1 < in.size(); k++) {
            out.add(blend(in.get(k), in.get(k + 1), CHAIKIN_NEAR));
            out.add(blend(in.get(k), in.get(k + 1), CHAIKIN_FAR));
        }
        out.add(in.get(in.size() - 1));
        return out;
    }

    private static ChainNode blend(ChainNode a, ChainNode b, float t) {
        return new ChainNode(
                Maths.lerp(a.x(), b.x(), t),
                Maths.lerp(a.z(), b.z(), t),
                Maths.lerp(a.accumulation(), b.accumulation(), t),
                Maths.lerp(a.level(), b.level(), t));
    }

    private static WaterMap carve(HeightField terrain, List<River> rivers, WorldGenParameters p, Noise2D bankNoise) {
        int cells = terrain.cells();
        int vertices = cells + 1;
        float cellSize = terrain.cellSize();
        float[] bestDistance = new float[vertices * vertices];
        float[] bestLevel = new float[vertices * vertices];
        float[] bestHalfWidth = new float[vertices * vertices];
        Arrays.fill(bestDistance, Float.MAX_VALUE);

        for (River river : rivers) {
            List<RiverPoint> pts = river.points();
            for (int k = 0; k + 1 < pts.size(); k++) {
                markSegment(pts.get(k), pts.get(k + 1), p, terrain, bestDistance, bestLevel, bestHalfWidth);
            }
        }

        float[] surface = new float[vertices * vertices];
        float[] edgeDistance = new float[vertices * vertices];
        for (int j = 0; j < vertices; j++) {
            for (int i = 0; i < vertices; i++) {
                int index = j * vertices + i;
                surface[index] = Float.NEGATIVE_INFINITY;
                edgeDistance[index] = DISTANCE_CAP;
                float d = bestDistance[index];
                if (d == Float.MAX_VALUE) {
                    continue;
                }
                float hw = bestHalfWidth[index];
                float level = bestLevel[index];
                edgeDistance[index] = Math.min(DISTANCE_CAP, Math.max(0f, d - hw));
                float reach = hw + p.riverValleyExtra();
                if (d > reach) {
                    continue;
                }
                float x = i * cellSize;
                float z = j * cellSize;
                float target;
                if (d <= hw) {
                    float t = d / hw;
                    float depth = Math.min(DEPTH_MAX, DEPTH_BASE + DEPTH_PER_HALF_WIDTH * hw);
                    target = level - depth * (1f - t * t);
                    surface[index] = level;
                } else {
                    float slope = Maths.lerp(p.riverBankSlopeMin(), p.riverBankSlopeMax(),
                            HALF + HALF * bankNoise.noise(x / BANK_WAVELENGTH, z / BANK_WAVELENGTH));
                    target = level + (d - hw) * slope;
                }
                float weight = 1f - Maths.smoothstep(VALLEY_FADE_START * reach, reach, d);
                float ground = terrain.get(i, j);
                terrain.set(i, j, ground + (Math.min(ground, target) - ground) * weight);
            }
        }
        return new WaterMap(cells, cellSize, surface, edgeDistance);
    }

    private static void markSegment(RiverPoint a, RiverPoint b, WorldGenParameters p, HeightField terrain,
            float[] bestDistance, float[] bestLevel, float[] bestHalfWidth) {
        int vertices = terrain.vertices();
        float cellSize = terrain.cellSize();
        float reach = Math.max(a.halfWidth(), b.halfWidth()) + p.riverValleyExtra();
        int minI = Math.max(0, (int) Math.floor((Math.min(a.x(), b.x()) - reach) / cellSize));
        int maxI = Math.min(terrain.cells(), (int) Math.ceil((Math.max(a.x(), b.x()) + reach) / cellSize));
        int minJ = Math.max(0, (int) Math.floor((Math.min(a.z(), b.z()) - reach) / cellSize));
        int maxJ = Math.min(terrain.cells(), (int) Math.ceil((Math.max(a.z(), b.z()) + reach) / cellSize));
        float sx = b.x() - a.x();
        float sz = b.z() - a.z();
        float lengthSquared = sx * sx + sz * sz;
        for (int j = minJ; j <= maxJ; j++) {
            for (int i = minI; i <= maxI; i++) {
                float px = i * cellSize - a.x();
                float pz = j * cellSize - a.z();
                float t = lengthSquared == 0f ? 0f : Maths.clamp((px * sx + pz * sz) / lengthSquared, 0f, 1f);
                float dx = px - sx * t;
                float dz = pz - sz * t;
                float d = (float) Math.sqrt(dx * dx + dz * dz);
                int index = j * vertices + i;
                if (d < bestDistance[index]) {
                    bestDistance[index] = d;
                    bestLevel[index] = Maths.lerp(a.level(), b.level(), t);
                    bestHalfWidth[index] = Maths.lerp(a.halfWidth(), b.halfWidth(), t);
                }
            }
        }
    }
}
