package com.coldfriend.state;

import com.coldfriend.input.GameAction;
import com.coldfriend.input.InputController;
import com.coldfriend.ui.MenuItem;
import com.coldfriend.ui.MenuModel;
import com.jme3.app.Application;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The title screen with New Game, Continue (disabled until saving exists), Settings and Quit. */
public final class MainMenuState extends MenuScreenState {

    private static final Logger LOG = LoggerFactory.getLogger(MainMenuState.class);
    private static final String FONT_PATH = "Interface/Fonts/Default.fnt";
    private static final String TITLE = "COLD FRIEND, COME IN.";
    private static final String NEW_GAME_STATUS = "Gameplay is not implemented yet.";
    private static final float TITLE_SCALE = 4f;
    private static final float ITEM_SCALE = 2f;
    private static final float STATUS_SCALE = 1.5f;
    private static final float TITLE_TOP_MARGIN = 90f;
    private static final float MENU_TOP_MARGIN = 280f;
    private static final float ITEM_SPACING = 46f;
    private static final float STATUS_GAP_BELOW_MENU = 60f;
    private static final ColorRGBA STATUS_COLOR = new ColorRGBA(0.75f, 0.82f, 0.9f, 1f);
    private static final ColorRGBA TITLE_COLOR = new ColorRGBA(0.92f, 0.95f, 1f, 1f);

    private BitmapText titleText;
    private BitmapText statusText;

    /**
     * @param navigator screen navigation, used for Settings and Quit
     * @param input the input controller this screen binds to while enabled
     */
    public MainMenuState(ScreenNavigator navigator, InputController input) {
        super("main-menu", input);
        this.model = new MenuModel(List.of(
                MenuItem.action("New Game", this::startNewGame),
                MenuItem.disabled("Continue"),
                MenuItem.action("Settings", navigator::showSettings),
                MenuItem.action("Quit", navigator::quit)));
    }

    @Override
    protected void initialize(Application app) {
        BitmapFont font = app.getAssetManager().loadFont(FONT_PATH);
        createMenuView(font, ITEM_SCALE, ITEM_SPACING);
        titleText = new BitmapText(font);
        titleText.setText(TITLE);
        titleText.setColor(TITLE_COLOR);
        titleText.setLocalScale(TITLE_SCALE);
        statusText = new BitmapText(font);
        statusText.setColor(STATUS_COLOR);
        statusText.setLocalScale(STATUS_SCALE);
        screenNode.attachChild(titleText);
        screenNode.attachChild(statusText);
    }

    @Override
    protected void onEnable() {
        statusText.setText("");
        showScreen();
    }

    @Override
    protected void onAction(GameAction action) {
        switch (action) {
            case MENU_UP -> moveSelection(-1);
            case MENU_DOWN -> moveSelection(1);
            case MENU_CONFIRM -> confirmSelected();
            case MENU_CLICK -> activateAt(input.cursorPosition());
            default -> {
                // Left, right and back have no effect on the title screen.
            }
        }
    }

    @Override
    protected void layoutScreen(float width, float height) {
        titleText.setLocalTranslation(
                width / 2f - titleText.getLineWidth() * TITLE_SCALE / 2f,
                height - TITLE_TOP_MARGIN,
                0f);
        menuView.layout(width, height, MENU_TOP_MARGIN);
        float statusTop = height - MENU_TOP_MARGIN - model.items().size() * ITEM_SPACING - STATUS_GAP_BELOW_MENU;
        statusText.setLocalTranslation(
                width / 2f - statusText.getLineWidth() * STATUS_SCALE / 2f,
                statusTop,
                0f);
    }

    private void startNewGame() {
        LOG.info("New Game selected; gameplay is not implemented in this milestone");
        statusText.setText(NEW_GAME_STATUS);
        relayout();
    }
}
