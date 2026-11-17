package com.coldfriend.state;

import com.coldfriend.input.GameAction;
import com.coldfriend.input.InputController;
import com.coldfriend.ui.MenuItem;
import com.coldfriend.ui.MenuModel;
import com.jme3.app.Application;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import java.util.List;

/** The pause menu shown over the world: Resume, Settings and Quit to Menu. */
public final class PauseState extends MenuScreenState {

    private static final String FONT_PATH = "Interface/Fonts/Default.fnt";
    private static final String TITLE = "PAUSED";
    private static final float TITLE_SCALE = 3.5f;
    private static final float ITEM_SCALE = 2f;
    private static final float TITLE_TOP_MARGIN = 150f;
    private static final float MENU_TOP_MARGIN = 280f;
    private static final float ITEM_SPACING = 46f;
    private static final ColorRGBA TITLE_COLOR = new ColorRGBA(0.92f, 0.95f, 1f, 1f);
    private static final ColorRGBA DIM_COLOR = new ColorRGBA(0f, 0f, 0f, 0.5f);

    private final ScreenNavigator navigator;
    private BitmapText titleText;
    private Geometry dim;

    /**
     * @param navigator used to resume, open settings and leave the world
     * @param input the input controller this screen binds to while enabled
     */
    public PauseState(ScreenNavigator navigator, InputController input) {
        super("pause", input);
        this.navigator = navigator;
        this.model = new MenuModel(List.of(
                MenuItem.action("Resume", navigator::resumeGame),
                MenuItem.action("Settings", navigator::showSettings),
                MenuItem.action("Quit to Menu", navigator::leaveWorld)));
    }

    @Override
    protected void initialize(Application app) {
        BitmapFont font = app.getAssetManager().loadFont(FONT_PATH);
        createMenuView(font, ITEM_SCALE, ITEM_SPACING);
        titleText = new BitmapText(font);
        titleText.setText(TITLE);
        titleText.setColor(TITLE_COLOR);
        titleText.setLocalScale(TITLE_SCALE);
        Material dimMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        dimMaterial.setColor("Color", DIM_COLOR);
        dimMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        dim = new Geometry("pause-dim", new Quad(1f, 1f));
        dim.setMaterial(dimMaterial);
        screenNode.attachChild(dim);
        screenNode.attachChild(titleText);
    }

    @Override
    protected void onEnable() {
        showScreen();
    }

    @Override
    protected void onAction(GameAction action) {
        switch (action) {
            case MENU_UP -> moveSelection(-1);
            case MENU_DOWN -> moveSelection(1);
            case MENU_CONFIRM -> confirmSelected();
            case MENU_CLICK -> activateAt(input.cursorPosition());
            case MENU_BACK -> navigator.resumeGame();
            default -> {
                // Left and right have no effect here.
            }
        }
    }

    @Override
    protected void layoutScreen(float width, float height) {
        dim.setMesh(new Quad(width, height));
        titleText.setLocalTranslation(
                width / 2f - titleText.getLineWidth() * TITLE_SCALE / 2f,
                height - TITLE_TOP_MARGIN,
                0f);
        menuView.layout(width, height, MENU_TOP_MARGIN);
    }
}
