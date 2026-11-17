package com.coldfriend.core;

import com.jme3.system.AppSettings;

/** Translates {@link GameSettings} into the engine's window and display configuration. */
public final class AppSettingsFactory {

    private AppSettingsFactory() {
    }

    /**
     * Creates engine settings for the given player settings.
     *
     * @param title window title
     * @param settings player settings
     * @return engine settings ready to pass to the application
     */
    public static AppSettings create(String title, GameSettings settings) {
        AppSettings appSettings = new AppSettings(true);
        appSettings.setTitle(title);
        appSettings.setWidth(settings.resolution().width());
        appSettings.setHeight(settings.resolution().height());
        appSettings.setFullscreen(settings.fullscreen());
        appSettings.setVSync(settings.vsync());
        return appSettings;
    }
}
