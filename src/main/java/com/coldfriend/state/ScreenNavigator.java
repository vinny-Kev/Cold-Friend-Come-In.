package com.coldfriend.state;

import com.coldfriend.core.GameSettings;

/**
 * Operations screens may request from the application. Implemented by the application so screens do not
 * depend on it directly.
 */
public interface ScreenNavigator {

    /** @return the settings currently in effect */
    GameSettings currentSettings();

    /** Closes the settings screen and returns to the menu it was opened from: the main menu or the pause menu. */
    void showMainMenu();

    /** Shows the settings screen in place of the main menu or the pause menu. */
    void showSettings();

    /** Generates a new world with a fresh or configured seed and starts playing in it. */
    void startNewGame();

    /** Opens the inventory screen over the world and stops the game while it is open. */
    void openInventory();

    /** Closes the inventory screen and carries on playing. */
    void closeInventory();

    /**
     * Closes the inventory and starts placing an item in the world.
     *
     * @param slot the inventory slot holding the item to place
     */
    void beginPlacement(int slot);

    /** Pauses the game and shows the pause menu over the world. */
    void pauseGame();

    /** Closes the pause menu and carries on playing. */
    void resumeGame();

    /** Leaves the world being played and returns to the main menu. */
    void leaveWorld();

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
