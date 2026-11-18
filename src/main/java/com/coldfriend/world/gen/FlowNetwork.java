package com.coldfriend.world.gen;

import com.coldfriend.world.model.HeightField;
import com.coldfriend.world.query.SearchKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Works out where water would flow over the terrain, on a coarse grid. Hollows are filled so every cell drains
 * to the map edge, each cell passes its water to one lower neighbour, and the number of cells draining through
 * each one gives the size of a river there.
 */
final class FlowNetwork {

    /** One point on a river's path before it is given a width. */
    record ChainNode(float x, float z, float accumulation, float level) {
    }

    /** Height added per step when filling a hollow, so every filled cell still slopes toward its outlet. */
    private static final float FILL_EPSILON = 0.002f;
    private static final float FILL_JITTER_MIN = 0.5f;
    private static final float FILL_JITTER_RANGE = 4f;
    private static final int JITTER_SHIFT = 40;
    private static final int JITTER_SCALE = 1 << 24;
    private static final int MIN_THRESHOLD = 2;
    private static final int[] DI = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] DJ = {-1, -1, -1, 0, 0, 1, 1, 1};
    private static final long INDEX_MASK = 0xFFFFFFFFL;
    private static final int MIN_CONFLUENCE_DONORS = 2;

    private final int side;
    private final float spacing;
    private final float[] filled;
    private final int[] receiver;
    private final float[] accumulation;

    private FlowNetwork(int side, float spacing, float[] filled, int[] receiver, float[] accumulation) {
        this.side = side;
        this.spacing = spacing;
        this.filled = filled;
        this.receiver = receiver;
        this.accumulation = accumulation;
    }

    /**
     * @param terrain the terrain height field
     * @param stride terrain cells per routing cell; must divide the terrain cell count
     * @return the flow network
     */
    static FlowNetwork build(HeightField terrain, int stride) {
        int side = terrain.cells() / stride + 1;
        int total = side * side;
        float[] ground = new float[total];
        for (int j = 0; j < side; j++) {
            for (int i = 0; i < side; i++) {
                ground[j * side + i] = terrain.get(i * stride, j * stride);
            }
        }
        float[] filled = new float[total];
        int[] receiver = new int[total];
        Arrays.fill(receiver, -1);
        boolean[] seen = new boolean[total];
        int[] order = new int[total];
        int count = 0;
        PriorityQueue<Long> queue = new PriorityQueue<>();
        for (int j = 0; j < side; j++) {
            for (int i = 0; i < side; i++) {
                if (i == 0 || j == 0 || i == side - 1 || j == side - 1) {
                    int index = j * side + i;
                    seen[index] = true;
                    filled[index] = ground[index];
                    queue.add(SearchKey.of(filled[index], index));
                }
            }
        }
        while (!queue.isEmpty()) {
            int index = (int) (queue.poll() & INDEX_MASK);
            order[count++] = index;
            int i = index % side;
            int j = index / side;
            for (int d = 0; d < DI.length; d++) {
                int ni = i + DI[d];
                int nj = j + DJ[d];
                if (ni < 0 || nj < 0 || ni >= side || nj >= side) {
                    continue;
                }
                int next = nj * side + ni;
                if (seen[next]) {
                    continue;
                }
                seen[next] = true;
                // Random step sizes make water wander across flat filled hollows instead of running in straight lines.
                float step = FILL_EPSILON * (FILL_JITTER_MIN + FILL_JITTER_RANGE * jitter(next));
                filled[next] = Math.max(ground[next], filled[index] + step);
                receiver[next] = index;
                queue.add(SearchKey.of(filled[next], next));
            }
        }
        float[] accumulation = new float[total];
        Arrays.fill(accumulation, 1f);
        for (int k = count - 1; k >= 0; k--) {
            int index = order[k];
            if (receiver[index] >= 0) {
                accumulation[receiver[index]] += accumulation[index];
            }
        }
        return new FlowNetwork(side, stride * terrain.cellSize(), filled, receiver, accumulation);
    }

    /**
     * Picks the drainage size above which a cell counts as river, so about {@code targetCells} cells are river.
     *
     * @param targetCells wanted number of river cells
     * @return the threshold
     */
    int chooseThreshold(int targetCells) {
        int low = MIN_THRESHOLD;
        int high = MIN_THRESHOLD;
        for (float a : accumulation) {
            high = Math.max(high, (int) a);
        }
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (countAtLeast(mid) >= targetCells) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    /**
     * Extracts the river network as reaches. A reach runs from a source or a confluence downstream to the next
     * confluence or the map edge, and its last node is the first node of the reach it joins.
     *
     * @param threshold drainage size at which a cell becomes river
     * @return the reaches, each with at least two nodes, upstream to downstream
     */
    List<List<ChainNode>> chains(int threshold) {
        int total = side * side;
        boolean[] river = new boolean[total];
        int[] donors = new int[total];
        for (int index = 0; index < total; index++) {
            river[index] = accumulation[index] >= threshold;
        }
        for (int index = 0; index < total; index++) {
            if (river[index] && receiver[index] >= 0) {
                donors[receiver[index]]++;
            }
        }
        List<List<ChainNode>> chains = new ArrayList<>();
        for (int start = 0; start < total; start++) {
            boolean isStart = river[start] && (donors[start] == 0 || donors[start] >= MIN_CONFLUENCE_DONORS);
            if (!isStart) {
                continue;
            }
            List<ChainNode> chain = new ArrayList<>();
            chain.add(node(start));
            int current = start;
            while (receiver[current] >= 0) {
                int next = receiver[current];
                chain.add(node(next));
                if (donors[next] >= MIN_CONFLUENCE_DONORS) {
                    break;
                }
                current = next;
            }
            if (chain.size() >= 2) {
                chains.add(chain);
            }
        }
        return chains;
    }

    private int countAtLeast(int threshold) {
        int count = 0;
        for (float a : accumulation) {
            if (a >= threshold) {
                count++;
            }
        }
        return count;
    }

    private ChainNode node(int index) {
        return new ChainNode((index % side) * spacing, (index / side) * spacing, accumulation[index], filled[index]);
    }

    /** @return a fixed pseudo-random value in [0, 1) for a cell, the same on every machine */
    private static float jitter(int index) {
        return (Mixer.mix(index) >>> JITTER_SHIFT) / (float) JITTER_SCALE;
    }

}
