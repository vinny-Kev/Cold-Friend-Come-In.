package com.coldfriend.audio;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plays one footstep on snow. The clips are single steps cut from the snow pack, because the pack's own files are
 * 11 to 19 seconds of continuous walking. Walking is quieter than sprinting. Only WAV is used: the engine's OGG
 * loader is not on the classpath. Sounds are not positional yet; they play at the listener.
 */
public final class Footsteps {

    private static final Logger LOG = LoggerFactory.getLogger(Footsteps.class);
    private static final String WALK_FILE = "audio/footsteps/snow/snow_step_medium.wav";
    private static final String SPRINT_FILE = "audio/footsteps/snow/snow_step_fast.wav";
    /** Walking steps are quieter than sprinting steps, so walking is more silent. */
    private static final float WALK_LEVEL = 0.45f;

    private final AudioNode walk;
    private final AudioNode sprint;
    private float volume = 1f;

    /**
     * Loads the two footstep sounds and attaches them to the scene so the engine keeps them updated.
     *
     * @param assets the engine asset manager, with the assets folder registered
     * @param parent the node the sound nodes are attached to
     */
    public Footsteps(AssetManager assets, Node parent) {
        this.walk = load(assets, parent, WALK_FILE);
        this.sprint = load(assets, parent, SPRINT_FILE);
    }

    private static AudioNode load(AssetManager assets, Node parent, String path) {
        AudioNode node = new AudioNode(assets, path, AudioData.DataType.Buffer);
        node.setPositional(false);
        node.setLooping(false);
        parent.attachChild(node);
        return node;
    }

    /**
     * Sets the loudness of both footstep sounds.
     *
     * @param newVolume 0 (silent) to 1 (full)
     */
    public void setVolume(float newVolume) {
        volume = Math.max(0f, Math.min(1f, newVolume));
        walk.setVolume(volume * WALK_LEVEL);
        sprint.setVolume(volume);
    }

    /**
     * Plays one footstep. Each call starts a new instance, so steps can overlap.
     *
     * @param sprinting true for the louder, faster sprint step
     */
    public void play(boolean sprinting) {
        (sprinting ? sprint : walk).playInstance();
    }

    /** Stops any sound still playing and removes the sound nodes from the scene. */
    public void cleanup() {
        walk.stop();
        sprint.stop();
        walk.removeFromParent();
        sprint.removeFromParent();
        LOG.debug("Footstep sounds released");
    }
}
