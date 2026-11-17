package com.coldfriend.audio;

import java.util.Random;

/**
 * Makes a looping wind sound in code, so no audio file or licence is needed. Low-pass filtered noise gives the
 * body of the wind; a mid-band layer adds a thin howl; slow swells make gusts. Every modulation completes a whole
 * number of cycles in the loop, and the filters run around the loop twice, so the end joins the start.
 * Engine-free, so it can be tested directly.
 */
public final class WindLoop {

    private static final double BODY_CUTOFF_HZ = 260.0;
    private static final double HOWL_LOW_HZ = 280.0;
    private static final double HOWL_HIGH_HZ = 900.0;
    private static final double PEAK = 0.6;
    /** Gusts per loop. Each gust is followed by a lull of silence, so the wind is not constant. */
    static final int GUSTS_PER_LOOP = 2;
    private static final double GUST_OFFSET = 0.5;
    /** Higher values make gusts shorter and the lulls between them longer. */
    private static final double GUST_SHARPNESS = 3.0;

    private WindLoop() {
    }

    /**
     * Generates one loop of mono wind.
     *
     * @param sampleRate samples per second
     * @param seconds loop length; the wind joins back on itself after this time
     * @param seed the same seed always gives the same loop
     * @return 16-bit mono samples
     */
    public static short[] generate(int sampleRate, double seconds, long seed) {
        int n = (int) Math.round(sampleRate * seconds);
        Random random = new Random(seed);
        double[] noise = new double[n];
        for (int i = 0; i < n; i++) {
            noise[i] = random.nextDouble() * 2.0 - 1.0;
        }
        double[] body = lowPass(noise, BODY_CUTOFF_HZ, sampleRate);
        double[] low = lowPass(noise, HOWL_LOW_HZ, sampleRate);
        double[] high = lowPass(noise, HOWL_HIGH_HZ, sampleRate);

        double[] mix = new double[n];
        for (int i = 0; i < n; i++) {
            double gust = gustEnvelope((double) i / n);
            mix[i] = (body[i] * 0.8 + (high[i] - low[i]) * 0.5) * gust;
        }
        return toPcm(mix);
    }

    /**
     * Loudness over one loop: {@link #GUSTS_PER_LOOP} gusts, each followed by a lull where the wind dies away to
     * silence. Periodic in the loop, so the join stays smooth.
     */
    static double gustEnvelope(double phase) {
        double s = Math.sin(2 * Math.PI * GUSTS_PER_LOOP * phase + GUST_OFFSET);
        return s <= 0 ? 0.0 : Math.pow(s, GUST_SHARPNESS);
    }

    /**
     * One-pole low-pass filter run around the loop twice: the first pass only sets the filter state, so the
     * second pass starts from the state the end of the loop leaves behind.
     */
    private static double[] lowPass(double[] input, double cutoffHz, int sampleRate) {
        double alpha = 1.0 - Math.exp(-2.0 * Math.PI * cutoffHz / sampleRate);
        double state = 0.0;
        for (double x : input) {
            state += alpha * (x - state);
        }
        double[] out = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            state += alpha * (input[i] - state);
            out[i] = state;
        }
        return out;
    }

    private static short[] toPcm(double[] signal) {
        double peak = 0.0;
        for (double v : signal) {
            peak = Math.max(peak, Math.abs(v));
        }
        double scale = peak > 0.0 ? PEAK / peak : 0.0;
        short[] pcm = new short[signal.length];
        for (int i = 0; i < signal.length; i++) {
            pcm[i] = (short) Math.round(signal[i] * scale * Short.MAX_VALUE);
        }
        return pcm;
    }
}
