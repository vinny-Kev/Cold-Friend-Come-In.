package com.coldfriend.input;

/** Logical actions the game responds to. Physical keys and buttons are mapped in {@link InputController}. */
public enum GameAction {
    MENU_UP,
    MENU_DOWN,
    MENU_LEFT,
    MENU_RIGHT,
    MENU_CONFIRM,
    MENU_BACK,
    MENU_CLICK,
    /** Closes the inventory screen (the same keys that open it). */
    INVENTORY_TOGGLE,
    /** Drops one of the selected item. */
    INVENTORY_DROP,
    /** Drops the whole selected stack. */
    INVENTORY_DROP_ALL,
    /** Starts placing the selected item in the world. */
    INVENTORY_PLACE
}
