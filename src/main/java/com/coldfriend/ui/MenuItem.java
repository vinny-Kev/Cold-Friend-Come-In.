package com.coldfriend.ui;

import java.util.function.IntConsumer;

/**
 * One row of a menu. An item may run an action when confirmed, change a value when adjusted left or right, or both.
 */
public final class MenuItem {

    private String label;
    private final boolean enabled;
    private final Runnable onConfirm;
    private final IntConsumer onAdjust;

    private MenuItem(String label, boolean enabled, Runnable onConfirm, IntConsumer onAdjust) {
        this.label = label;
        this.enabled = enabled;
        this.onConfirm = onConfirm;
        this.onAdjust = onAdjust;
    }

    /**
     * An item that runs an action when confirmed.
     *
     * @param label text shown for the item
     * @param onConfirm action to run
     * @return the item
     */
    public static MenuItem action(String label, Runnable onConfirm) {
        return new MenuItem(label, true, onConfirm, null);
    }

    /**
     * An item that cannot be selected or activated; it is drawn dimmed.
     *
     * @param label text shown for the item
     * @return the item
     */
    public static MenuItem disabled(String label) {
        return new MenuItem(label, false, null, null);
    }

    /**
     * An item whose value is changed by a signed step: left or right adjusts by -1 or +1, and confirming adds +1.
     *
     * @param label initial text shown for the item
     * @param onAdjust receives -1 or +1
     * @return the item
     */
    public static MenuItem value(String label, IntConsumer onAdjust) {
        return value(label, onAdjust, () -> onAdjust.accept(1));
    }

    /**
     * A value item whose confirm action is its own, for values that should not simply step up on confirm.
     *
     * @param label initial text shown for the item
     * @param onAdjust receives -1 or +1 from left and right
     * @param onConfirm runs when the item is confirmed or clicked
     * @return the item
     */
    public static MenuItem value(String label, IntConsumer onAdjust, Runnable onConfirm) {
        return new MenuItem(label, true, onConfirm, onAdjust);
    }

    public String label() {
        return label;
    }

    /**
     * Changes the displayed text, for example to show a current value.
     *
     * @param newLabel the new text
     */
    public void setLabel(String newLabel) {
        label = newLabel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    void confirm() {
        if (enabled && onConfirm != null) {
            onConfirm.run();
        }
    }

    void adjust(int step) {
        if (enabled && onAdjust != null) {
            onAdjust.accept(step);
        }
    }
}
