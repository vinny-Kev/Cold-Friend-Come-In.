package com.coldfriend.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.interaction.ActionResult;
import com.coldfriend.interaction.Interactable;
import com.coldfriend.interaction.InteractableRef;
import com.coldfriend.interaction.InteractionRequest;
import com.coldfriend.inventory.Inventory;
import com.coldfriend.item.ItemCatalog;
import com.coldfriend.item.ItemId;
import com.coldfriend.item.ItemStack;
import com.coldfriend.player.PlayerId;
import com.coldfriend.player.PlayerInput;
import com.coldfriend.player.PlayerState;
import com.coldfriend.survival.HearthState;
import com.coldfriend.survival.HearthStatus;
import com.coldfriend.survival.Need;
import com.coldfriend.survival.SurvivalStatus;
import com.coldfriend.time.DayPhase;
import com.coldfriend.world.gen.WorldGenParameters;
import com.coldfriend.world.gen.WorldGenerator;
import com.coldfriend.world.gen.WorldSeed;
import com.coldfriend.world.model.CabinLayout;
import com.coldfriend.world.model.LocationKind;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.model.WorldLocation;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The clock, needs and hearth working together inside the real simulation, with the shipped item data. */
class SurvivalSimulationTest {

    private static final ItemId FIREWOOD = new ItemId("firewood");
    private static final ItemId BRANCH = new ItemId("branch");
    private static final ItemId MATCHES = new ItemId("matches");
    private static final ItemId RATION = new ItemId("ration");
    private static final ItemId WATER_FULL = new ItemId("water_container");
    private static final ItemId WATER_EMPTY = new ItemId("water_container_empty");
    /** One update of exactly the step cap, so long runs are quick: 0.25 s is five steps. */
    private static final float BIG_FRAME = 0.25f;
    private static WorldData world;
    private static ItemCatalog items;

    @BeforeAll
    static void setUp() throws IOException {
        world = WorldGenerator.generate(new WorldSeed(404L), WorldGenParameters.DEFAULT);
        items = ItemCatalog.load(Path.of("config", "items.properties"));
    }

    private static WorldSimulation simulation(int startMinute) {
        return new WorldSimulation(world, items, startMinute);
    }

    /** Puts the player in front of the stove, facing the door, so the stove is the nearest thing to use. */
    private static PlayerState byTheStove(WorldSimulation simulation) {
        for (CabinLayout.Furnishing f : CabinLayout.furnishings(world.cabin().footprint())) {
            if ("stove".equals(f.id())) {
                float[] spot = CabinLayout.point(world.cabin(), f.part().side(), f.part().forward() + 1.0f);
                return simulation.addPlayer(PlayerId.LOCAL,
                        new SpawnPoints.Spawn(spot[0], spot[1], world.cabin().footprint().yaw()));
            }
        }
        throw new IllegalStateException("no stove");
    }

    /** Runs the simulation for some in-game minutes with the player standing still. */
    private static void runMinutes(WorldSimulation simulation, double minutes) {
        long steps = Math.round(minutes / 0.06);
        for (long i = 0; i < steps; i += FixedStep.MAX_STEPS_PER_UPDATE) {
            simulation.update(BIG_FRAME, Map.of());
        }
    }

    private static ActionResult tendStove(WorldSimulation simulation) {
        Interactable target = simulation.interactableFor(PlayerId.LOCAL).orElseThrow();
        assertEquals(InteractableRef.CABIN_HEARTH, target.ref());
        return simulation.interact(new InteractionRequest(PlayerId.LOCAL, target.ref()));
    }

    private static int slotOf(Inventory inventory, ItemId item) {
        for (int i = 0; i < inventory.size(); i++) {
            Optional<ItemStack> stack = inventory.slot(i);
            if (stack.isPresent() && stack.get().item().equals(item)) {
                return i;
            }
        }
        return -1;
    }

