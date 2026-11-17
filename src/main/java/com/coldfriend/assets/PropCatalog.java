package com.coldfriend.assets;

/** Turns a {@link PropKind} or an item model key into something drawable. The only place that knows which model files exist. */
public interface PropCatalog {

    /**
     * @param kind what to draw
     * @param variant any number; the same number always picks the same model for a kind
     * @return a model for the kind, set up for instanced drawing
     */
    PropModel model(PropKind kind, int variant);

    /**
     * A model for an item lying or placed in the world. Items are few and move, so these are ordinary models, not
     * instanced ones. An item with no model gets a bright magenta cube so the gap is obvious.
     *
     * @param modelKey the item definition model key, never a file name
     * @return a model for the item
     */
    PropModel itemModel(String modelKey);
}
