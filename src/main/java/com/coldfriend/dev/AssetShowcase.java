package com.coldfriend.dev;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary developer tool, not part of the game. Places every model side by side at its real size so their
 * scale and style can be compared.
 */
public final class AssetShowcase extends SimpleApplication {

    private static final Logger LOG = LoggerFactory.getLogger(AssetShowcase.class);
    private static final String[] CHARACTERS = {"Barbarian", "Knight", "Mage", "Ranger", "Rogue", "Rogue_Hooded"};
    private static final String[] KAYKIT_TREES = {
        "Tree_Bare_1_A_Color1", "Tree_Bare_1_B_Color1", "Tree_Bare_1_C_Color1",
        "Tree_Bare_2_A_Color1", "Tree_Bare_2_B_Color1", "Tree_Bare_2_C_Color1"};
    private static final String[] DEAD_TREES = {"DeadTree_1", "DeadTree_2", "DeadTree_3", "DeadTree_4", "DeadTree_5"};
    private static final String[] HOUSES = {"house1", "house2", "house3", "house4", "house5"};

    /** @param args unused */
    public static void main(String[] args) {
        AssetShowcase app = new AssetShowcase();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Asset showcase (temporary)");
        settings.setWidth(1280);
        settings.setHeight(720);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        assetManager.registerLocator("assets", FileLocator.class);
        flyCam.setEnabled(false);
        viewPort.setBackgroundColor(new ColorRGBA(0.52f, 0.58f, 0.66f, 1f));
        rootNode.addLight(new AmbientLight(new ColorRGBA(0.6f, 0.6f, 0.65f, 1f)));
        rootNode.addLight(new DirectionalLight(new Vector3f(-0.4f, -1f, -0.3f).normalizeLocal(), ColorRGBA.White));

        Material groundMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        groundMaterial.setColor("Color", new ColorRGBA(0.55f, 0.58f, 0.62f, 1f));
        Geometry ground = new Geometry("ground", new Box(70f, 0.1f, 70f));
        ground.setMaterial(groundMaterial);
        ground.setLocalTranslation(0f, -0.1f, -20f);
        rootNode.attachChild(ground);

        for (int i = 0; i < CHARACTERS.length; i++) {
            place("models/characters/" + CHARACTERS[i] + ".glb", -8f + i * 3.2f, 0f);
        }
        for (int i = 0; i < KAYKIT_TREES.length; i++) {
            place("models/environment/trees/kaykit_forest/" + KAYKIT_TREES[i] + ".gltf", -15f + i * 6f, -12f);
        }
        for (int i = 0; i < DEAD_TREES.length; i++) {
            place("models/environment/trees/quaternius_megakit/" + DEAD_TREES[i] + ".gltf", -20f + i * 10f, -30f);
        }
        for (int i = 0; i < HOUSES.length; i++) {
            place("models/structures/" + HOUSES[i] + ".glb", -36f + i * 18f, -55f);
        }

        flyCam.setEnabled(false);
        cam.setLocation(new Vector3f(0f, 9f, 34f));
        cam.lookAt(new Vector3f(0f, 4f, -22f), Vector3f.UNIT_Y);
    }

    private void place(String path, float x, float z) {
        try {
            Spatial model = assetManager.loadModel(path);
            model.setLocalTranslation(x, 0f, z);
            rootNode.attachChild(model);
        } catch (RuntimeException e) {
            LOG.error("Could not load {}: {}", path, e.getMessage());
        }
    }

}
