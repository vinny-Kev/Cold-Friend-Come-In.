package com.coldfriend.creature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.entity.EntityId;
import com.coldfriend.player.PlayerId;
import com.coldfriend.player.PlayerState;
import com.coldfriend.time.DayPhase;
import com.coldfriend.world.gen.RandomStreams;
import com.coldfriend.world.gen.WorldGenParameters;
import com.coldfriend.world.gen.WorldGenerator;
import com.coldfriend.world.gen.WorldSeed;
import com.coldfriend.world.model.HeightField;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.query.WorldCollision;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Sight and hearing, checked on a real generated world with a creature facing a known way. */
class PerceptionTest {

    private static WorldData world;
    private static WorldCollision collision;
    private static Perception perception;
    private static float baseX;
    private static float baseZ;
    private static float baseHeading;
    private static final double MINUTE = 600.0;

    @BeforeAll
    static void setUp() {
        world = WorldGenerator.generate(new WorldSeed(404L), WorldGenParameters.DEFAULT);
        collision = new WorldCollision(world);
        perception = new Perception(world, collision, new SplittableRandom(5L));
        float[] open = openGround();
        baseX = open[0];
        baseZ = open[1];
        baseHeading = 0f;
    }

    /** A flat, open spot: clear sight for 60 m in eight directions and nearly level ground. */
    private static float[] openGround() {
        HeightField terrain = world.terrain();
        for (float x = 200f; x < world.sizeMeters() - 200f; x += 20f) {
            for (float z = 200f; z < world.sizeMeters() - 200f; z += 20f) {
                if (!collision.canStandAt(x, z, 0.4f) || world.water().isWaterAt(x, z)) {
                    continue;
                }
                boolean open = true;
                for (int d = 0; d < 8 && open; d++) {
                    double angle = d * Math.PI / 4;
                    float ex = x + (float) (Math.sin(angle) * 60);
                    float ez = z + (float) (Math.cos(angle) * 60);
                    open = collision.lineClear(x, z, ex, ez, 0.1f, null)
                            && Math.abs(terrain.heightAt(ex, ez) - terrain.heightAt(x, z)) < 1.0f;
                }
                if (open) {
                    return new float[] {x, z};
                }
            }
        }
        throw new IllegalStateException("no open ground in the test world");
    }

    /** A creature at the base spot, facing +z, with a player placed at the given bearing and distance. */
    private static CreatureState creature() {
        return new CreatureState(new EntityId(9L), baseX, baseZ, baseHeading);
    }

    /** A player at bearing (radians from +z, positive toward +x) and distance from the creature. */
    private static PlayerState playerAt(double bearing, double distance, double facing) {
        float x = baseX + (float) (Math.sin(bearing) * distance);
        float z = baseZ + (float) (Math.cos(bearing) * distance);
        return new PlayerState(PlayerId.LOCAL, x, world.terrain().heightAt(x, z), z, (float) facing);
    }

    private static Sighting look(CreatureState creature, PlayerState player, DayPhase phase, Memory memory) {
        return perception.look(creature, player, phase, memory, MINUTE);
    }

    /** The bearing the player is at, plus the direction facing: toward the creature. */
    private static double facingTheCreature(double bearing) {
        return bearing + Math.PI;
    }

    @Test
    void aCreatureSeesAPlayerInFrontOfItAndNotBehind() {
        Memory memory = new Memory();
        Sighting front = look(creature(), playerAt(0, 20, 0), DayPhase.DAY, memory);
        assertTrue(front.creatureCanSeePlayer());
        Sighting behind = look(creature(), playerAt(Math.PI, 20, 0), DayPhase.DAY, memory);
        assertFalse(behind.creatureCanSeePlayer());
    }

    @Test
    void theCreaturesConeIsSixtyDegreesEitherSide() {
        Memory memory = new Memory();
        assertTrue(look(creature(), playerAt(Math.toRadians(55), 20, 0), DayPhase.DAY, memory)
                .creatureCanSeePlayer());
        assertFalse(look(creature(), playerAt(Math.toRadians(65), 20, 0), DayPhase.DAY, memory)
                .creatureCanSeePlayer());
    }

    @Test
    void theCreatureSeesFurtherByDayThanAtDuskAndNight() {
        Memory memory = new Memory();
        assertTrue(look(creature(), playerAt(0, 55, 0), DayPhase.DAY, memory).creatureCanSeePlayer());
        assertFalse(look(creature(), playerAt(0, 61, 0), DayPhase.DAY, memory).creatureCanSeePlayer());
        assertTrue(look(creature(), playerAt(0, 30, 0), DayPhase.DUSK, memory).creatureCanSeePlayer());
        assertFalse(look(creature(), playerAt(0, 36, 0), DayPhase.DUSK, memory).creatureCanSeePlayer());
        assertTrue(look(creature(), playerAt(0, 10, 0), DayPhase.NIGHT, memory).creatureCanSeePlayer());
        assertFalse(look(creature(), playerAt(0, 16, 0), DayPhase.NIGHT, memory).creatureCanSeePlayer());
    }

    @Test
    void thePlayerSeesTheCreatureOnlyWhenLookingAtIt() {
        Memory memory = new Memory();
        Sighting facing = look(creature(), playerAt(0, 15, facingTheCreature(0)), DayPhase.DAY, memory);
        assertTrue(facing.playerCanSeeCreature());
        Sighting away = look(creature(), playerAt(0, 15, 0), DayPhase.DAY, memory);
        assertFalse(away.playerCanSeeCreature());
    }

