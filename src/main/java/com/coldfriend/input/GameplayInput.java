package com.coldfriend.input;

import com.coldfriend.player.PlayerInput;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.controls.Trigger;
import java.util.function.BooleanSupplier;

/**
 * Turns the keyboard and mouse into {@link PlayerInput} and one-off requests (interact, pause, inventory, drop,
 * place, rotate, confirm, cancel, choose a slot) while the player is in the world. It owns its own mappings,
 * registers them in {@link #attach()} and removes every one in {@link #detach()}, so leaving the world leaves
 * nothing behind. The mouse cursor is hidden while attached.
 */
public final class GameplayInput {

    private static final String PREFIX = "play.";
    private static final String FORWARD = PREFIX + "forward";
    private static final String BACK = PREFIX + "back";
    private static final String LEFT = PREFIX + "left";
    private static final String RIGHT = PREFIX + "right";
    private static final String SPRINT = PREFIX + "sprint";
    private static final String INTERACT = PREFIX + "interact";
    private static final String PAUSE = PREFIX + "pause";
    private static final String INVENTORY = PREFIX + "inventory";
    private static final String DROP = PREFIX + "drop";
    private static final String PLACE = PREFIX + "place";
    private static final String ROTATE = PREFIX + "rotate";
    private static final String CONFIRM = PREFIX + "confirm";
    private static final String CANCEL = PREFIX + "cancel";
    private static final String SLOT = PREFIX + "slot.";
    private static final String LOOK_LEFT = PREFIX + "look.left";
    private static final String LOOK_RIGHT = PREFIX + "look.right";
    private static final String LOOK_UP = PREFIX + "look.up";
    private static final String LOOK_DOWN = PREFIX + "look.down";
    private static final String WHEEL_UP = PREFIX + "wheel.up";
    private static final String WHEEL_DOWN = PREFIX + "wheel.down";
    private static final int SLOT_KEYS = 9;
    private static final int[] DIGIT_KEYS = {KeyInput.KEY_1, KeyInput.KEY_2, KeyInput.KEY_3, KeyInput.KEY_4,
        KeyInput.KEY_5, KeyInput.KEY_6, KeyInput.KEY_7, KeyInput.KEY_8, KeyInput.KEY_9};
    private static final String[] ACTION_NAMES = {FORWARD, BACK, LEFT, RIGHT, SPRINT, INTERACT, PAUSE, INVENTORY,
        DROP, PLACE, ROTATE, CONFIRM, CANCEL};
    private static final String[] ANALOG_NAMES = {LOOK_LEFT, LOOK_RIGHT, LOOK_UP, LOOK_DOWN, WHEEL_UP, WHEEL_DOWN};
    /** Radians of turning per unit of mouse movement the engine reports. */
    private static final float LOOK_SENSITIVITY = 2f;
    /** Radians a placed object turns per notch of the mouse wheel. */
    private static final float WHEEL_STEP = 0.35f;

    private final InputManager inputManager;
    private final ActionListener actions = this::onAction;
    private final AnalogListener analogs = this::onAnalog;
    private boolean forward;
    private boolean back;
    private boolean left;
    private boolean right;
    private boolean sprint;
    private boolean interactPressed;
    private boolean pausePressed;
    private boolean inventoryPressed;
    private boolean dropPressed;
    private boolean placePressed;
    private boolean rotatePressed;
    private boolean confirmPressed;
    private boolean cancelPressed;
    private int slotPressed = -1;
    private float turn;
    private float look;
    private float wheel;
    private boolean attached;

    /** @param inputManager the engine input manager that mappings are registered with */
    public GameplayInput(InputManager inputManager) {
        this.inputManager = inputManager;
    }

