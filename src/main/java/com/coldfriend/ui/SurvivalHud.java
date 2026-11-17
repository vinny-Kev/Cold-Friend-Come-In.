package com.coldfriend.ui;

import com.coldfriend.survival.HearthState;
import com.coldfriend.survival.HearthStatus;
import com.coldfriend.survival.Need;
import com.coldfriend.survival.SurvivalStatus;
import com.coldfriend.time.DayPhase;
import com.jme3.font.BitmapFont;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import java.util.Locale;
import java.util.Optional;

/**
 * Presentation only: the day and time, the player's needs, the stove, and a warning when a need is low. Reads
 * snapshots from the simulation and never changes them. Text is rebuilt a few times a second, not every frame.
 */
public final class SurvivalHud {

    private static final float SCALE = 1.1f;
    private static final float WARNING_SCALE = 1.4f;
    private static final float DEATH_SCALE = 2.0f;
    private static final float MARGIN = 20f;
    private static final float LINE_GAP = 4f;
    private static final float WARNING_TOP = 90f;
    private static final float REFRESH_SECONDS = 0.25f;
    private static final ColorRGBA NORMAL = new ColorRGBA(0.95f, 0.95f, 1f, 0.9f);
    private static final ColorRGBA LOW = new ColorRGBA(1f, 0.8f, 0.4f, 1f);
    private static final ColorRGBA CRITICAL = new ColorRGBA(1f, 0.4f, 0.35f, 1f);

    private final Node root = new Node("survival-hud");
    private final ShadowText time;
    private final ShadowText needs;
    private final ShadowText stove;
    private final ShadowText warning;
    private final ShadowText death;
    private float sinceRefresh = REFRESH_SECONDS;

    /** @param font the font to write with */
    public SurvivalHud(BitmapFont font) {
        time = new ShadowText(font, SCALE, NORMAL);
        needs = new ShadowText(font, SCALE, NORMAL);
        stove = new ShadowText(font, SCALE, NORMAL);
        warning = new ShadowText(font, WARNING_SCALE, LOW);
        death = new ShadowText(font, DEATH_SCALE, CRITICAL);
        root.attachChild(time);
        root.attachChild(needs);
        root.attachChild(stove);
        root.attachChild(warning);
        root.attachChild(death);
    }

    /** @return the node to attach to the screen's GUI node */
    public Node root() {
        return root;
    }

    /**
     * Counts frame time and says whether the text should be rebuilt now.
     *
     * @param tpf seconds since the last frame
     * @return true a few times a second
     */
    public boolean due(float tpf) {
        sinceRefresh += tpf;
        if (sinceRefresh < REFRESH_SECONDS) {
            return false;
        }
        sinceRefresh = 0f;
        return true;
    }

    /** Makes the next {@link #due} call return true, for example after a pause. */
    public void refreshSoon() {
        sinceRefresh = REFRESH_SECONDS;
    }

    /**
     * Rewrites the text from the simulation's current state.
     *
     * @param day the day number
     * @param minuteOfDay minutes after midnight
     * @param phase the part of the day
     * @param status the player's needs
     * @param hearth the cabin stove
     * @param width screen width in pixels
     * @param height screen height in pixels
     */
    public void show(int day, double minuteOfDay, DayPhase phase, SurvivalStatus status, HearthStatus hearth,
            float width, float height) {
        int minute = (int) minuteOfDay;
        time.setText(String.format(Locale.ROOT, "Day %d   %02d:%02d   %s", day, minute / 60, minute % 60,
                phaseName(phase)));
        needs.setText(String.format(Locale.ROOT, "Warmth %d   Food %d   Water %d   Health %d",
                Math.round(status.warmth()), Math.round(status.nourishment()), Math.round(status.hydration()),
                Math.round(status.health())));
        needs.setColor(worstColor(status));
        stove.setText("Stove: " + stoveText(hearth));
        stove.setColor(hearth.state().lit() ? NORMAL : LOW);

        float y = height - MARGIN;
        for (ShadowText line : new ShadowText[] {time, needs, stove}) {
            line.setLocalTranslation(MARGIN, y, 0f);
            y -= line.lineHeight() + LINE_GAP;
        }

        Optional<String> message = warningText(status);
        warning.setText(message.orElse(""));
        warning.setColor(anyCritical(status) || status.healthFalling() ? CRITICAL : LOW);
        warning.setLocalTranslation(width / 2f - warning.lineWidth() / 2f, height - WARNING_TOP, 0f);

        death.setText(status.dead() ? deathText(status.causeOfDeath().orElseThrow())
                + "\n[Esc] menu" : "");
        death.setLocalTranslation(width / 2f - death.lineWidth() / 2f, height / 2f + death.lineHeight(), 0f);
    }

    private static String phaseName(DayPhase phase) {
        return switch (phase) {
            case DAWN -> "Dawn";
            case DAY -> "Day";
            case DUSK -> "Dusk";
            case NIGHT -> "Night";
        };
    }

    private static String stoveText(HearthStatus hearth) {
        if (hearth.state() == HearthState.UNLIT) {
            return hearth.fuel() > 0 ? "not lit (wood inside)" : "not lit";
        }
        if (hearth.state() == HearthState.OUT) {
            return hearth.fuel() > 0 ? "out (wood inside, needs lighting)" : "out";
        }
        String size = switch (hearth.heat()) {
            case STRONG -> "burning well";
            case LOW -> hearth.state() == HearthState.EMBERS ? "embers" : "burning low";
            case NONE -> "cold";
        };
        return String.format(Locale.ROOT, "%s, about %.0f h of wood", size, Math.ceil(hearth.fuel()));
    }

    private static boolean anyCritical(SurvivalStatus status) {
        return status.critical(Need.WARMTH) || status.critical(Need.NOURISHMENT) || status.critical(Need.HYDRATION);
    }

    private static ColorRGBA worstColor(SurvivalStatus status) {
        if (anyCritical(status) || status.healthFalling()) {
            return CRITICAL;
        }
        for (Need need : Need.values()) {
            if (status.low(need)) {
                return LOW;
            }
        }
        return NORMAL;
    }

    /** The most urgent thing to tell the player, if anything is low. */
    private static Optional<String> warningText(SurvivalStatus status) {
        if (status.dead()) {
            return Optional.empty();
        }
        String health = status.healthFalling() ? " Your health is failing." : "";
        if (status.critical(Need.WARMTH)) {
            return Optional.of("You are freezing. Get to a fire." + health);
        }
        if (status.critical(Need.HYDRATION)) {
            return Optional.of("You are parched. Drink something." + health);
        }
        if (status.critical(Need.NOURISHMENT)) {
            return Optional.of("You are starving. Eat something." + health);
        }
        if (status.low(Need.WARMTH)) {
            return Optional.of("You are getting cold.");
        }
        if (status.low(Need.HYDRATION)) {
            return Optional.of("You are thirsty.");
        }
        if (status.low(Need.NOURISHMENT)) {
            return Optional.of("You are hungry.");
        }
        if (status.low(Need.HEALTH)) {
            return Optional.of("You feel weak.");
        }
        return Optional.empty();
    }

    private static String deathText(Need cause) {
        return switch (cause) {
            case WARMTH -> "You froze to death.";
            case HYDRATION -> "You died of thirst.";
            case NOURISHMENT -> "You starved.";
            case HEALTH -> "You died.";
        };
    }
}
