package com.coldfriend.ui;

import com.coldfriend.inventory.Inventory;
import com.coldfriend.item.ItemCategory;
import com.coldfriend.item.ItemDefinition;
import com.coldfriend.item.ItemStack;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import java.util.Locale;
import java.util.Optional;

/**
 * Draws an {@link Inventory} as a grid of slots with a details panel, in the same plain text-and-boxes style as the
 * menus. It shows state and answers where a point on screen falls; it never changes the inventory. Coordinates
 * are screen pixels with the origin at the bottom left.
 */
public final class InventoryView {

    private static final int COLUMNS = 6;
    private static final float SLOT = 84f;
    private static final float GAP = 8f;
    private static final float FRAME = 4f;
    private static final float ICON = 40f;
    private static final float DETAILS_WIDTH = 360f;
    private static final float PANEL_GAP = 40f;
    private static final float TITLE_TOP = 70f;
    private static final float GRID_TOP = 140f;
    private static final float TEXT_PAD = 6f;
    private static final float NAME_SCALE = 0.85f;
    private static final float COUNT_SCALE = 1.1f;
    private static final float LETTER_SCALE = 1.6f;
    private static final int NAME_CHARS = 10;
    private static final float Z_FRAME = 0.0f;
    private static final float Z_SLOT = 0.1f;
    private static final float Z_ICON = 0.2f;
    private static final float Z_TEXT = 0.3f;
    private static final ColorRGBA SLOT_COLOR = new ColorRGBA(0.12f, 0.14f, 0.18f, 0.88f);
    private static final ColorRGBA EMPTY_COLOR = new ColorRGBA(0.08f, 0.09f, 0.12f, 0.7f);
    private static final ColorRGBA FRAME_NONE = new ColorRGBA(0f, 0f, 0f, 0f);
    private static final ColorRGBA FRAME_CURSOR = new ColorRGBA(1.0f, 0.85f, 0.45f, 1f);
    private static final ColorRGBA FRAME_HELD = new ColorRGBA(0.5f, 0.9f, 1.0f, 1f);
    private static final ColorRGBA TEXT_COLOR = new ColorRGBA(0.92f, 0.95f, 1f, 1f);
    private static final ColorRGBA DIM_TEXT = new ColorRGBA(0.7f, 0.76f, 0.84f, 1f);
    private static final ColorRGBA DIM = new ColorRGBA(0f, 0f, 0f, 0.55f);
    private static final ColorRGBA PANEL = new ColorRGBA(0.1f, 0.12f, 0.16f, 0.88f);

    private final Node root = new Node("inventory-view");
    private final int slotCount;
    private final Geometry dim;
    private final Geometry details;
    private final Geometry[] frames;
    private final Geometry[] backs;
    private final Geometry[] icons;
    private final BitmapText[] letters;
    private final BitmapText[] names;
    private final BitmapText[] counts;
    private final BitmapText title;
    private final BitmapText weight;
    private final BitmapText detailName;
    private final BitmapText detailMeta;
    private final BitmapText detailBody;
    private final BitmapText hint;
    private final BitmapText status;
    private final AssetManager assets;
    private final Material emptyMaterial;
    private float gridLeft;
    private float gridTop;