    @Test
    void aNewGameStartsAtSevenOnDayOneWithTheStoveLit() {
        WorldSimulation simulation = new WorldSimulation(world, items);
        assertEquals(1, simulation.day());
        assertEquals(420.0, simulation.minuteOfDay(), 1e-9);
        assertEquals(DayPhase.DAY, simulation.phase());
        assertEquals(HearthState.BURNING, simulation.hearth().state());
    }

    @Test
    void feedingTheStoveMovesExactlyOneFirewoodIntoTheFuel() {
        WorldSimulation simulation = simulation(420);
        byTheStove(simulation);
        Inventory inventory = simulation.inventory(PlayerId.LOCAL);
        int wood = inventory.count(FIREWOOD);
        double fuel = simulation.hearth().fuel();

        assertTrue(tendStove(simulation).success());

        assertEquals(wood - 1, inventory.count(FIREWOOD));
        assertEquals(fuel + 1.0, simulation.hearth().fuel(), 1e-9);
    }

    @Test
    void aFullStoveRefusesFirewoodAndKeepsItInTheInventory() {
        WorldSimulation simulation = simulation(420);
        byTheStove(simulation);
        Inventory inventory = simulation.inventory(PlayerId.LOCAL);
        inventory.add(new ItemStack(FIREWOOD, 6));
        while (simulation.hearth().fuel() + 1.0 <= simulation.hearth().maxFuel()) {
            assertTrue(tendStove(simulation).success());
        }
        int wood = inventory.count(FIREWOOD);
        double fuel = simulation.hearth().fuel();
        int total = wood + (int) Math.round(fuel);

        ActionResult result = tendStove(simulation);

        assertFalse(result.success());
        assertEquals(wood, inventory.count(FIREWOOD), "no firewood lost");
        assertEquals(fuel, simulation.hearth().fuel(), "no fuel gained");
        assertEquals(total, inventory.count(FIREWOOD) + (int) Math.round(simulation.hearth().fuel()));
    }

    @Test
    void withNoFirewoodTheStoveSaysSo() {
        WorldSimulation simulation = simulation(420);
        byTheStove(simulation);
        Inventory inventory = simulation.inventory(PlayerId.LOCAL);
        inventory.remove(FIREWOOD, inventory.count(FIREWOOD));
        double fuel = simulation.hearth().fuel();

        assertFalse(tendStove(simulation).success());
        assertEquals(fuel, simulation.hearth().fuel());
    }

    @Test
    void aStoveThatWentOutIsRelitWithMatchesAndOneBranch() {
        WorldSimulation simulation = simulation(420);
        byTheStove(simulation);
        runMinutes(simulation, 1200);
        assertEquals(HearthState.OUT, simulation.hearth().state());
        Inventory inventory = simulation.inventory(PlayerId.LOCAL);

        assertTrue(tendStove(simulation).success(), "put wood in the cold stove");
        assertEquals(HearthState.OUT, simulation.hearth().state(), "wood alone does not light it");

        int branches = inventory.count(BRANCH);
        int matches = inventory.count(MATCHES);
        int wood = inventory.count(FIREWOOD);
        inventory.remove(MATCHES, matches);
        assertFalse(tendStove(simulation).success(), "no matches");
        assertEquals(branches, inventory.count(BRANCH));
        assertEquals(wood, inventory.count(FIREWOOD), "a failed light does not take firewood either");
        assertEquals(HearthState.OUT, simulation.hearth().state());

        inventory.add(new ItemStack(MATCHES, 1));
        assertTrue(tendStove(simulation).success());
        assertEquals(HearthState.BURNING, simulation.hearth().state());
        assertEquals(branches - 1, inventory.count(BRANCH));
        assertEquals(1, inventory.count(MATCHES), "the box of matches is kept");
    }

