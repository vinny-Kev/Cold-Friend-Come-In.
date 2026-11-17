package com.coldfriend.assets;

import com.jme3.asset.AssetManager;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.texture.Texture;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the models listed in {@code assets/models/catalog.properties} and prepares them for instanced drawing:
 * each model's PBR material is replaced by a simple lit material that keeps its texture, which is cheaper and
 * works with the scene's plain lights. A kind with no usable model gets simple procedural geometry. Item models
 * (keys {@code ITEM.<key>}) are loaded without instancing; an item with no model gets a magenta placeholder cube.
 */
public final class ModelCatalog implements PropCatalog {

    private static final Logger LOG = LoggerFactory.getLogger(ModelCatalog.class);
    private static final String LIGHTING = "Common/MatDefs/Light/Lighting.j3md";
    private static final String SCALE_SEPARATOR = ":";
    private static final String TINT_SUFFIX = ".tint";
    private static final String ITEM_PREFIX = "ITEM.";
    private static final float PLACEHOLDER_HALF = 0.15f;
    private static final ColorRGBA PLACEHOLDER_COLOR = new ColorRGBA(1f, 0.1f, 0.9f, 1f);
    private static final ColorRGBA TINT = new ColorRGBA(0.88f, 0.9f, 0.95f, 1f);
    private static final float LOG_RADIUS = 0.3f;
    private static final float LOG_LENGTH = 3.2f;
    private static final int LOG_SAMPLES = 8;
    private static final ColorRGBA LOG_COLOR = new ColorRGBA(0.32f, 0.22f, 0.15f, 1f);
    private static final ColorRGBA FALLBACK_COLOR = new ColorRGBA(0.45f, 0.45f, 0.48f, 1f);
    private static final float FALLBACK_SIZE = 0.4f;

    private final AssetManager assets;
    private final Map<PropKind, List<PropModel>> models = new EnumMap<>(PropKind.class);
    private final Map<String, PropModel> itemModels = new HashMap<>();
    private PropModel placeholder;

