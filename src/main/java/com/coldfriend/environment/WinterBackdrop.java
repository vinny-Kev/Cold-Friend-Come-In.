package com.coldfriend.environment;

import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import java.util.Random;

/**
 * Procedural winter forest scene used behind the menus: snow ground, crooked conifers, a lit cabin, falling
 * snow, distance fog, and a camera that sways very slowly. Uses placeholder materials and geometry only.
 */
public final class WinterBackdrop {

    private static final long LAYOUT_SEED = 710_17L;
    private static final float GROUND_SIZE = 400f;
    private static final float GROUND_THICKNESS = 0.2f;
    private static final float GROUND_CENTER_Z = -80f;
    private static final float CABIN_Z = -30f;
    private static final float CABIN_CLEARING_HALF_WIDTH = 8f;
    private static final float CABIN_CLEARING_HALF_DEPTH = 10f;
    private static final float TREE_SPREAD_X = 40f;
    private static final float TREE_MIN_Z = -60f;
    private static final float TREE_MAX_Z = -4f;
    private static final float CAMERA_HEIGHT = 2.0f;
    private static final float CAMERA_Z = 6f;
    private static final float CAMERA_FOCUS_Y = 1.5f;
    private static final float CAMERA_FOV = 55f;
    private static final float CAMERA_NEAR = 0.5f;
    private static final float CAMERA_FAR = 300f;
    private static final float WINDOW_LIGHT_RADIUS = 14f;
    private static final float WINDOW_LIGHT_Y = 1.4f;
    private static final float WINDOW_LIGHT_Z = CABIN_Z + 2.6f;
    private static final float WINDOW_LIGHT_INTENSITY = 0.9f;
    private static final Vector3f SUN_DIRECTION = new Vector3f(-0.4f, -1f, -0.3f);
    private static final String SNOW_TEXTURE = "Common/Textures/dot.png";

    private static final ColorRGBA GROUND_COLOR = new ColorRGBA(0.7f, 0.74f, 0.8f, 1f);
    private static final ColorRGBA TREE_COLOR = new ColorRGBA(0.12f, 0.17f, 0.15f, 1f);
    private static final ColorRGBA TRUNK_COLOR = new ColorRGBA(0.22f, 0.17f, 0.13f, 1f);
    private static final ColorRGBA AMBIENT_COLOR = new ColorRGBA(0.35f, 0.38f, 0.46f, 1f);
    private static final ColorRGBA MOON_COLOR = new ColorRGBA(0.5f, 0.56f, 0.68f, 1f);
    private static final ColorRGBA WINDOW_GLOW = new ColorRGBA(1.0f, 0.72f, 0.38f, 1f);
    private static final ColorRGBA DOOR_COLOR = new ColorRGBA(0.16f, 0.11f, 0.08f, 1f);
    private static final ColorRGBA LOG_COLOR = new ColorRGBA(0.36f, 0.25f, 0.17f, 1f);
    private static final ColorRGBA ROOF_COLOR = new ColorRGBA(0.2f, 0.16f, 0.14f, 1f);

    private final AssetManager assets;
    private final BackdropSettings settings;
    private final Node scene = new Node("winter-backdrop");
    private final Vector3f cameraPosition = new Vector3f();
    private final Vector3f cameraFocus = new Vector3f(0f, CAMERA_FOCUS_Y, CABIN_Z);
    private Camera camera;
    private float cameraPhase;

    /**
     * Builds the scene graph. Nothing is attached until {@link #attach}.
     *
     * @param assets engine asset manager used to load materials and textures
     * @param settings tuning values
     */
    public WinterBackdrop(AssetManager assets, BackdropSettings settings) {
        this.assets = assets;
        this.settings = settings;
        buildLights();
        buildGround();
        buildTrees();
        buildCabin();
        buildSnowfall();
    }

    /**
     * Adds the scene to the root node and takes control of the camera.
     *
     * @param rootNode the 3D root node
     * @param camera the camera the backdrop moves
     */
    public void attach(Node rootNode, Camera camera) {
        this.camera = camera;
        rootNode.attachChild(scene);
        float aspect = (float) camera.getWidth() / camera.getHeight();
        camera.setFrustumPerspective(CAMERA_FOV, aspect, CAMERA_NEAR, CAMERA_FAR);
        updateCamera();
    }

    /** Removes the scene from the root node. */
    public void detach() {
        scene.removeFromParent();
    }

    /**
     * Advances the camera sway.
     *
     * @param tpf seconds since the previous frame
     */
    public void update(float tpf) {
        if (camera == null) {
            return;
        }
        cameraPhase += tpf * settings.cameraDriftSpeed();
        updateCamera();
    }

    private void updateCamera() {
        cameraPosition.set(
                FastMath.sin(cameraPhase) * settings.cameraDriftRadius(),
                CAMERA_HEIGHT,
                CAMERA_Z);
        camera.setLocation(cameraPosition);
        camera.lookAt(cameraFocus, Vector3f.UNIT_Y);
    }

    private void buildLights() {
        scene.addLight(new AmbientLight(AMBIENT_COLOR));
        DirectionalLight sun = new DirectionalLight(SUN_DIRECTION.normalize(), MOON_COLOR);
        scene.addLight(sun);
        PointLight window = new PointLight(new Vector3f(0f, WINDOW_LIGHT_Y, WINDOW_LIGHT_Z), WINDOW_GLOW,
                WINDOW_LIGHT_RADIUS);
        window.setColor(WINDOW_GLOW.mult(WINDOW_LIGHT_INTENSITY));
        scene.addLight(window);
    }

    private void buildGround() {
        Geometry ground = new Geometry("ground",
                new Box(GROUND_SIZE / 2f, GROUND_THICKNESS / 2f, GROUND_SIZE / 2f));
        ground.setMaterial(litMaterial(GROUND_COLOR));
        ground.setLocalTranslation(0f, -GROUND_THICKNESS / 2f, GROUND_CENTER_Z);
        scene.attachChild(ground);
    }

    private void buildTrees() {
        Random random = new Random(LAYOUT_SEED);
        TreeFactory trees = new TreeFactory(litMaterial(TRUNK_COLOR), litMaterial(TREE_COLOR), random);
        for (int i = 0; i < settings.treeCount(); i++) {
            float x;
            float z;
            do {
                x = (random.nextFloat() * 2f - 1f) * TREE_SPREAD_X;
                z = TREE_MIN_Z + random.nextFloat() * (TREE_MAX_Z - TREE_MIN_Z);
            } while (inCabinClearing(x, z));
            scene.attachChild(trees.createTree(x, z));
        }
    }

    private static boolean inCabinClearing(float x, float z) {
        return Math.abs(x) < CABIN_CLEARING_HALF_WIDTH
                && Math.abs(z - CABIN_Z) < CABIN_CLEARING_HALF_DEPTH;
    }

    private void buildCabin() {
        CabinFactory factory = new CabinFactory(
                litMaterial(LOG_COLOR),
                litMaterial(ROOF_COLOR),
                litMaterial(DOOR_COLOR),
                unshadedMaterial(WINDOW_GLOW));
        Node cabin = factory.createCabin();
        cabin.setLocalTranslation(0f, 0f, CABIN_Z);
        scene.attachChild(cabin);
    }

    private void buildSnowfall() {
        scene.attachChild(SnowfallEffect.create(assets, SNOW_TEXTURE, settings.snowParticleCount()));
    }

    private Material litMaterial(ColorRGBA color) {
        Material material = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Diffuse", color);
        material.setColor("Ambient", color);
        return material;
    }

    private Material unshadedMaterial(ColorRGBA color) {
        Material material = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        return material;
    }
}
