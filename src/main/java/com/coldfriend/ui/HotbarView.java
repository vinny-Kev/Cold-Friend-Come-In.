package com.coldfriend.ui;

import com.coldfriend.assets.IconCatalog;
import com.coldfriend.inventory.Inventory;
import com.coldfriend.item.ItemDefinition;
import com.coldfriend.item.ItemStack;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * The four-slot hotbar at the bottom centre of the play screen. It shows backpack slots 1 to 4 (indexes 0 to 3), so
 * it is a view of the inventory, not a second store. Redraws only the slots whose contents or selection changed.
 */
public final class HotbarView {

    /** Number of hotbar slots; they are the first backpack slots. */
    public static final int SLOTS = Inventory.HOTBAR_SLOTS;

    private static final float SLOT = 64f;
    private static final float GAP = 8f;
    private static final float RIGHT_MARGIN = 24f;
    private static final float FRAME = 3f;
    private static final float ICON = 34f;
    private static final float LETTER_SCALE = 1.4f;
    private static final float COUNT_SCALE = 0.9f;
    private static final float Z_FRAME = 0.0f;
    private static final float Z_SLOT = 0.1f;
    private static final float Z_ICON = 0.2f;
    private static final float Z_TEXT = 0.3f;
    private static final ColorRGBA SLOT_COLOR = new ColorRGBA(0.12f, 0.14f, 0.18f, 0.88f);
    private static final ColorRGBA EMPTY_COLOR = new ColorRGBA(0.08f, 0.09f, 0.12f, 0.7f);
    private static final ColorRGBA FRAME_NONE = new ColorRGBA(0f, 0f, 0f, 0f);
    private static final ColorRGBA FRAME_SELECTED = new ColorRGBA(1.0f, 0.85f, 0.45f, 1f);
    private static final ColorRGBA TEXT_COLOR = new ColorRGBA(0.92f, 0.95f, 1f, 1f);

    private final AssetManager assets;
    private final IconCatalog iconCatalog;
    private final Node root = new Node("hotbar");
    private final Geometry[] frames = new Geometry[SLOTS];
    private final Geometry[] backs = new Geometry[SLOTS];
    private final Geometry[] icons = new Geometry[SLOTS];
    private final BitmapText[] letters = new BitmapText[SLOTS];
    private final BitmapText[] counts = new BitmapText[SLOTS];
    private final ItemStack[] shownStack = new ItemStack[SLOTS];
    private final boolean[] shownDrawn = new boolean[SLOTS];
    private final boolean[] shownSelected = new boolean[SLOTS];
    private final Material emptyMaterial;

    /**
     * @param assets the engine asset manager, used for the slot materials
     * @param font the font for letters and counts
     */
    public HotbarView(AssetManager assets, BitmapFont font, IconCatalog iconCatalog) {
        this.assets = assets;
        this.iconCatalog = iconCatalog;
        this.emptyMaterial = material(EMPTY_COLOR);
        for (int i = 0; i < SLOTS; i++) {
            frames[i] = quad("hotbar-frame-" + i, FRAME_NONE);
            backs[i] = quad("hotbar-slot-" + i, SLOT_COLOR);
            icons[i] = quad("hotbar-icon-" + i, SLOT_COLOR);
            letters[i] = text(font, LETTER_SCALE);
            counts[i] = text(font, COUNT_SCALE);
            root.attachChild(frames[i]);
            root.attachChild(backs[i]);
            root.attachChild(icons[i]);
            root.attachChild(letters[i]);
            root.attachChild(counts[i]);
        }
    }

    /** @return the node to attach to the GUI */
    public Node root() {
        return root;
    }

    /**
     * Places the slots in a vertical column on the right edge, centred vertically, with slot 1 at the top.
     *
     * @param width drawing width in pixels
     * @param height drawing height in pixels
     */
    public void layout(float width, float height) {
        float columnHeight = SLOTS * SLOT + (SLOTS - 1) * GAP;
        float x = width - RIGHT_MARGIN - SLOT;
        float top = height / 2f + columnHeight / 2f;
        for (int i = 0; i < SLOTS; i++) {
            float bottom = top - (i + 1) * SLOT - i * GAP;
            place(frames[i], x - FRAME, bottom - FRAME, SLOT + 2 * FRAME, SLOT + 2 * FRAME, Z_FRAME);
            place(backs[i], x, bottom, SLOT, SLOT, Z_SLOT);
            place(icons[i], x + (SLOT - ICON) / 2f, bottom + (SLOT - ICON) / 2f, ICON, ICON, Z_ICON);
            letters[i].setLocalTranslation(x + SLOT / 2f - 6f, bottom + SLOT / 2f + 8f, Z_TEXT);
            counts[i].setLocalTranslation(x + SLOT - 24f, bottom + SLOT - 16f, Z_TEXT);
        }
    }

    /**
     * Shows the current contents and selection. Only slots that changed since the last call are redrawn.
     *
     * @param inventory the player's backpack; the hotbar shows its first {@link #SLOTS} slots
     */
    public void refresh(Inventory inventory) {
        int selected = inventory.selected();
        for (int i = 0; i < SLOTS && i < inventory.size(); i++) {
            Optional<ItemStack> stack = inventory.slot(i);
            ItemStack now = stack.orElse(null);
            boolean selectedNow = selected == i;
            if (shownDrawn[i] && Objects.equals(now, shownStack[i]) && selectedNow == shownSelected[i]) {
                continue;
            }
            shownDrawn[i] = true;
            shownStack[i] = now;
            shownSelected[i] = selectedNow;
            frames[i].getMaterial().setColor("Color", selectedNow ? FRAME_SELECTED : FRAME_NONE);
            if (stack.isEmpty()) {
                backs[i].setMaterial(emptyMaterial);
                icons[i].setCullHint(Spatial.CullHint.Always);
                letters[i].setText("");
                counts[i].setText("");
                continue;
            }
            ItemDefinition definition = inventory.definitionOf(stack.get());
            backs[i].setMaterial(material(SLOT_COLOR));
            icons[i].setCullHint(Spatial.CullHint.Inherit);
            Optional<Texture> picture = iconCatalog.icon(definition.iconKey());
            if (picture.isPresent()) {
                icons[i].setMaterial(pictureMaterial(picture.get()));
                letters[i].setText("");
            } else {
                icons[i].setMaterial(material(InventoryView.categoryColor(definition.category())));
                letters[i].setText(definition.name().substring(0, 1).toUpperCase(Locale.ROOT));
            }
            counts[i].setText(stack.get().quantity() > 1 ? "x" + stack.get().quantity() : "");
        }
    }

    private static void place(Geometry geometry, float x, float y, float width, float height, float z) {
        geometry.setMesh(new Quad(width, height));
        geometry.setLocalTranslation(x, y, z);
    }

    private Geometry quad(String name, ColorRGBA color) {
        Geometry geometry = new Geometry(name, new Quad(1f, 1f));
        geometry.setMaterial(material(color));
        return geometry;
    }

    private Material material(ColorRGBA color) {
        Material material = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        return material;
    }

    private Material pictureMaterial(Texture picture) {
        Material material = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", picture);
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        return material;
    }

    private static BitmapText text(BitmapFont font, float scale) {
        BitmapText text = new BitmapText(font);
        text.setLocalScale(scale);
        text.setColor(TEXT_COLOR);
        return text;
    }
}
