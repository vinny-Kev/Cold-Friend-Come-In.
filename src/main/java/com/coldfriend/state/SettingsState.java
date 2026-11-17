package com.coldfriend.state;

import com.coldfriend.core.GameSettings;
import com.coldfriend.core.Resolution;
import com.coldfriend.input.GameAction;
import com.coldfriend.input.InputController;
import com.coldfriend.ui.MenuItem;
import com.coldfriend.ui.MenuModel;
import com.jme3.app.Application;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import java.util.List;

/**
 * Settings screen. Edits a working copy of the settings: Apply saves them and applies display changes,
 * Back discards them.
 */
public final class SettingsState extends MenuScreenState {

    private static final String FONT_PATH = "Interface/Fonts/Default.fnt";
    private static final String TITLE = "SETTINGS";
    private static final String AUDIO_NOTE =
            "Volume settings are saved now. Audio playback arrives in a later milestone.";
    private static final float TITLE_SCALE = 3f;
    private static final float ITEM_SCALE = 1.8f;
    private static final float NOTE_SCALE = 1.2f;
    private static final float TITLE_TOP_MARGIN = 70f;
    private static final float MENU_TOP_MARGIN = 190f;
    private static final float ITEM_SPACING = 40f;
    private static final float NOTE_GAP_BELOW_MENU = 40f;
    private static final ColorRGBA TITLE_COLOR = new ColorRGBA(0.92f, 0.95f, 1f, 1f);
    private static final ColorRGBA NOTE_COLOR = new ColorRGBA(0.7f, 0.76f, 0.84f, 1f);

    private static final String SAVE_FAILED_NOTE =
            "Could not save settings to disk. They stay active until the game closes.";

    private final ScreenNavigator navigator;
    private final List<MenuItem> items;
    private GameSettings draft;
    private BitmapText titleText;
    private BitmapText noteText;

    /**
     * @param navigator screen navigation and settings access
     * @param input the input controller this screen binds to while enabled
     */
    public SettingsState(ScreenNavigator navigator, InputController input) {
        super("settings", input);
        this.navigator = navigator;
        this.items = List.of(
                MenuItem.value("", delta -> draft = draft.withMasterVolume(
                        draft.masterVolume() + delta * GameSettings.VOLUME_STEP),
                        () -> draft = draft.withMasterVolume(nextVolume(draft.masterVolume()))),
                MenuItem.value("", delta -> draft = draft.withMusicVolume(
                        draft.musicVolume() + delta * GameSettings.VOLUME_STEP),
                        () -> draft = draft.withMusicVolume(nextVolume(draft.musicVolume()))),
                MenuItem.value("", delta -> draft = draft.withSfxVolume(
                        draft.sfxVolume() + delta * GameSettings.VOLUME_STEP),
                        () -> draft = draft.withSfxVolume(nextVolume(draft.sfxVolume()))),
                MenuItem.value("", delta -> draft = draft.withResolution(nextResolution(delta))),
                MenuItem.value("", delta -> draft = draft.withFullscreen(!draft.fullscreen())),
                MenuItem.value("", delta -> draft = draft.withVsync(!draft.vsync())),
                MenuItem.value("", delta -> draft = draft.withFog(draft.fog().step(delta))),
                MenuItem.value("", delta -> draft = draft.withSnow(draft.snow().step(delta))),
                MenuItem.value("", delta -> draft = draft.withCameraSway(!draft.cameraSway())),
                MenuItem.action("Apply", this::applyAndClose),
                MenuItem.action("Back", navigator::showMainMenu));
        this.model = new MenuModel(items);
    }

    @Override
    protected void initialize(Application app) {
        BitmapFont font = app.getAssetManager().loadFont(FONT_PATH);
        createMenuView(font, ITEM_SCALE, ITEM_SPACING);
        titleText = new BitmapText(font);
        titleText.setText(TITLE);
        titleText.setColor(TITLE_COLOR);
        titleText.setLocalScale(TITLE_SCALE);
        noteText = new BitmapText(font);
        noteText.setText(AUDIO_NOTE);
        noteText.setColor(NOTE_COLOR);
        noteText.setLocalScale(NOTE_SCALE);
        screenNode.attachChild(titleText);
        screenNode.attachChild(noteText);
    }

    @Override
    protected void onEnable() {
        draft = navigator.currentSettings();
        noteText.setText(AUDIO_NOTE);
        refreshLabels();
        showScreen();
    }

    @Override
    protected void onAction(GameAction action) {
        switch (action) {
            case MENU_UP -> moveSelection(-1);
            case MENU_DOWN -> moveSelection(1);
            case MENU_LEFT -> adjustSelected(-1);
            case MENU_RIGHT -> adjustSelected(1);
            case MENU_CONFIRM -> confirmSelected();
            case MENU_BACK -> navigator.showMainMenu();
            case MENU_CLICK -> activateAt(input.cursorPosition());
        }
    }

    @Override
    protected void onValuesChanged() {
        refreshLabels();
    }

    @Override
    protected void layoutScreen(float width, float height) {
        titleText.setLocalTranslation(
                width / 2f - titleText.getLineWidth() * TITLE_SCALE / 2f,
                height - TITLE_TOP_MARGIN,
                0f);
        menuView.layout(width, height, MENU_TOP_MARGIN);
        float noteTop = height - MENU_TOP_MARGIN - model.items().size() * ITEM_SPACING - NOTE_GAP_BELOW_MENU;
        noteText.setLocalTranslation(
                width / 2f - noteText.getLineWidth() * NOTE_SCALE / 2f,
                noteTop,
                0f);
    }

    private void applyAndClose() {
        if (!navigator.saveAndApplySettings(draft)) {
            // Stay open so the player sees the warning; the settings still apply for this session.
            noteText.setText(SAVE_FAILED_NOTE);
            relayout();
            return;
        }
        navigator.showMainMenu();
    }

    /** Clicking or confirming a volume steps it up by one step and wraps from the maximum back to the minimum. */
    private static int nextVolume(int value) {
        return value >= GameSettings.MAX_VOLUME ? GameSettings.MIN_VOLUME : value + GameSettings.VOLUME_STEP;
    }

    private Resolution nextResolution(int delta) {
        List<Resolution> options = Resolution.OPTIONS;
        int index = options.indexOf(draft.resolution());
        if (index < 0) {
            index = 0;
        }
        return options.get(Math.floorMod(index + delta, options.size()));
    }

    private void refreshLabels() {
        items.get(0).setLabel("Master Volume: " + draft.masterVolume() + "%");
        items.get(1).setLabel("Music Volume: " + draft.musicVolume() + "%");
        items.get(2).setLabel("Sound Effects Volume: " + draft.sfxVolume() + "%");
        items.get(3).setLabel("Resolution: " + draft.resolution());
        items.get(4).setLabel("Fullscreen: " + onOff(draft.fullscreen()));
        items.get(5).setLabel("VSync: " + onOff(draft.vsync()));
        items.get(6).setLabel("Fog: " + draft.fog().label());
        items.get(7).setLabel("Snow: " + draft.snow().label());
        items.get(8).setLabel("Camera Sway: " + onOff(draft.cameraSway()));
        menuView.refresh();
    }

    private static String onOff(boolean value) {
        return value ? "On" : "Off";
    }
}
