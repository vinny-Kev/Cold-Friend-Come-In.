package com.coldfriend.dev;

import com.coldfriend.environment.MeshCleanup;
import com.coldfriend.environment.TerrainMeshBuilder;
import com.coldfriend.environment.WaterLighting;
import com.coldfriend.environment.WaterView;
import com.coldfriend.world.GeneratedWorld;
import com.coldfriend.world.WaterBody;
import com.coldfriend.world.WaterType;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FogFilter;
import com.jme3.scene.Geometry;
import com.jme3.system.AppSettings;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Developer tool: flies a camera around one generated world so terrain and water can be inspected.
 *
 * <p>Arguments: {@code --seed N} (default 1), {@code --view overview|river|stream|lake|ice|open} (default
 * overview), {@code --night}, {@code --cam x,y,z --look x,y,z} (explicit camera), {@code --shot PATH} (save a screenshot, then exit; PATH is a directory and
 * file name without extension, e.g. {@code build/shots/seed1}). Fly with WASD and the mouse.
 */
public final class WorldPreviewApp extends SimpleApplication {

    private static final Logger LOG = LoggerFactory.getLogger(WorldPreviewApp.class);
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int SHOT_FRAME = 45;
    private static final int EXIT_FRAME = 60;
    private static final float MOVE_SPEED = 25f;
    /** Lit terrain is vertex-coloured, so scene lights are scaled down to avoid blowing out snow. */
    private static final float SCENE_LIGHT_SCALE = 0.55f;
    private static final float FOV = 55f;
    private static final float FAR = 600f;
    private static final float FOG_DENSITY = 1.0f;
    private static final float FOG_DISTANCE = 260f;
    private static final float CAMERA_BACK = 14f;
    private static final float CAMERA_UP = 7f;
    private static final float LAKE_CAMERA_BACK = 42f;
    private static final float LAKE_CAMERA_UP = 14f;
    private static final float OVERVIEW_UP = 110f;
    private static final float OVERVIEW_BACK = 150f;

    private final long seed;
    private final String view;
    private final boolean night;
    private final String shotPath;
    private Vector3f camPosition;
    private Vector3f camTarget;
    private GeneratedWorld world;
    private WaterView water;
    private Geometry terrain;
    private int frame;

    private WorldPreviewApp(long seed, String view, boolean night, String shotPath) {
        super();
        this.seed = seed;
        this.view = view;
        this.night = night;
        this.shotPath = shotPath;
    }

