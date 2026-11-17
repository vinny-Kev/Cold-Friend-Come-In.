package com.coldfriend.input;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.controls.Trigger;
import com.jme3.math.Vector2f;
import java.util.function.Consumer;

/**
 * Central input mapping. Translates physical input into {@link GameAction}s and forwards them to the
 * one screen that currently owns input. Ownership is claimed with {@link #bind} and released with
 * {@link #release}, so a screen that is being disabled cannot clear the handler of the screen that replaced it.
 */
public final class InputController {

    private static final String MAPPING_PREFIX = "action.";

    private final InputManager inputManager;
    private final ActionListener listener = this::onRawAction;
    private Object owner;
    private Consumer<GameAction> handler;

    /** @param inputManager the engine input manager that mappings are registered with */
    public InputController(InputManager inputManager) {
        this.inputManager = inputManager;
    }

    /** Registers all key and mouse mappings. Call once during application start-up. */
    public void attach() {
        String[] names = new String[GameAction.values().length];
        for (GameAction action : GameAction.values()) {
            String name = MAPPING_PREFIX + action.name();
            inputManager.addMapping(name, triggersFor(action));
            names[action.ordinal()] = name;
        }
        inputManager.addListener(listener, names);
    }

    /** Removes all mappings registered by {@link #attach()}. */
    public void detach() {
        inputManager.removeListener(listener);
        for (GameAction action : GameAction.values()) {
            inputManager.deleteMapping(MAPPING_PREFIX + action.name());
        }
        owner = null;
        handler = null;
    }

    /**
     * Makes {@code newOwner} the receiver of actions, replacing any previous owner.
     *
     * @param newOwner the screen claiming input
     * @param newHandler receives each pressed action
     */
    public void bind(Object newOwner, Consumer<GameAction> newHandler) {
        owner = newOwner;
        handler = newHandler;
    }

    /**
     * Releases input if {@code previousOwner} is still the owner; otherwise does nothing.
     *
     * @param previousOwner the screen releasing input
     */
    public void release(Object previousOwner) {
        if (owner == previousOwner) {
            owner = null;
            handler = null;
        }
    }

    /**
     * Current cursor position in screen pixels, origin at the bottom-left corner.
     *
     * @return a new vector holding the cursor position
     */
    public Vector2f cursorPosition() {
        return inputManager.getCursorPosition().clone();
    }

    private void onRawAction(String name, boolean pressed, float tpf) {
        if (!pressed || handler == null) {
            return;
        }
        handler.accept(GameAction.valueOf(name.substring(MAPPING_PREFIX.length())));
    }

    private static Trigger[] triggersFor(GameAction action) {
        return switch (action) {
            case MENU_UP -> new Trigger[] {new KeyTrigger(KeyInput.KEY_UP), new KeyTrigger(KeyInput.KEY_W)};
            case MENU_DOWN -> new Trigger[] {new KeyTrigger(KeyInput.KEY_DOWN), new KeyTrigger(KeyInput.KEY_S)};
            case MENU_LEFT -> new Trigger[] {new KeyTrigger(KeyInput.KEY_LEFT), new KeyTrigger(KeyInput.KEY_A)};
            case MENU_RIGHT -> new Trigger[] {new KeyTrigger(KeyInput.KEY_RIGHT), new KeyTrigger(KeyInput.KEY_D)};
            case MENU_CONFIRM -> new Trigger[] {
                new KeyTrigger(KeyInput.KEY_RETURN), new KeyTrigger(KeyInput.KEY_SPACE)};
            case MENU_BACK -> new Trigger[] {new KeyTrigger(KeyInput.KEY_ESCAPE)};
            case MENU_CLICK -> new Trigger[] {new MouseButtonTrigger(MouseInput.BUTTON_LEFT)};
            case INVENTORY_TOGGLE -> new Trigger[] {new KeyTrigger(KeyInput.KEY_I), new KeyTrigger(KeyInput.KEY_TAB)};
            case INVENTORY_DROP -> new Trigger[] {new KeyTrigger(KeyInput.KEY_Q)};
            case INVENTORY_DROP_ALL -> new Trigger[] {new KeyTrigger(KeyInput.KEY_X)};
            case INVENTORY_PLACE -> new Trigger[] {new KeyTrigger(KeyInput.KEY_P)};
        };
    }
}
