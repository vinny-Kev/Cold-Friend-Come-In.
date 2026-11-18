package com.coldfriend.world.query;

/** Packs a priority and a node index into one long, so a priority queue of longs can stand in for objects. */
public final class SearchKey {

    private static final int KEY_SHIFT = 32;
    private static final int SORT_FLIP = Integer.MAX_VALUE;
    private static final int SIGN_SHIFT = 31;

    private SearchKey() {
    }

    /**
     * @param priority the value to sort by, lowest first; negative values sort correctly too
     * @param index the node; ties in priority are broken by it, so results do not depend on queue internals
     * @return one long that sorts by priority, then by index
     */
    public static long of(float priority, int index) {
        int bits = Float.floatToIntBits(priority);
        int sortable = bits ^ ((bits >> SIGN_SHIFT) & SORT_FLIP);
        return ((long) sortable << KEY_SHIFT) | index;
    }
}