    @Test
    void theCreatureWatchedFactIsSeparateFromSeeing() {
        Memory memory = new Memory();
        // Close, facing the creature: the creature sees the player and believes it is being watched.
        Sighting watched = look(creature(), playerAt(0, 15, facingTheCreature(0)), DayPhase.DAY, memory);
        assertTrue(watched.creatureCanSeePlayer());
        assertTrue(watched.creatureBelievesWatched());
        // Close, facing away: the creature sees the player but is not watched.
        Sighting unwatched = look(creature(), playerAt(0, 15, 0), DayPhase.DAY, memory);
        assertTrue(unwatched.creatureCanSeePlayer());
        assertFalse(unwatched.creatureBelievesWatched());
        // Facing the creature but too far away to tell: not watched.
        Sighting far = look(creature(), playerAt(0, 40, facingTheCreature(0)), DayPhase.DAY, memory);
        assertFalse(far.creatureBelievesWatched());
    }

    @Test
    void aTreeBetweenThemBlocksBothWays() {
        collision.addObstacle(new EntityId(77L), baseX, baseZ + 10f, 0.5f);
        try {
            Memory memory = new Memory();
            Sighting sighting = look(creature(), playerAt(0, 20, facingTheCreature(0)), DayPhase.DAY, memory);
            assertTrue(sighting.blocked());
            assertFalse(sighting.creatureCanSeePlayer());
            assertFalse(sighting.playerCanSeeCreature());
            assertFalse(sighting.creatureBelievesWatched());
            assertEquals(0, memory.size(), "nothing is remembered from a view that is blocked");
        } finally {
            collision.removeObstacle(new EntityId(77L));
        }
    }

    @Test
    void aSeenPlayerIsRemembered() {
        Memory memory = new Memory();
        look(creature(), playerAt(0, 20, 0), DayPhase.DAY, memory);
        assertEquals(ObservationKind.SIGHT, memory.get(0).kind());
        assertTrue(memory.trace(PlayerId.LOCAL).hasSeen());
    }

    @Test
    void aFarDoorIsNotHeardAndANearDoorIs() {
        CreatureState listener = creature();
        Memory memory = new Memory();
        perception.hear(listener, List.of(new SenseEvent(SenseKind.DOOR, baseX + 40f, baseZ, MINUTE)),
                DayPhase.DAY, memory);
        assertEquals(0, memory.size(), "a door is heard within 20 m only");

        perception.hear(listener, List.of(new SenseEvent(SenseKind.DOOR, baseX + 15f, baseZ, MINUTE)),
                DayPhase.DAY, memory);
        assertEquals(1, memory.size());
        assertEquals(ObservationKind.SOUND, memory.get(0).kind());
        float confidence = memory.get(0).startConfidence();
        assertTrue(confidence >= CreatureParameters.HEARING_FLOOR && confidence <= CreatureParameters.HEARING_CAP,
                "confidence " + confidence);
    }

    @Test
    void aHeardPlaceIsNeverExact() {
        CreatureState listener = creature();
        float maxError = 0f;
        for (int i = 0; i < 50; i++) {
            Memory memory = new Memory();
            perception.hear(listener, List.of(new SenseEvent(SenseKind.DOOR, baseX + 5f, baseZ, MINUTE)),
                    DayPhase.DAY, memory);
            Observation heard = memory.get(0);
            float error = (float) Math.hypot(heard.x() - (baseX + 5f), heard.z() - baseZ);
            maxError = Math.max(maxError, error);
        }
        assertTrue(maxError > 0f, "sound should be jittered");
        assertTrue(maxError <= CreatureParameters.HEARING_JITTER * Math.sqrt(2) + 0.001f,
                "error " + maxError + " is larger than the jitter allows");
    }

    @Test
    void aDroppedItemIsNoticedAsAChange() {
        Memory memory = new Memory();
        perception.hear(creature(), List.of(new SenseEvent(SenseKind.DROP, baseX + 2f, baseZ, MINUTE)),
                DayPhase.DAY, memory);
        assertEquals(ObservationKind.CHANGED, memory.get(0).kind());
    }

    @Test
    void aWallCutsASoundThatWouldOtherwiseBeHeard() {
        // A footstep is heard within 6 m in the open. From the cabin's centre to a spot just outside its wall is less
        // than 6 m, but the wall passes it down to about 2 m, so it is not heard.
        var cabin = world.cabin().footprint();
        float outsideX = cabin.x() + cabin.halfWidth() + 1f;
        float outsideZ = cabin.z();
        float distance = outsideX - cabin.x();
        assertTrue(distance < 6f, "the test needs the spot to be inside a footstep's reach without walls");

        Memory open = new Memory();
        perception.hear(new CreatureState(new EntityId(9L), baseX + 4f, baseZ, 0f),
                List.of(new SenseEvent(SenseKind.FOOTSTEP_WALK, baseX, baseZ, MINUTE)), DayPhase.DAY, open);
        assertEquals(1, open.size(), "control: heard in the open at 4 m");

        Memory walled = new Memory();
        perception.hear(new CreatureState(new EntityId(9L), outsideX, outsideZ, 0f),
                List.of(new SenseEvent(SenseKind.FOOTSTEP_WALK, cabin.x(), cabin.z(), MINUTE)), DayPhase.DAY, walled);
        assertEquals(0, walled.size(), "behind a wall the same step is too quiet to hear");
    }

    @Test
    void theSameSeedHearsTheSamePlace() {
        Memory first = new Memory();
        Memory second = new Memory();
        SenseEvent event = new SenseEvent(SenseKind.DOOR, baseX + 6f, baseZ, MINUTE);
        new Perception(world, collision, new SplittableRandom(9L))
                .hear(creature(), List.of(event), DayPhase.DAY, first);
        new Perception(world, collision, new SplittableRandom(9L))
                .hear(creature(), List.of(event), DayPhase.DAY, second);
        assertEquals(first.get(0), second.get(0));
    }
}
