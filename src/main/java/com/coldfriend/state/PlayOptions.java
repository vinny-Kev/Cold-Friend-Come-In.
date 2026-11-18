package com.coldfriend.state;

import com.coldfriend.time.ClockParameters;
import java.util.Locale;
import java.util.OptionalInt;

/**
 * Choices for starting a world that are not part of the world itself. The defaults are what a normal game uses;
 * the others exist for development and testing.
 *
 * @param spawn where the player starts: {@code cabin}, {@code cabindoor}, {@code cabinview}, {@code village}
 *        or {@code river}
 * @param startMinute the time of day on day 1 that the game starts at, in minutes after midnight
 * @param scenario a development script such as {@code inventory}, or empty for none
 * @param showCreature development only: draw the creature and its state
 */
public record PlayOptions(String spawn, int startMinute, String scenario, boolean showCreature) {

    /** A normal game: start in the cabin at 07:00 on day 1 with no script. */
    public static final PlayOptions NORMAL = new PlayOptions("cabin", ClockParameters.DEFAULT.newGameStart(), "", false);

    /**
     * Reads the {@code dev.time} development setting: a start time, not a lighting mode. The lighting follows the
     * game clock from there.
     *
     * @param value {@code day} (12:00), {@code dusk} (18:00), {@code night} (22:00), or {@code HH:MM}
     * @return the start minute, or empty if the value is not one of those
     */
    public static OptionalInt parseStartTime(String value) {
        String text = value.trim().toLowerCase(Locale.ROOT);
        switch (text) {
            case "day" -> {
                return OptionalInt.of(12 * 60);
            }
            case "dusk" -> {
                return OptionalInt.of(18 * 60);
            }
            case "night" -> {
                return OptionalInt.of(22 * 60);
            }
            default -> {
                // Try HH:MM below.
            }
        }
        String[] parts = text.split(":", -1);
        if (parts.length != 2) {
            return OptionalInt.empty();
        }
        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(hours * 60 + minutes);
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }
}
