package com.coldfriend.audio;

import com.jme3.audio.AudioBuffer;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioKey;
import com.jme3.audio.AudioNode;
import com.jme3.scene.Node;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Background wind: one generated loop, played on repeat at the listener. */
public final class Ambience {

    private static final Logger LOG = LoggerFactory.getLogger(Ambience.class);
    private static final int SAMPLE_RATE = 22050;
    private static final double LOOP_SECONDS = 16.0;
    private static final long SEED = 20261117L;
    /** Wind is kept well under the footsteps so it sits in the background. */
    private static final float WIND_LEVEL = 0.2f;

    private final AudioNode wind;
    private float volume = 1f;

    /**
     * Builds the wind loop and attaches it to the scene.
     *
     * @param parent the node the sound node is attached to
     */
    public Ambience(Node parent) {
        short[] samples = WindLoop.generate(SAMPLE_RATE, LOOP_SECONDS, SEED);
        AudioData data = toBuffer(samples);
        this.wind = new AudioNode(data, new AudioKey("procedural/wind-loop"));
        wind.setPositional(false);
        wind.setLooping(true);
        wind.setVolume(WIND_LEVEL * volume);
        parent.attachChild(wind);
        LOG.info("Wind loop ready: {} s at {} Hz", LOOP_SECONDS, SAMPLE_RATE);
    }

    private static AudioData toBuffer(short[] samples) {
        AudioBuffer buffer = new AudioBuffer();
        buffer.setupFormat(1, 16, SAMPLE_RATE);
        ByteBuffer bytes = ByteBuffer.allocateDirect(samples.length * 2).order(ByteOrder.nativeOrder());
        bytes.asShortBuffer().put(samples);
        buffer.updateData(bytes);
        return buffer;
    }

    /**
     * Sets the wind loudness, as a share of the effects volume.
     *
     * @param newVolume 0 (silent) to 1 (full)
     */
    public void setVolume(float newVolume) {
        volume = Math.max(0f, Math.min(1f, newVolume));
        wind.setVolume(WIND_LEVEL * volume);
    }

    /** Starts the wind from the beginning, or resumes it if it was stopped. */
    public void start() {
        wind.play();
    }

    /** Stops the wind. It starts again from the beginning on the next {@link #start()}. */
    public void stop() {
        wind.stop();
    }

    /** Stops the wind and removes it from the scene. */
    public void cleanup() {
        wind.stop();
        wind.removeFromParent();
    }
}