    @Test
    void theFireKeepsTheCabinWarmerThanAColdStove() {
        WorldSimulation warm = simulation(1200);
        byTheStove(warm);
        Inventory inventory = warm.inventory(PlayerId.LOCAL);
        inventory.add(new ItemStack(FIREWOOD, 3));
        tendStove(warm);
        tendStove(warm);
        tendStove(warm);

        WorldSimulation cold = simulation(1200);
        byTheStove(cold);
        runMinutes(cold, 1200);
        assertEquals(HearthState.OUT, cold.hearth().state());
        runMinutes(warm, 60);
        runMinutes(cold, 60);

        assertTrue(warm.survival(PlayerId.LOCAL).warmth() > 80, "by a fire: " + warm.survival(PlayerId.LOCAL));
        assertEquals(25, cold.survival(PlayerId.LOCAL).warmth(), 1e-6, "a cold cabin settles at 25");
    }

    @Test
    void eatingUsesOneRationAndRestoresNourishment() {
        WorldSimulation simulation = simulation(420);
        byTheStove(simulation);
        Inventory inventory = simulation.inventory(PlayerId.LOCAL);
        int slot = slotOf(inventory, RATION);
        int rations = inventory.count(RATION);
        assertFalse(simulation.useItem(PlayerId.LOCAL, slot).success(), "not hungry when full");
        assertEquals(rations, inventory.count(RATION));

        runMinutes(simulation, 720);
        double before = simulation.survival(PlayerId.LOCAL).nourishment();
        assertTrue(simulation.useItem(PlayerId.LOCAL, slotOf(inventory, RATION)).success());
        assertEquals(rations - 1, inventory.count(RATION));
        assertEquals(Math.min(100, before + 45), simulation.survival(PlayerId.LOCAL).nourishment(), 1e-9);
    }

    @Test
    void drinkingEmptiesTheContainerAndFillingNeedsWater() {
        WorldSimulation simulation = simulation(420);
        byTheStove(simulation);
        Inventory inventory = simulation.inventory(PlayerId.LOCAL);
        runMinutes(simulation, 600);
        double before = simulation.survival(PlayerId.LOCAL).hydration();

        assertTrue(simulation.useItem(PlayerId.LOCAL, slotOf(inventory, WATER_FULL)).success());
        assertEquals(before + 30, simulation.survival(PlayerId.LOCAL).hydration(), 1e-9);
        assertEquals(0, inventory.count(WATER_FULL));
        assertEquals(1, inventory.count(WATER_EMPTY));

        ActionResult dry = simulation.useItem(PlayerId.LOCAL, slotOf(inventory, WATER_EMPTY));
        assertFalse(dry.success(), "there is no water in the cabin");
        assertEquals(1, inventory.count(WATER_EMPTY));
    }

    @Test
    void anEmptyContainerFillsAtTheRiver() {
        WorldSimulation simulation = simulation(420);
        float[] wet = waterNearAccessPoint();
        assertNotNull(wet, "seed 404 should have water near a water access point");
        simulation.addPlayer(PlayerId.LOCAL, new SpawnPoints.Spawn(wet[0], wet[1], 0f));
        Inventory inventory = simulation.inventory(PlayerId.LOCAL);
        inventory.remove(WATER_FULL, inventory.count(WATER_FULL));
        inventory.add(new ItemStack(WATER_EMPTY, 1));

        assertTrue(simulation.useItem(PlayerId.LOCAL, slotOf(inventory, WATER_EMPTY)).success());
        assertEquals(1, inventory.count(WATER_FULL));
        assertEquals(0, inventory.count(WATER_EMPTY));
    }

