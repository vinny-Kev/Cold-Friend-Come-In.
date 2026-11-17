package com.coldfriend.interaction;

/**
 * The outcome of something a player asked the simulation to do, with a short message the screen can show.
 *
 * @param success whether it worked
 * @param message what to tell the player
 */
public record ActionResult(boolean success, String message) {

    /** @return a successful result with a message */
    public static ActionResult ok(String message) {
        return new ActionResult(true, message);
    }

    /** @return a failed result with the reason */
    public static ActionResult failed(String message) {
        return new ActionResult(false, message);
    }
}