    /** Registers the mappings and hides the cursor. */
    public void attach() {
        if (attached) {
            return;
        }
        map(FORWARD, new KeyTrigger(KeyInput.KEY_W), new KeyTrigger(KeyInput.KEY_UP));
        map(BACK, new KeyTrigger(KeyInput.KEY_S), new KeyTrigger(KeyInput.KEY_DOWN));
        map(LEFT, new KeyTrigger(KeyInput.KEY_A), new KeyTrigger(KeyInput.KEY_LEFT));
        map(RIGHT, new KeyTrigger(KeyInput.KEY_D), new KeyTrigger(KeyInput.KEY_RIGHT));
        map(SPRINT, new KeyTrigger(KeyInput.KEY_LSHIFT), new KeyTrigger(KeyInput.KEY_RSHIFT));
        map(INTERACT, new KeyTrigger(KeyInput.KEY_E));
        map(PAUSE, new KeyTrigger(KeyInput.KEY_ESCAPE));
        map(INVENTORY, new KeyTrigger(KeyInput.KEY_I), new KeyTrigger(KeyInput.KEY_TAB));
        map(DROP, new KeyTrigger(KeyInput.KEY_Q));
        map(PLACE, new KeyTrigger(KeyInput.KEY_P));
        map(ROTATE, new KeyTrigger(KeyInput.KEY_R));
        map(CONFIRM, new KeyTrigger(KeyInput.KEY_RETURN), new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        map(CANCEL, new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        for (int i = 0; i < SLOT_KEYS; i++) {
            map(SLOT + i, new KeyTrigger(DIGIT_KEYS[i]));
        }
        map(LOOK_LEFT, new MouseAxisTrigger(MouseInput.AXIS_X, true));
        map(LOOK_RIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, false));
        map(LOOK_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        map(LOOK_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        map(WHEEL_UP, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        map(WHEEL_DOWN, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        inputManager.addListener(actions, ACTION_NAMES);
        for (int i = 0; i < SLOT_KEYS; i++) {
            inputManager.addListener(actions, SLOT + i);
        }
        inputManager.addListener(analogs, ANALOG_NAMES);
        inputManager.setCursorVisible(false);
        attached = true;
    }

    /** Removes every mapping this class registered and shows the cursor again. */
    public void detach() {
        if (!attached) {
            return;
        }
        inputManager.removeListener(actions);
        inputManager.removeListener(analogs);
        for (String name : ACTION_NAMES) {
            inputManager.deleteMapping(name);
        }
        for (String name : ANALOG_NAMES) {
            inputManager.deleteMapping(name);
        }
        for (int i = 0; i < SLOT_KEYS; i++) {
            inputManager.deleteMapping(SLOT + i);
        }
        inputManager.setCursorVisible(true);
        clear();
        attached = false;
    }

    /** Forgets held keys and unread input, for example after a pause. */
    public void clear() {
        forward = false;
        back = false;
        left = false;
        right = false;
        sprint = false;
        interactPressed = false;
        pausePressed = false;
        inventoryPressed = false;
        dropPressed = false;
        placePressed = false;
        rotatePressed = false;
        confirmPressed = false;
        cancelPressed = false;
        slotPressed = -1;
        turn = 0f;
        look = 0f;
        wheel = 0f;
    }

    /** @return what the player asked for since the last call; mouse movement is used up by reading it */
    public PlayerInput consume() {
        PlayerInput input = new PlayerInput(axis(forward, back), axis(right, left), turn, look, sprint);
        turn = 0f;
        look = 0f;
        return input;
    }

    /** @return true while a sprint key is held */
    public boolean sprinting() {
        return sprint;
    }

    /** @return how far the mouse wheel asked an object to turn since the last call, in radians */
    public float consumeWheelTurn() {
        float amount = wheel * WHEEL_STEP;
        wheel = 0f;
        return amount;
    }

    /** @return true once each time the interact key was pressed since the last call */
    public boolean consumeInteract() {
        return take(() -> interactPressed, () -> interactPressed = false);
    }

    /** @return true once each time the pause key was pressed since the last call */
    public boolean consumePause() {
        return take(() -> pausePressed, () -> pausePressed = false);
    }

    /** @return true once each time the inventory key was pressed since the last call */
    public boolean consumeInventory() {
        return take(() -> inventoryPressed, () -> inventoryPressed = false);
    }

    /** @return true once each time the drop key was pressed since the last call */
    public boolean consumeDrop() {
        return take(() -> dropPressed, () -> dropPressed = false);
    }

    /** @return true once each time the place key was pressed since the last call */
    public boolean consumePlace() {
        return take(() -> placePressed, () -> placePressed = false);
    }

    /** @return true once each time the rotate key was pressed since the last call */
    public boolean consumeRotate() {
        return take(() -> rotatePressed, () -> rotatePressed = false);
    }

    /** @return true once each time confirm (Enter or left mouse button) was pressed since the last call */
    public boolean consumeConfirm() {
        return take(() -> confirmPressed, () -> confirmPressed = false);
    }

    /** @return true once each time cancel (right mouse button) was pressed since the last call */
    public boolean consumeCancel() {
        return take(() -> cancelPressed, () -> cancelPressed = false);
    }

    /** @return the inventory slot (from 0) chosen with a number key since the last call, or -1 if none */
    public int consumeSlot() {
        int slot = slotPressed;
        slotPressed = -1;
        return slot;
    }

    private static boolean take(BooleanSupplier flag, Runnable reset) {
        boolean value = flag.getAsBoolean();
        reset.run();
        return value;
    }

    private void map(String name, Trigger... triggers) {
        inputManager.addMapping(name, triggers);
    }

    private static float axis(boolean positive, boolean negative) {
        return (positive ? 1f : 0f) - (negative ? 1f : 0f);
    }

    private void onAction(String name, boolean pressed, float tpf) {
        if (name.startsWith(SLOT)) {
            if (pressed) {
                slotPressed = Integer.parseInt(name.substring(SLOT.length()));
            }
            return;
        }
        switch (name) {
            case FORWARD -> forward = pressed;
            case BACK -> back = pressed;
            case LEFT -> left = pressed;
            case RIGHT -> right = pressed;
            case SPRINT -> sprint = pressed;
            case INTERACT -> interactPressed |= pressed;
            case PAUSE -> pausePressed |= pressed;
            case INVENTORY -> inventoryPressed |= pressed;
            case DROP -> dropPressed |= pressed;
            case PLACE -> placePressed |= pressed;
            case ROTATE -> rotatePressed |= pressed;
            case CONFIRM -> confirmPressed |= pressed;
            case CANCEL -> cancelPressed |= pressed;
            default -> {
                // Not one of ours.
            }
        }
    }

    private void onAnalog(String name, float value, float tpf) {
        float amount = value * LOOK_SENSITIVITY;
        switch (name) {
            case LOOK_LEFT -> turn -= amount;
            case LOOK_RIGHT -> turn += amount;
            case LOOK_UP -> look += amount;
            case LOOK_DOWN -> look -= amount;
            case WHEEL_UP -> wheel += value;
            case WHEEL_DOWN -> wheel -= value;
            default -> {
                // Not one of ours.
            }
        }
    }
}
