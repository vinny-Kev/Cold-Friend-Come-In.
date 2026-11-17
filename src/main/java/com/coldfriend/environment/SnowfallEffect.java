package com.coldfriend.environment;

import com.jme3.asset.AssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.shapes.EmitterBoxShape;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;

/** Subtle falling snow: a particle emitter spread over a box above the camera's view. */
final class SnowfallEffect {

    private static final Vector3f BOX_MIN = new Vector3f(-30f, 6f, -45f);
    private static final Vector3f BOX_MAX = new Vector3f(30f, 14f, 4f);
    private static final Vector3f FALL_VELOCITY = new Vector3f(0.2f, -1.2f, 0f);
    private static final float VELOCITY_VARIATION = 0.3f;
    private static final float MIN_LIFE = 7f;
    private static final float MAX_LIFE = 11f;
    private static final float FLAKE_SIZE = 0.07f;
    private static final float FLAKE_ALPHA = 0.9f;
    private static final float FLAKE_ALPHA_END = 0.4f;

    private SnowfallEffect() {
    }

    /**
     * Creates the snow emitter.
     *
     * @param assets asset manager used to load the particle material
     * @param textureName texture path used for each flake
     * @param maxParticles maximum flakes alive at once
     * @return the emitter, ready to attach to the scene
     */
    static ParticleEmitter create(AssetManager assets, String textureName, int maxParticles) {
        ParticleEmitter emitter = new ParticleEmitter("snowfall", ParticleMesh.Type.Triangle, maxParticles);
        Material material = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
        material.setTexture("Texture", assets.loadTexture(textureName));
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        emitter.setMaterial(material);
        emitter.setQueueBucket(RenderQueue.Bucket.Transparent);
        emitter.setImagesX(1);
        emitter.setImagesY(1);
        emitter.setStartColor(new ColorRGBA(1f, 1f, 1f, FLAKE_ALPHA));
        emitter.setEndColor(new ColorRGBA(1f, 1f, 1f, FLAKE_ALPHA_END));
        emitter.setStartSize(FLAKE_SIZE);
        emitter.setEndSize(FLAKE_SIZE);
        emitter.setGravity(0f, 0f, 0f);
        emitter.setLowLife(MIN_LIFE);
        emitter.setHighLife(MAX_LIFE);
        emitter.setParticlesPerSec(maxParticles / ((MIN_LIFE + MAX_LIFE) / 2f));
        emitter.setShape(new EmitterBoxShape(BOX_MIN, BOX_MAX));
        emitter.getParticleInfluencer().setInitialVelocity(FALL_VELOCITY);
        emitter.getParticleInfluencer().setVelocityVariation(VELOCITY_VARIATION);
        return emitter;
    }
}