    private static float[] waterNearAccessPoint() {
        for (WorldLocation location : world.locations()) {
            if (location.kind() != LocationKind.WATER_ACCESS) {
                continue;
            }
            for (float r = 0f; r <= 8f; r += 0.5f) {
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8.0;
                    float x = location.x() + (float) Math.sin(a) * r;
                    float z = location.z() + (float) Math.cos(a) * r;
                    if (world.water().depthAt(world.terrain(), x, z) > 0.1f) {
                        return new float[] {x, z};
                    }
                }
            }
        }
        return null;
    }

    @Test
    void nothingChangesWhileTheSimulationIsNotUpdated() {
        WorldSimulation simulation = simulation(420);
        byTheStove(simulation);
        runMinutes(simulation, 30);
        double minute = simulation.minuteOfDay();
        SurvivalStatus needs = simulation.survival(PlayerId.LOCAL);
        HearthStatus fire = simulation.hearth();

        // Paused: the play screen stops calling update. On resume it resets frame timing.
        simulation.resetFrameTiming();
        simulation.update(0.01f, Map.of());

        assertEquals(minute, simulation.minuteOfDay());
        assertEquals(needs, simulation.survival(PlayerId.LOCAL));
        assertEquals(fire, simulation.hearth());
    }

    @Test
    void clockNeedsAndFireDoNotDependOnFrameRate() {
        WorldSimulation a = simulation(1000);
        WorldSimulation b = simulation(1000);
        a.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, "cabindoor"));
        b.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, "cabindoor"));
        PlayerInput walk = new PlayerInput(1f, 0f, 0f, 0f, false);
        for (int i = 0; i < 32 * 90; i++) {
            a.update(1f / 32f, Map.of(PlayerId.LOCAL, walk));
        }
        for (int i = 0; i < 128 * 90; i++) {
            b.update(1f / 128f, Map.of(PlayerId.LOCAL, walk));
        }
        assertEquals(a.minuteOfDay(), b.minuteOfDay());
        assertEquals(a.survival(PlayerId.LOCAL), b.survival(PlayerId.LOCAL));
        assertEquals(a.hearth(), b.hearth());
        assertEquals(a.player(PlayerId.LOCAL).x(), b.player(PlayerId.LOCAL).x());
    }

    @Test
    void aPlayerLeftOutInTheNightColdDiesAfterAWarningAndThenCannotAct() {
        WorldSimulation simulation = simulation(22 * 60);
        PlayerState player = simulation.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, "village"));
        boolean warned = false;
        boolean warnedBeforeHarm = false;
        for (int i = 0; i < 20_000 && !simulation.survival(PlayerId.LOCAL).dead(); i++) {
            simulation.update(BIG_FRAME, Map.of());
            SurvivalStatus s = simulation.survival(PlayerId.LOCAL);
            if (s.low(Need.WARMTH) && !warned) {
                warned = true;
                warnedBeforeHarm = s.health() == 100.0;
            }
        }
        assertTrue(simulation.survival(PlayerId.LOCAL).dead());
        assertTrue(warnedBeforeHarm, "the warmth warning came before any health was lost");
        float x = player.x();
        float z = player.z();
        for (int i = 0; i < 20; i++) {
            simulation.update(BIG_FRAME, Map.of(PlayerId.LOCAL, new PlayerInput(1f, 0f, 0f, 0f, true)));
        }
        assertEquals(x, player.x());
        assertEquals(z, player.z());
    }

    @Test
    void refsAreStableAcrossCallsAndSimulations() {
        WorldSimulation first = simulation(420);
        byTheStove(first);
        WorldSimulation second = simulation(420);
        byTheStove(second);
        assertEquals(first.interactableFor(PlayerId.LOCAL).orElseThrow().ref(),
                first.interactableFor(PlayerId.LOCAL).orElseThrow().ref());
        assertEquals(first.interactableFor(PlayerId.LOCAL).orElseThrow(),
                second.interactableFor(PlayerId.LOCAL).orElseThrow());
    }

    @Test
    void aRequestForSomethingOutOfReachIsRefused() {
        WorldSimulation simulation = simulation(420);
        simulation.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, "village"));
        double fuel = simulation.hearth().fuel();
        int wood = simulation.inventory(PlayerId.LOCAL).count(FIREWOOD);
        assertFalse(simulation.interact(new InteractionRequest(PlayerId.LOCAL, InteractableRef.CABIN_HEARTH))
                .success());
        assertEquals(fuel, simulation.hearth().fuel());
        assertEquals(wood, simulation.inventory(PlayerId.LOCAL).count(FIREWOOD));
    }
}