    /**
     * Starts the preview.
     *
     * @param args see class comment
     */
    public static void main(String[] args) {
        long seed = 1L;
        String view = "overview";
        boolean night = false;
        String shot = null;
        Vector3f camPosition = null;
        Vector3f camTarget = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--seed" -> seed = Long.parseLong(args[++i]);
                case "--view" -> view = args[++i];
                case "--night" -> night = true;
                case "--shot" -> shot = args[++i];
                case "--cam" -> camPosition = parseVector(args[++i]);
                case "--look" -> camTarget = parseVector(args[++i]);
                default -> throw new IllegalArgumentException("unknown argument: " + args[i]);
            }
        }
        WorldPreviewApp app = new WorldPreviewApp(seed, view, night, shot);
        app.camPosition = camPosition;
        app.camTarget = camTarget;
        AppSettings settings = new AppSettings(true);
        settings.setResolution(WIDTH, HEIGHT);
        settings.setTitle("Water preview");
        settings.setVSync(true);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.setDisplayStatView(false);
        app.setDisplayFps(false);
        app.start();
    }

    private static Vector3f parseVector(String text) {
        String[] parts = text.split(",");
        return new Vector3f(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2]));
    }

    @Override
    public void simpleInitApp() {
        world = GeneratedWorld.generate(seed);
        ColorRGBA sky = night ? new ColorRGBA(0.04f, 0.05f, 0.09f, 1f) : new ColorRGBA(0.52f, 0.58f, 0.66f, 1f);
        viewPort.setBackgroundColor(sky);
        WaterLighting lighting = night ? WaterLighting.night() : WaterLighting.day();

        rootNode.addLight(new AmbientLight(lighting.ambient().mult(SCENE_LIGHT_SCALE)));
        rootNode.addLight(new DirectionalLight(lighting.sunDirection().normalize(),
                lighting.sun().mult(SCENE_LIGHT_SCALE)));
        terrain = TerrainMeshBuilder.build(assetManager, world);
        rootNode.attachChild(terrain);
        water = new WaterView(assetManager, world, lighting);
        rootNode.attachChild(water.node());

        FilterPostProcessor fog = new FilterPostProcessor(assetManager);
        fog.addFilter(new FogFilter(sky, FOG_DENSITY, FOG_DISTANCE));
        viewPort.addProcessor(fog);

        flyCam.setMoveSpeed(MOVE_SPEED);
        cam.setFrustumPerspective(FOV, (float) WIDTH / HEIGHT, 0.3f, FAR);
        placeCamera();
        LOG.info("seed {} view {} night {}: {} water bodies", seed, view, night, world.waterBodies().size());
        for (WaterBody b : world.waterBodies()) {
            LOG.info("  {} points={} frozen={} ice={}m", b.type(), b.pointCount(), b.isFrozen(),
                    b.iceThickness());
        }
        if (shotPath != null) {
            ScreenshotAppState shots = new ScreenshotAppState("", shotPath);
            shots.setIsNumbered(false);
            stateManager.attach(shots);
        }
    }

    private void placeCamera() {
        if (camPosition != null && camTarget != null) {
            cam.setLocation(camPosition);
            cam.lookAt(camTarget, Vector3f.UNIT_Y);
            return;
        }
        switch (view) {
            case "river" -> lookAlong(first(b -> b.type() == WaterType.RIVER));
            case "stream" -> lookAlong(first(b -> b.type() == WaterType.STREAM));
            case "lake" -> lookAtBasin(first(b -> b.type() == WaterType.LAKE));
            case "ice" -> lookAt(first(WaterBody::isFrozen));
            case "open" -> lookAt(first(b -> !b.isFrozen()));
            default -> {
                cam.setLocation(new Vector3f(0f, OVERVIEW_UP, OVERVIEW_BACK));
                cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
            }
        }
    }

    private void lookAt(WaterBody body) {
        if (body.pointCount() == 1) {
            lookAtBasin(body);
        } else {
            lookAlong(body);
        }
    }

    private WaterBody first(Predicate<WaterBody> filter) {
        return world.waterBodies().stream().filter(filter).findFirst()
                .orElseThrow(() -> new IllegalStateException("this seed has no such water body"));
    }

    /** Stands upstream and a little above a flowing body's midpoint, looking downstream. */
    private void lookAlong(WaterBody body) {
        int mid = body.pointCount() / 2;
        float tx = body.x(mid + 1) - body.x(mid);
        float tz = body.z(mid + 1) - body.z(mid);
        float len = (float) Math.hypot(tx, tz);
        tx /= len;
        tz /= len;
        float x = body.x(mid) - tx * CAMERA_BACK;
        float z = body.z(mid) - tz * CAMERA_BACK;
        cam.setLocation(new Vector3f(x, world.terrainHeight(x, z) + CAMERA_UP, z));
        cam.lookAt(new Vector3f(body.x(mid), body.surface(mid), body.z(mid)), Vector3f.UNIT_Y);
    }

    private void lookAtBasin(WaterBody body) {
        float x = body.x(0);
        float z = body.z(0) + LAKE_CAMERA_BACK;
        cam.setLocation(new Vector3f(x, body.surface(0) + LAKE_CAMERA_UP, z));
        cam.lookAt(new Vector3f(body.x(0), body.surface(0), body.z(0)), Vector3f.UNIT_Y);
    }

    @Override
    public void simpleUpdate(float tpf) {
        frame++;
        if (shotPath != null) {
            if (frame == SHOT_FRAME) {
                stateManager.getState(ScreenshotAppState.class).takeScreenshot();
            } else if (frame >= EXIT_FRAME) {
                stop();
            }
        }
    }

    @Override
    public void destroy() {
        if (water != null) {
            water.dispose();
        }
        if (terrain != null) {
            terrain.removeFromParent();
            MeshCleanup.release(terrain.getMesh());
        }
        super.destroy();
    }
}
