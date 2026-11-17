package com.coldfriend.state;

import com.coldfriend.input.GameAction;
import com.coldfriend.input.InputController;
import com.coldfriend.interaction.ActionResult;
import com.coldfriend.inventory.Inventory;
import com.coldfriend.item.ItemStack;
import com.coldfriend.player.PlayerId;
import com.coldfriend.ui.InventoryView;
import com.coldfriend.world.WorldSimulation;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.math.Vector2f;
import java.util.Optional;

/**
 * The inventory screen. While it is open the game stands still and this screen owns the keyboard and mouse, so
 * navigating cannot move or turn the player. It shows the simulation's inventory and sends changes back to the
 * simulation; it keeps only a cursor and a picked-up slot of its own.
 */
public final class InventoryState extends BaseAppState {

    private static final String FONT_PATH = "Interface/Fonts/Default.fnt";
    private static final int NO_SLOT = -1;

    private final ScreenNavigator navigator;
    private final InputController input;
    private final WorldSimulation simulation;
    private final PlayerId player;
    private InventoryView view;
    private int cursor;
    private int held = NO_SLOT;
    private String message = "";
    private float shownWidth = -1f;
    private float shownHeight = -1f;
    private Vector2f lastMouse = new Vector2f(-1f, -1f);

    /**
     * @param navigator used to close the screen and start placing
     * @param input the input controller this screen binds to while enabled
     * @param simulation the simulation that owns the inventory
     * @param player whose inventory to show
     */
    public InventoryState(ScreenNavigator navigator, InputController input, WorldSimulation simulation,
            PlayerId player) {
        this.navigator = navigator;
        this.input = input;
        this.simulation = simulation;
        this.player = player;
    }

    @Override
    protected void initialize(Application app) {
        BitmapFont font = app.getAssetManager().loadFont(FONT_PATH);
        view = new InventoryView(app.getAssetManager(), font, inventory().size());
        cursor = inventory().selected();
    }

    @Override
    protected void onEnable() {
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(view.root());
        input.bind(this, this::onAction);
        relayout();
        redraw();
    }

    @Override
    protected void onDisable() {
        input.release(this);
        view.root().removeFromParent();
    }

    @Override
    protected void cleanup(Application app) {
        view = null;
    }

    @Override
    public void update(float tpf) {
        if (!isEnabled()) {
            return;
        }
        float width = getApplication().getCamera().getWidth();
        float height = getApplication().getCamera().getHeight();
        if (width != shownWidth || height != shownHeight) {
            relayout();
        }
        Vector2f mouse = input.cursorPosition();
        if (!mouse.equals(lastMouse)) {
            lastMouse = mouse;
            int slot = view.slotAt(mouse.x, mouse.y);
            if (slot >= 0 && slot != cursor) {
                cursor = slot;
                redraw();
            }
        }
    }

    private void onAction(GameAction action) {
        switch (action) {
            case MENU_UP -> moveCursor(-view.columns());
            case MENU_DOWN -> moveCursor(view.columns());
            case MENU_LEFT -> moveCursor(-1);
            case MENU_RIGHT -> moveCursor(1);
            case MENU_CONFIRM -> activate(cursor);
            case MENU_CLICK -> clickAt(input.cursorPosition());
            case INVENTORY_DROP -> drop(false);
            case INVENTORY_DROP_ALL -> drop(true);
            case INVENTORY_PLACE -> place();
            case MENU_BACK, INVENTORY_TOGGLE -> navigator.closeInventory();
        }
    }

    private void moveCursor(int delta) {
        int next = cursor + delta;
        if (next >= 0 && next < inventory().size()) {
            cursor = next;
            inventory().select(cursor);
        }
        redraw();
    }

    private void clickAt(Vector2f at) {
        int slot = view.slotAt(at.x, at.y);
        if (slot >= 0) {
            cursor = slot;
            activate(slot);
        }
    }

    /** First activation picks a stack up; the second puts it down in the chosen slot, merging or swapping. */
    private void activate(int slot) {
        inventory().select(slot);
        if (held == NO_SLOT) {
            if (inventory().slot(slot).isPresent()) {
                held = slot;
                message = "Picked up. Choose a slot to put it in.";
            } else {
                message = "That slot is empty.";
            }
        } else if (held == slot) {
            held = NO_SLOT;
            message = "";
        } else {
            Inventory.MoveResult result = simulation.moveInventoryItem(player, held, slot);
            message = switch (result) {
                case MOVED -> "Moved.";
                case MERGED -> "Stacked.";
                case SWAPPED -> "Swapped.";
                case UNCHANGED -> "Nothing changed; that stack is full.";
            };
            held = NO_SLOT;
        }
        redraw();
    }

    private void drop(boolean all) {
        Optional<ItemStack> stack = inventory().slot(cursor);
        if (stack.isEmpty()) {
            message = "Nothing to drop.";
        } else {
            ActionResult result = simulation.dropItem(player, cursor, all ? stack.get().quantity() : 1);
            message = result.message();
        }
        held = NO_SLOT;
        redraw();
    }

    private void place() {
        Optional<ItemStack> stack = inventory().slot(cursor);
        if (stack.isEmpty()) {
            message = "Nothing to place.";
        } else if (!simulation.items().get(stack.get().item()).canPlace()) {
            message = "That cannot be placed.";
        } else {
            inventory().select(cursor);
            navigator.beginPlacement(cursor);
            return;
        }
        redraw();
    }

    private Inventory inventory() {
        return simulation.inventory(player);
    }

    private void relayout() {
        shownWidth = getApplication().getCamera().getWidth();
        shownHeight = getApplication().getCamera().getHeight();
        view.layout(shownWidth, shownHeight);
        redraw();
    }

    private void redraw() {
        view.refresh(inventory(), cursor, held, message);
    }
}
