package com.coldfriend.state;

import com.coldfriend.input.GameAction;
import com.coldfriend.input.InputController;
import com.coldfriend.ui.MenuModel;
import com.coldfriend.ui.MenuView;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;

/**
 * Shared behaviour of the menu screens: drawing a {@link MenuModel}, following the mouse, re-laying out on
 * window resize, and taking and releasing input. Subclasses supply their own text and actions.
 */
abstract class MenuScreenState extends BaseAppState {

    protected final InputController input;
    protected final Node screenNode;
    /** Set by the subclass constructor; it cannot be passed in here because its items refer to the subclass. */
    protected MenuModel model;
    protected MenuView menuView;
    private float laidOutWidth = -1f;
    private float laidOutHeight = -1f;
    private Vector2f lastCursor = new Vector2f(-1f, -1f);

    MenuScreenState(String nodeName, InputController input) {
        this.input = input;
        this.screenNode = new Node(nodeName);
    }

    /** Creates the menu view. Call from initialize, after loading the font. */
    protected void createMenuView(BitmapFont font, float itemScale, float lineSpacing) {
        menuView = new MenuView(font, itemScale, lineSpacing);
        menuView.setModel(model);
    }

    /** Positions this screen's text and menu rows for a drawing area of the given size. */
    protected abstract void layoutScreen(float width, float height);

    /** Handles one input action while this screen owns input. */
    protected abstract void onAction(GameAction action);

    /** Called after a row ran or changed a value. Subclasses that show values override this. */
    protected void onValuesChanged() {
        menuView.refresh();
    }

    /** Draws the screen and takes input. Call from onEnable, after refreshing any screen-specific text. */
    protected void showScreen() {
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(screenNode);
        menuView.attach(screenNode);
        input.bind(this, this::onAction);
        relayout();
    }

    @Override
    protected void cleanup(Application app) {
        menuView = null;
    }

    @Override
    protected void onDisable() {
        input.release(this);
        screenNode.removeFromParent();
        menuView.detach();
    }

    @Override
    public void update(float tpf) {
        if (!isEnabled()) {
            return;
        }
        if (screenSizeChanged()) {
            relayout();
        }
        Vector2f cursor = input.cursorPosition();
        if (!cursor.equals(lastCursor)) {
            lastCursor = cursor;
            int hovered = menuView.itemAt(cursor.x, cursor.y);
            if (hovered >= 0) {
                model.select(hovered);
                menuView.refresh();
            }
        }
    }

    protected void moveSelection(int delta) {
        model.moveSelection(delta);
        menuView.refresh();
    }

    protected void adjustSelected(int step) {
        model.adjustSelected(step);
        onValuesChanged();
    }

    protected void confirmSelected() {
        model.confirmSelected();
        onValuesChanged();
    }

    protected void activateAt(Vector2f cursor) {
        int index = menuView.itemAt(cursor.x, cursor.y);
        if (index >= 0) {
            model.activate(index);
            onValuesChanged();
        }
    }

    protected void relayout() {
        float width = getApplication().getCamera().getWidth();
        float height = getApplication().getCamera().getHeight();
        laidOutWidth = width;
        laidOutHeight = height;
        layoutScreen(width, height);
    }

    private boolean screenSizeChanged() {
        float width = getApplication().getCamera().getWidth();
        float height = getApplication().getCamera().getHeight();
        return width != laidOutWidth || height != laidOutHeight;
    }
}
