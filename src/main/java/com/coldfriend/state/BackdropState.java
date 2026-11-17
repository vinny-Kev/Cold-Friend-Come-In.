package com.coldfriend.state;

import com.coldfriend.core.GameSettings;
import com.coldfriend.environment.BackdropSettings;
import com.coldfriend.environment.WinterBackdrop;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FogFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;

/**
 * Owns the menu backdrop: the 3D winter scene, distance fog that fades it into the sky colour, and a translucent
 * dark overlay that keeps menu text readable. Stays enabled for the whole time the menus are shown.
 */
public final class BackdropState extends BaseAppState {

    /** Overcast sky colour; the background and the colour distant geometry fades into. */
    private static final ColorRGBA SKY_COLOR = new ColorRGBA(0.52f, 0.58f, 0.66f, 1f);
    private static final ColorRGBA DIM_COLOR = new ColorRGBA(0f, 0f, 0f, 0.45f);
    /** Fog density at the Normal level; the player's fog setting scales it. */
    private static final float FOG_DENSITY = 1.0f;
    private static final float FOG_DISTANCE = 120f;

    private final BackdropSettings settings;
    private GameSettings player;
    private WinterBackdrop backdrop;
    private FogFilter fog;
    private FilterPostProcessor fogProcessor;
    private Geometry dimmer;
    private float dimmerWidth = -1f;
    private float dimmerHeight = -1f;

    /**
     * @param settings tuning values for the backdrop
     * @param player the player's backdrop preferences at start-up
     */
    public BackdropState(BackdropSettings settings, GameSettings player) {
        this.settings = settings;
        this.player = player;
    }

    /**
     * Applies the player's fog, snow and camera sway preferences, immediately.
     *
     * @param updated the settings now in effect
     */
    public void apply(GameSettings updated) {
        player = updated;
        if (backdrop == null) {
            return;
        }
        float factor = updated.fog().factor();
        fog.setFogDensity(FOG_DENSITY * factor);
        fog.setEnabled(factor > 0f);
        backdrop.setSnow(updated.snow());
        backdrop.setSway(updated.cameraSway());
    }

    @Override
    protected void initialize(Application app) {
        backdrop = new WinterBackdrop(app.getAssetManager(), settings);
        fog = new FogFilter(SKY_COLOR, FOG_DENSITY, FOG_DISTANCE);
        fogProcessor = new FilterPostProcessor(app.getAssetManager());
        fogProcessor.addFilter(fog);
        apply(player);
        Material dimMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        dimMaterial.setColor("Color", DIM_COLOR);
        dimMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        dimmer = new Geometry("menu-dim", new Quad(1f, 1f));
        dimmer.setMaterial(dimMaterial);
    }

    @Override
    protected void cleanup(Application app) {
        backdrop = null;
        fog = null;
        fogProcessor = null;
        dimmer = null;
    }

    @Override
    protected void onEnable() {
        SimpleApplication simple = (SimpleApplication) getApplication();
        simple.getViewPort().setBackgroundColor(SKY_COLOR);
        simple.getViewPort().addProcessor(fogProcessor);
        backdrop.attach(simple.getRootNode(), simple.getCamera());
        simple.getGuiNode().attachChild(dimmer);
        resizeDimmer();
    }

    @Override
    protected void onDisable() {
        ((SimpleApplication) getApplication()).getViewPort().removeProcessor(fogProcessor);
        backdrop.detach();
        dimmer.removeFromParent();
    }

    @Override
    public void update(float tpf) {
        if (!isEnabled()) {
            return;
        }
        backdrop.update(tpf);
        if (screenSizeChanged()) {
            resizeDimmer();
        }
    }

    private boolean screenSizeChanged() {
        float width = getApplication().getCamera().getWidth();
        float height = getApplication().getCamera().getHeight();
        return width != dimmerWidth || height != dimmerHeight;
    }

    private void resizeDimmer() {
        dimmerWidth = getApplication().getCamera().getWidth();
        dimmerHeight = getApplication().getCamera().getHeight();
        dimmer.setMesh(new Quad(dimmerWidth, dimmerHeight));
    }
}
