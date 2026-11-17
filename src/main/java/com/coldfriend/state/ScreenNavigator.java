package com.coldfriend.state;

import com.coldfriend.core.GameSettings;

/**
 * Operations screens may request from the application. Implemented by the application so screens do not
 * depend on it directly.
 */
public interface ScreenNavigator {

    /** @return the settings currently in effect */
    GameSettings currentSettings();

    /** Shows the main menu in place of the settings screen. */
    void showMainMenu();

    /** Shows the settings screen in place of the main menu. */
    void showSettings();

    /**
     * Applies the settings for this session, restarts the display if resolution, fullscreen or vsync changed,
     * and saves them to disk.
     *
     * @param updated the settings chosen by the player
     * @return false if the settings could not be saved to disk (they still apply for this session)
     */
    boolean saveAndApplySettings(GameSettings updated);

    /** Closes the application. */
    void quit();
}