    /**
     * @param assets the engine asset manager, with the assets folder registered
     * @param catalogFile the catalogue file listing the models
     * @throws IOException if the catalogue file cannot be read
     */
    public ModelCatalog(AssetManager assets, Path catalogFile) throws IOException {
        this.assets = assets;
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(catalogFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        for (PropKind kind : PropKind.values()) {
            List<PropModel> list = new ArrayList<>();
            ColorRGBA tint = parseTint(properties.getProperty(kind.name() + TINT_SUFFIX));
            for (String entry : properties.getProperty(kind.name(), "").split(",")) {
                if (!entry.isBlank()) {
                    load(entry.trim(), tint, true).ifPresent(list::add);
                }
            }
            if (list.isEmpty()) {
                LOG.info("No model files for {}; using procedural geometry", kind);
                list.add(fallback(kind));
            }
            models.put(kind, list);
        }
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(ITEM_PREFIX) && !key.endsWith(TINT_SUFFIX)) {
                String first = properties.getProperty(key).split(",")[0].trim();
                if (!first.isEmpty()) {
                    load(first, TINT, false).ifPresent(model -> itemModels.put(key.substring(ITEM_PREFIX.length()),
                            model));
                }
            }
        }
    }

    @Override
    public PropModel itemModel(String modelKey) {
        PropModel model = itemModels.get(modelKey);
        return model != null ? model : placeholderItem();
    }

    /** One shared bright magenta cube standing in for every item that has no model yet. */
    private PropModel placeholderItem() {
        if (placeholder == null) {
            Geometry cube = new Geometry("item-placeholder", new Box(PLACEHOLDER_HALF, PLACEHOLDER_HALF,
                    PLACEHOLDER_HALF));
            Material material = new Material(assets, LIGHTING);
            material.setBoolean("UseMaterialColors", true);
            material.setColor("Diffuse", PLACEHOLDER_COLOR);
            material.setColor("Ambient", PLACEHOLDER_COLOR);
            cube.setMaterial(material);
            cube.setLocalTranslation(0f, PLACEHOLDER_HALF, 0f);
            Node holder = new Node("item-placeholder");
            holder.attachChild(cube);
            placeholder = new PropModel(holder, 1f);
        }
        return placeholder;
    }

    @Override
    public PropModel model(PropKind kind, int variant) {
        List<PropModel> list = models.get(kind);
        return list.get(Math.floorMod(variant, list.size()));
    }

    private Optional<PropModel> load(String entry, ColorRGBA tint, boolean instancing) {
        String path = entry;
        float scale = 1f;
        int colon = entry.lastIndexOf(SCALE_SEPARATOR);
        if (colon > 0 && !entry.substring(colon).contains("/")) {
            path = entry.substring(0, colon);
            scale = Float.parseFloat(entry.substring(colon + 1));
        }
        try {
            Spatial model = assets.loadModel(path);
            prepare(model, tint, instancing);
            return Optional.of(new PropModel(model, scale));
        } catch (RuntimeException e) {
            LOG.warn("Could not load model {}: {}", path, e.getMessage());
            return Optional.empty();
        }
    }

    /** Swaps each geometry's material for an instancing-capable lit material that keeps its texture. */
    private void prepare(Spatial model, ColorRGBA tint, boolean instancing) {
        model.depthFirstTraversal(new SceneGraphVisitorAdapter() {
            @Override
            public void visit(Geometry geometry) {
                geometry.setMaterial(convert(geometry.getMaterial(), tint, instancing));
                geometry.setShadowMode(ShadowMode.Off);
            }
        });
    }

    private Material convert(Material original, ColorRGBA tint, boolean instancing) {
        Material material = new Material(assets, LIGHTING);
        MatParam base = original.getParam("BaseColorMap");
        if (base != null && base.getValue() instanceof Texture texture) {
            material.setTexture("DiffuseMap", texture);
        }
        MatParam cutoff = original.getParam("AlphaDiscardThreshold");
        if (cutoff != null) {
            material.setFloat("AlphaDiscardThreshold", (Float) cutoff.getValue());
        }
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Diffuse", tint);
        material.setColor("Ambient", tint);
        material.setColor("Specular", ColorRGBA.Black);
        material.setBoolean("UseInstancing", instancing);
        material.getAdditionalRenderState().setFaceCullMode(original.getAdditionalRenderState().getFaceCullMode());
        return material;
    }

    /** Reads "r,g,b" (each 0 to 1) from the catalogue, or returns the default tint if absent or malformed. */
    private static ColorRGBA parseTint(String text) {
        if (text == null || text.isBlank()) {
            return TINT;
        }
        String[] parts = text.split(",");
        try {
            return new ColorRGBA(Float.parseFloat(parts[0].trim()), Float.parseFloat(parts[1].trim()),
                    Float.parseFloat(parts[2].trim()), 1f);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            LOG.warn("Ignoring malformed tint '{}'", text);
            return TINT;
        }
    }

    private PropModel fallback(PropKind kind) {
        Geometry geometry;
        ColorRGBA color = FALLBACK_COLOR;
        if (kind == PropKind.FALLEN_LOG) {
            geometry = new Geometry("log", new Cylinder(2, LOG_SAMPLES, LOG_RADIUS, LOG_LENGTH, true));
            geometry.setLocalTranslation(0f, LOG_RADIUS, 0f);
            color = LOG_COLOR;
        } else {
            geometry = new Geometry(kind.name(), new Box(FALLBACK_SIZE, FALLBACK_SIZE, FALLBACK_SIZE));
            geometry.setLocalTranslation(0f, FALLBACK_SIZE, 0f);
        }
        Material material = new Material(assets, LIGHTING);
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Diffuse", color);
        material.setColor("Ambient", color);
        material.setBoolean("UseInstancing", true);
        material.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Back);
        geometry.setMaterial(material);
        Node holder = new Node(kind.name() + "-fallback");
        holder.attachChild(geometry);
        return new PropModel(holder, 1f);
    }
}
