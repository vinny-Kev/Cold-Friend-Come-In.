package com.coldfriend.world.gen;

/** SplitMix64 finaliser: a fast 64-bit hash that gives the same result on every platform. */
final class Mixer {

    private static final long GOLDEN = 0x9E3779B97F4A7C15L;
    private static final long MULTIPLY_A = 0xBF58476D1CE4E5B9L;
    private static final long MULTIPLY_B = 0x94D049BB133111EBL;
    private static final int SHIFT_A = 30;
    private static final int SHIFT_B = 27;
    private static final int SHIFT_C = 31;

    private Mixer() {
    }

    static long mix(long value) {
        long z = value + GOLDEN;
        z = (z ^ (z >>> SHIFT_A)) * MULTIPLY_A;
        z = (z ^ (z >>> SHIFT_B)) * MULTIPLY_B;
        return z ^ (z >>> SHIFT_C);
    }

    /** Hashes a name into a 64-bit value, so stage names can be mixed into seeds. */
    static long hash(String text) {
        long h = 0;
        for (int i = 0; i < text.length(); i++) {
            h = mix(h ^ text.charAt(i));
        }
        return h;
    }
}
