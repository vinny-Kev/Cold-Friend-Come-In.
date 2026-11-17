package com.coldfriend.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.player.PlayerId;
import com.coldfriend.player.PlayerInput;
import com.coldfriend.player.PlayerState;
import com.coldfriend.world.gen.WorldGenParameters;
import com.coldfriend.world.gen.WorldGenerator;
import com.coldfriend.world.gen.WorldSeed;
import com.coldfriend.world.model.WorldData;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FixedStepTest {

    private static WorldData world;

    @BeforeAll
    static void generate() {
        world = WorldGenerator.generate(new WorldSeed(77L), WorldGenParameters.DEFAULT);
    }

    /** Runs frames of one length (binary fractions, so they add up exactly) and counts the steps. */
    private static long steps(FixedStep step, float frame, int frames) {
        long total = 0;
        for (int i = 0; i < frames; i++) {
            total += step.advance(frame);
        }
        return total;
    }

    @Test
    void theSameElapsedTimeGivesTheSameStepsWhateverTheFrameRate() {
        // Ten seconds as 32, 64 and 128 frames per second: 200 steps each.
        assertEquals(200, steps(new FixedStep(), 1f / 32f, 320));
        assertEquals(200, steps(new FixedStep(), 1f / 64f, 640));
        assertEquals(200, steps(new FixedStep(), 1f / 128f, 1280));
    }

    @Test
    void irregularFramesAddUpLikeSteadyOnes() {
        FixedStep step = new FixedStep();
        float[] pattern = {1f / 128f, 1f / 16f, 1f / 64f, 1f / 32f, 1f / 128f, 1f / 64f};
        // The pattern sums to 0.140625 s; 64 repetitions are exactly 9 s, or 180 steps.
        long total = 0;
        for (int r = 0; r < 64; r++) {
            for (float frame : pattern) {
                total += step.advance(frame);
            }
        }
        assertEquals(180, total);
    }

    @Test
    void stepBoundaryIsExact() {
        FixedStep step = new FixedStep();
        // Times are rounded to whole nanoseconds: 0.025f is 25 000 000 ns, so two make exactly one step.
        assertEquals(0, step.advance(0.025f));
        assertEquals(0, step.advance(0.0249f));
        assertEquals(1, step.advance(0.0001f));
        assertEquals(0f, step.alpha(), 1e-6f);
        assertEquals(1, step.advance(0.05f));
        assertEquals(0, step.advance(0.025f));
        assertEquals(0.5f, step.alpha(), 1e-4f);
    }

    @Test
    void aHugeFrameRunsAtMostTheCapAndDropsTheRest() {
        FixedStep step = new FixedStep();
        assertEquals(FixedStep.MAX_STEPS_PER_UPDATE, step.advance(10f));
        assertEquals(200 - FixedStep.MAX_STEPS_PER_UPDATE, step.droppedSteps());
        // Nothing is owed afterwards: the next short frame does not run a burst.
        assertEquals(0, step.advance(0.01f));
    }

    @Test
    void badFrameTimesCountAsNoTime() {
        FixedStep step = new FixedStep();
        assertEquals(0, step.advance(0f));
        assertEquals(0, step.advance(-1f));
        assertEquals(0, step.advance(Float.NaN));
        assertEquals(0f, step.alpha());
    }

    @Test
    void resetForgetsOwedTime() {
        FixedStep step = new FixedStep();
        step.advance(0.04f);
        step.reset();
        assertEquals(0, step.advance(0.02f));
        assertEquals(0f + 0.4f, step.alpha(), 1e-4f);
    }

    /** The whole simulation ends in the same state whatever the frame rate, for the same elapsed time. */
    @Test
    void simulationStateDoesNotDependOnFrameRate() {
        PlayerInput walk = new PlayerInput(1f, 0.3f, 0f, 0f, false);
        float[] reference = runWorld(walk, 1f / 32f, 320);
        float[] fast = runWorld(walk, 1f / 128f, 1280);
        float[] slow = runWorld(walk, 1f / 16f, 160);
        for (int i = 0; i < reference.length; i++) {
            assertEquals(reference[i], fast[i], "value " + i + " differs at 128 fps");
            assertEquals(reference[i], slow[i], "value " + i + " differs at 16 fps");
        }
    }

    private static float[] runWorld(PlayerInput input, float frame, int frames) {
        WorldSimulation simulation = new WorldSimulation(world);
        PlayerState player = simulation.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, "cabindoor"));
        for (int i = 0; i < frames; i++) {
            simulation.update(frame, Map.of(PlayerId.LOCAL, input));
        }
        return new float[] {player.x(), player.y(), player.z(), (float) simulation.minuteOfDay()};
    }

    @Test
    void noTimePassesWhileTheSimulationIsNotUpdated() {
        WorldSimulation simulation = new WorldSimulation(world);
        PlayerState player = simulation.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, "cabindoor"));
        PlayerInput walk = new PlayerInput(1f, 0f, 0f, 0f, false);
        simulation.update(0.04f, Map.of(PlayerId.LOCAL, walk));
        double minute = simulation.minuteOfDay();
        float x = player.x();
        // A pause: the play screen stops calling update, then resets timing on resume.
        simulation.resetFrameTiming();
        simulation.update(0.02f, Map.of(PlayerId.LOCAL, walk));
        assertEquals(minute, simulation.minuteOfDay(), "owed time before the pause must not run after it");
        assertEquals(x, player.x());
        assertTrue(simulation.interpolation() < 1f);
    }
}
