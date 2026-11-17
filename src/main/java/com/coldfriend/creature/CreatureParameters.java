package com.coldfriend.creature;

/**
 * Tuning for the creature. Values are the ones approved in docs/development/CREATURE_PHASE_B_PLAN.md, unless a
 * comment says otherwise. Angles are in degrees, distances in metres, times in game minutes or real seconds as
 * named. Kept as constants rather than a record: this set is large and read in one place.
 */
public final class CreatureParameters {

    /** Height of the creature's eyes above the ground, in metres. */
    public static final float CREATURE_EYE_HEIGHT = 1.6f;
    /** Height of the player's eyes above their feet, in metres (matches the camera). */
    public static final float PLAYER_EYE_HEIGHT = 1.65f;

    /** The creature sees this far to each side of its heading, in degrees: 120 degrees in all. */
    public static final float CREATURE_HALF_ANGLE = 60f;
    /** The creature sees this far above and below level, in degrees: 60 degrees in all. */
    public static final float CREATURE_HALF_ELEVATION = 30f;
    /** The player's look cone, to each side, in degrees (also used to judge whether the player faces the creature). */
    public static final float PLAYER_HALF_ANGLE = 35f;
    /** The player's look cone, above and below the look line, in degrees. */
    public static final float PLAYER_HALF_ELEVATION = 25f;

    /** How far the creature sees by day, dawn/dusk and night, in metres. */
    public static final float CREATURE_RANGE_DAY = 60f;
    public static final float CREATURE_RANGE_TWILIGHT = 35f;
    public static final float CREATURE_RANGE_NIGHT = 15f;

    /**
     * How far the player can see the creature. Not reused from the creature's own range (the brief keeps them
     * apart). Confirmed by the owner.
     */
    public static final float PLAYER_RANGE_DAY = 100f;
    public static final float PLAYER_RANGE_TWILIGHT = 60f;
    public static final float PLAYER_RANGE_NIGHT = 30f;

    /** The creature counts as "watched" only within this distance, in metres. */
    public static final float WATCHED_RANGE = 25f;
    /** Radius of the thin line used for sight checks, in metres. */
    public static final float SIGHT_RADIUS = 0.1f;
    /** Spacing of the terrain checks along a sight line, in metres, and the most checks per line. */
    public static final float TERRAIN_SAMPLE_STEP = 2f;
    public static final int TERRAIN_MAX_SAMPLES = 50;
    /** How far above a sight line the ground may rise before it blocks the view, in metres. */
    public static final float TERRAIN_MARGIN = 0.2f;

    /** Each solid building wall a sound passes through multiplies its loudness by this. */
    public static final float WALL_FACTOR = 0.35f;
    /** Spacing of the checks along a sound's path for walls, in metres. */
    public static final float WALL_SAMPLE_STEP = 0.5f;
    /** Confidence of a heard sound is kept between these, in 0 to 1. */
    public static final float HEARING_FLOOR = 0.3f;
    public static final float HEARING_CAP = 0.9f;
    /** A heard position is wrong by up to this much, in metres, so sound is never an exact marker. */
    public static final float HEARING_JITTER = 1.5f;

    /** Confidence given to each kind of observation when it is first made. */
    public static final float SIGHT_CONFIDENCE = 1.0f;
    public static final float TRACK_CONFIDENCE = 0.6f;
    public static final float CHANGED_CONFIDENCE = 0.5f;

    /** How long each kind of observation lasts, in game minutes, before its confidence reaches zero. */
    public static final double SIGHT_LIFETIME = 4.0;
    public static final double SOUND_LIFETIME = 3.0;
    public static final double TRACK_LIFETIME = 90.0;
    public static final double CHANGED_LIFETIME = 240.0;

    /** Most observations kept at once, and how close two of one kind must be to count as the same, in metres. */
    public static final int MEMORY_CAPACITY = 32;
    public static final float MERGE_RADIUS = 8f;

    /** Speed while wandering, in metres per second. Below the player's walking speed of 3.4 m/s. */
    public static final float WANDER_SPEED = 1.4f;
    /** Distance range for a wander target, in metres from the creature. */
    public static final float WANDER_MIN_DISTANCE = 30f;
    public static final float WANDER_MAX_DISTANCE = 80f;
    /** A target is given up after this many game minutes, if it has not been reached. */
    public static final double WANDER_TIMEOUT_MINUTES = 1.0;
    /** Random spots tried for a wander target before giving up for this brain tick. */
    public static final int WANDER_ATTEMPTS = 40;

    /** The creature keeps this far from the cabin and the village while wandering, in metres. */
    public static final float CABIN_KEEP_OUT_TARGET = 60f;
    public static final float CABIN_KEEP_OUT_ROUTE = 30f;
    public static final float VILLAGE_KEEP_OUT_TARGET = 40f;
    public static final float VILLAGE_KEEP_OUT_ROUTE = 20f;

    /** Where the creature starts: this far from the cabin, outside the village, in metres. */
    public static final float SPAWN_MIN_DISTANCE = 150f;
    public static final float SPAWN_MAX_DISTANCE = 300f;
    /** Random spots tried for the start. */
    public static final int SPAWN_ATTEMPTS = 500;

    /** Sight checks run every this many fixed steps (5 Hz). Hearing runs on every step. */
    public static final int PERCEPTION_EVERY_STEPS = 4;
    /** Decisions run every this many fixed steps (1 Hz). */
    public static final int BRAIN_EVERY_STEPS = 20;

    /** Paths are searched on a grid this fine, in metres, within this many grid cells examined. */
    public static final float PATH_CELL = 4f;
    public static final int PATH_NODE_BUDGET = 4000;
    /** A step counts as stalled if the creature moved less than this fraction of the way it tried to go. */
    public static final float STALL_FRACTION = 0.5f;
    /** A route is blocked if the creature stays stalled for this many seconds. */
    public static final double STALL_SECONDS = 2.0;
    /** A point on a route counts as reached within this distance, in metres. */
    public static final float WAYPOINT_REACH = 0.5f;

    private CreatureParameters() {
    }
}
