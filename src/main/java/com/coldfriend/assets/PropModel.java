package com.coldfriend.assets;

import com.jme3.scene.Spatial;

/**
 * A model ready to be drawn many times. The template is shared, never attached to a scene itself; draw an
 * instance by cloning it without cloning its materials.
 *
 * @param template the model, with materials set up for hardware instancing
 * @param baseScale scale applied on top of each placement's own scale
 */
public record PropModel(Spatial template, float baseScale) {
}