    /**
     * @param assets the engine asset manager
     * @param font font for all text
     * @param slotCount number of slots to draw
     */
    public InventoryView(AssetManager assets, BitmapFont font, int slotCount) {
        this.assets = assets;
        this.slotCount = slotCount;
        dim = quad("inventory-dim", DIM);
        details = quad("inventory-details", PANEL);
        root.attachChild(dim);
        root.attachChild(details);
        frames = new Geometry[slotCount];
        backs = new Geometry[slotCount];
        icons = new Geometry[slotCount];
        letters = new BitmapText[slotCount];
        names = new BitmapText[slotCount];
        counts = new BitmapText[slotCount];
        emptyMaterial = material(EMPTY_COLOR);
        for (int i = 0; i < slotCount; i++) {
            frames[i] = quad("frame-" + i, FRAME_NONE);
            backs[i] = quad("slot-" + i, SLOT_COLOR);
            icons[i] = quad("icon-" + i, SLOT_COLOR);
            letters[i] = text(font, LETTER_SCALE, TEXT_COLOR);
            names[i] = text(font, NAME_SCALE, DIM_TEXT);
            counts[i] = text(font, COUNT_SCALE, TEXT_COLOR);
            root.attachChild(frames[i]);
            root.attachChild(backs[i]);
            root.attachChild(icons[i]);
            root.attachChild(letters[i]);
            root.attachChild(names[i]);
            root.attachChild(counts[i]);
        }
        title = text(font, 2.4f, TEXT_COLOR);
        title.setText("INVENTORY");
        weight = text(font, 1.0f, DIM_TEXT);
        detailName = text(font, 1.8f, FRAME_CURSOR);
        detailMeta = text(font, 1.0f, DIM_TEXT);
        detailBody = text(font, 1.1f, TEXT_COLOR);
        hint = text(font, 1.0f, DIM_TEXT);
        hint.setText("[Arrows / mouse] select   [Enter / click] pick up and put down   [Q] drop one   "
                + "[X] drop all   [P] place   [I / Esc] close");
        status = text(font, 1.2f, FRAME_HELD);
        for (BitmapText t : new BitmapText[] {title, weight, detailName, detailMeta, detailBody, hint, status}) {
            t.setLocalTranslation(0f, 0f, Z_TEXT);
            root.attachChild(t);
        }
    }

    /** @return the node to attach to the GUI */
    public Node root() {
        return root;
    }

    /**
     * Positions everything for a screen size.
     *
     * @param width screen width in pixels
     * @param height screen height in pixels
     */
    public void layout(float width, float height) {
        float gridWidth = COLUMNS * (SLOT + GAP) - GAP;
        float totalWidth = gridWidth + PANEL_GAP + DETAILS_WIDTH;
        gridLeft = (width - totalWidth) / 2f;
        gridTop = height - GRID_TOP;
        resize(dim, width, height, 0f, 0f, -0.1f);
        title.setLocalTranslation(gridLeft, height - TITLE_TOP, Z_TEXT);
        weight.setLocalTranslation(gridLeft, gridTop + 26f, Z_TEXT);
        float rows = (float) Math.ceil(slotCount / (double) COLUMNS);
        float gridHeight = rows * (SLOT + GAP) - GAP;
        for (int i = 0; i < slotCount; i++) {
            float x = slotX(i);
            float y = slotY(i);
            resize(frames[i], SLOT + 2 * FRAME, SLOT + 2 * FRAME, x - FRAME, y - FRAME, Z_FRAME);
            resize(backs[i], SLOT, SLOT, x, y, Z_SLOT);
            resize(icons[i], ICON, ICON, x + (SLOT - ICON) / 2f, y + SLOT - ICON - 8f, Z_ICON);
            letters[i].setLocalTranslation(x + SLOT / 2f - 8f, y + SLOT - 16f, Z_TEXT);
            names[i].setLocalTranslation(x + TEXT_PAD, y + 18f, Z_TEXT);
            counts[i].setLocalTranslation(x + SLOT - 34f, y + SLOT - 6f, Z_TEXT);
        }
        float panelX = gridLeft + gridWidth + PANEL_GAP;
        resize(details, DETAILS_WIDTH, gridHeight, panelX, gridTop - gridHeight, -0.05f);
        detailName.setLocalTranslation(panelX + 14f, gridTop - 14f, Z_TEXT);
        detailMeta.setLocalTranslation(panelX + 14f, gridTop - 60f, Z_TEXT);
        detailBody.setBox(new Rectangle(0f, 0f, DETAILS_WIDTH - 28f, gridHeight - 120f));
        detailBody.setLocalTranslation(panelX + 14f, gridTop - 100f, Z_TEXT);
        hint.setLocalTranslation(gridLeft, gridTop - gridHeight - 30f, Z_TEXT);
        status.setLocalTranslation(gridLeft, gridTop - gridHeight - 64f, Z_TEXT);
    }

