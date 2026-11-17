package com.coldfriend.ui;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws a {@link MenuModel} as centred text in a GUI node and answers hit tests for mouse input.
 * Coordinates use the engine's screen space: origin at the bottom-left, y increasing upwards.
 */
public final class MenuView {

    private static final String SELECTED_PREFIX = "> ";
    private static final String PLAIN_PREFIX = "  ";
    private static final ColorRGBA SELECTED_COLOR = new ColorRGBA(1.0f, 0.85f, 0.45f, 1f);
    private static final ColorRGBA ENABLED_COLOR = new ColorRGBA(0.9f, 0.92f, 0.95f, 1f);
    private static final ColorRGBA DISABLED_COLOR = new ColorRGBA(0.4f, 0.42f, 0.46f, 1f);

    private final BitmapFont font;
    private final float textScale;
    private final float lineSpacing;
    private final Node node = new Node("menu-view");
    private final List<BitmapText> texts = new ArrayList<>();
    private MenuModel model;
    private float anchorX;
    private float topY;

    /**
     * @param font font used for menu text
     * @param textScale uniform scale applied to every menu line
     * @param lineSpacing vertical distance between rows, in screen pixels
     */
    public MenuView(BitmapFont font, float textScale, float lineSpacing) {
        this.font = font;
        this.textScale = textScale;
        this.lineSpacing = lineSpacing;
    }

    /**
     * Sets the model to draw and rebuilds the text nodes for its rows.
     *
     * @param newModel the menu model
     */
    public void setModel(MenuModel newModel) {
        this.model = newModel;
        texts.clear();
        node.detachAllChildren();
        for (int i = 0; i < newModel.items().size(); i++) {
            BitmapText text = new BitmapText(font);
            text.setLocalScale(textScale);
            texts.add(text);
            node.attachChild(text);
        }
        refresh();
    }

    /**
     * Attaches the view's node under {@code parent}.
     *
     * @param parent the node to attach to
     */
    public void attach(Node parent) {
        parent.attachChild(node);
    }

    /** Removes the view from its parent. */
    public void detach() {
        node.removeFromParent();
    }

    /**
     * Repositions the menu, centred horizontally with the first row {@code topMargin} below {@code screenHeight}.
     *
     * @param screenWidth width of the drawing area
     * @param screenHeight height of the drawing area
     * @param topMargin distance from the top edge to the first row
     */
    public void layout(float screenWidth, float screenHeight, float topMargin) {
        anchorX = screenWidth / 2f;
        topY = screenHeight - topMargin;
        positionTexts();
    }

    /** Re-applies row text and colours from the model and recentres the rows. */
    public void refresh() {
        if (model == null) {
            return;
        }
        for (int i = 0; i < texts.size(); i++) {
            MenuItem item = model.items().get(i);
            boolean selected = i == model.selectedIndex();
            BitmapText text = texts.get(i);
            text.setText((selected ? SELECTED_PREFIX : PLAIN_PREFIX) + item.label());
            text.setColor(colorFor(item, selected));
        }
        positionTexts();
    }

    /**
     * Returns the row under a screen point.
     *
     * @param x screen x in pixels, origin at the left
     * @param y screen y in pixels, origin at the bottom
     * @return the row index, or -1 if none
     */
    public int itemAt(float x, float y) {
        for (int i = 0; i < texts.size(); i++) {
            BitmapText text = texts.get(i);
            float left = text.getLocalTranslation().x;
            float top = text.getLocalTranslation().y;
            float width = text.getLineWidth() * textScale;
            float height = text.getLineHeight() * textScale;
            if (x >= left && x <= left + width && y <= top && y >= top - height) {
                return i;
            }
        }
        return -1;
    }

    private void positionTexts() {
        for (int i = 0; i < texts.size(); i++) {
            BitmapText text = texts.get(i);
            float width = text.getLineWidth() * textScale;
            text.setLocalTranslation(anchorX - width / 2f, topY - i * lineSpacing, 0f);
        }
    }

    private static ColorRGBA colorFor(MenuItem item, boolean selected) {
        if (!item.isEnabled()) {
            return DISABLED_COLOR;
        }
        return selected ? SELECTED_COLOR : ENABLED_COLOR;
    }
}
