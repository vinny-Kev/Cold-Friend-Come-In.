package com.coldfriend.assets;

import com.jme3.asset.AssetManager;
import com.jme3.texture.Texture;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the 2D item icons listed in the catalogue as {@code ICON.<key> = path}. The key is an item's icon key (see
 * {@code item.<id>.icon} in items.properties). A key with no icon, or an icon that fails to load, gets no icon, so the
 * hotbar falls back to a letter.
 */
public final class IconCatalog {

    private static final Logger LOG = LoggerFactory.getLogger(IconCatalog.class);
    private static final String PREFIX = "ICON.";

    private final Map<String, Texture> icons = new HashMap<>();

    /**
     * @param assets the engine asset manager, with the assets folder registered
     * @param catalogFile the catalogue file that lists the icons
     * @throws IOException if the catalogue file cannot be read
     */
    public IconCatalog(AssetManager assets, Path catalogFile) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(catalogFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(PREFIX)) {
                String path = properties.getProperty(key).trim();
                try {
                    icons.put(key.substring(PREFIX.length()), assets.loadTexture(path));
                } catch (RuntimeException e) {
                    LOG.warn("Could not load icon {}; the item will show a letter", path, e);
                }
            }
        }
    }

    /**
     * @param key an item's icon key
     * @return the icon, or empty if there is none
     */
    public Optional<Texture> icon(String key) {
        return Optional.ofNullable(icons.get(key));
    }
}