    /**
     * Redraws from the inventory's current contents.
     *
     * @param inventory what to show
     * @param cursor the slot under the cursor
     * @param held the slot picked up and waiting to be put down, or -1
     * @param message a line of feedback, or empty
     */
    public void refresh(Inventory inventory, int cursor, int held, String message) {
        weight.setText(String.format(Locale.ROOT, "Weight carried: %.1f kg      Free slots: %d",
                inventory.totalWeight(), inventory.emptySlots()));
        status.setText(message);
        for (int i = 0; i < slotCount; i++) {
            Optional<ItemStack> stack = inventory.slot(i);
            frames[i].getMaterial().setColor("Color", i == held ? FRAME_HELD : i == cursor ? FRAME_CURSOR : FRAME_NONE);
            if (stack.isEmpty()) {
                backs[i].setMaterial(emptyMaterial);
                icons[i].setCullHint(Spatial.CullHint.Always);
                letters[i].setText("");
                names[i].setText("");
                counts[i].setText("");
                continue;
            }
            ItemDefinition definition = inventory.definitionOf(stack.get());
            backs[i].setMaterial(material(SLOT_COLOR));
            icons[i].setCullHint(Spatial.CullHint.Inherit);
            icons[i].setMaterial(material(categoryColor(definition.category())));
            letters[i].setText(definition.name().substring(0, 1).toUpperCase(Locale.ROOT));
            names[i].setText(shorten(definition.name()));
            counts[i].setText(stack.get().quantity() > 1 ? "x" + stack.get().quantity() : "");
        }
        showDetails(inventory, cursor);
    }

    /**
     * @param x screen x
     * @param y screen y
     * @return the slot under the point, or -1
     */
    public int slotAt(float x, float y) {
        for (int i = 0; i < slotCount; i++) {
            float left = slotX(i);
            float bottom = slotY(i);
            if (x >= left && x <= left + SLOT && y >= bottom && y <= bottom + SLOT) {
                return i;
            }
        }
        return -1;
    }

    /** @return the number of columns in the grid */
    public int columns() {
        return COLUMNS;
    }

    private void showDetails(Inventory inventory, int cursor) {
        Optional<ItemStack> stack = inventory.slot(cursor);
        if (stack.isEmpty()) {
            detailName.setText("Empty slot");
            detailMeta.setText("Slot " + (cursor + 1));
            detailBody.setText("Nothing here. Pick up an item and put it down here to move it.");
            return;
        }
        ItemDefinition definition = inventory.definitionOf(stack.get());
        detailName.setText(definition.name());
        detailMeta.setText(String.format(Locale.ROOT, "%s   x%d   %.1f kg each%s",
                definition.category().name().charAt(0) + definition.category().name().substring(1).toLowerCase(Locale.ROOT),
                stack.get().quantity(), definition.weight(),
                definition.canPlace() ? "   Can be placed" : ""));
        detailBody.setText(definition.description());
    }

    private float slotX(int index) {
        return gridLeft + (index % COLUMNS) * (SLOT + GAP);
    }

    private float slotY(int index) {
        return gridTop - SLOT - (index / COLUMNS) * (SLOT + GAP);
    }

    private static String shorten(String name) {
        return name.length() <= NAME_CHARS ? name : name.substring(0, NAME_CHARS - 1) + ".";
    }

    /** The icon colour for each item category; shared with the hotbar so both look the same. */
    static ColorRGBA categoryColor(ItemCategory category) {
        return switch (category) {
            case MATERIAL -> new ColorRGBA(0.62f, 0.50f, 0.32f, 1f);
            case FOOD -> new ColorRGBA(0.72f, 0.42f, 0.30f, 1f);
            case CONTAINER -> new ColorRGBA(0.35f, 0.55f, 0.78f, 1f);
            case SUPPLY -> new ColorRGBA(0.40f, 0.70f, 0.50f, 1f);
            case TOOL -> new ColorRGBA(0.70f, 0.70f, 0.76f, 1f);
        };
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

    private static BitmapText text(BitmapFont font, float scale, ColorRGBA color) {
        BitmapText text = new BitmapText(font);
        text.setLocalScale(scale);
        text.setColor(color);
        return text;
    }

    private static void resize(Geometry geometry, float width, float height, float x, float y, float z) {
        geometry.setMesh(new Quad(width, height));
        geometry.setLocalTranslation(x, y, z);
    }
}
