package com.coldfriend.ui;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;

/**
 * Text with a dark drop shadow, so it stays readable on bright snow and on dark interiors alike. Behaves like one
 * piece of text: set its text, colour and position and the shadow follows.
 */
public final class ShadowText extends Node {

    private static final ColorRGBA SHADOW = new ColorRGBA(0f, 0f, 0f, 0.85f);
    private static final float OFFSET = 2f;
    private static final float SHADOW_DEPTH = -0.01f;

    private final BitmapText text;
    private final BitmapText shadow;
    private final float scale;

    /**
     * @param font the font
     * @param scale text scale
     * @param color text colour
     */
    public ShadowText(BitmapFont font, float scale, ColorRGBA color) {
        super("shadow-text");
        this.scale = scale;
        text = new BitmapText(font);
        text.setLocalScale(scale);
        text.setColor(color);
        shadow = new BitmapText(font);
        shadow.setLocalScale(scale);
        shadow.setColor(SHADOW);
        shadow.setLocalTranslation(OFFSET, -OFFSET, SHADOW_DEPTH);
        attachChild(shadow);
        attachChild(text);
    }

    /** @param value the text to show; empty hides it */
    public void setText(String value) {
        text.setText(value);
        shadow.setText(value);
    }

    /** @param color the colour of the text itself; the shadow stays dark */
    public void setColor(ColorRGBA color) {
        text.setColor(color);
    }

    /** @return the width of the line in pixels, after scaling */
    public float lineWidth() {
        return text.getLineWidth() * scale;
    }

    /** @return the height of a line in pixels, after scaling */
    public float lineHeight() {
        return text.getLineHeight() * scale;
    }
}
