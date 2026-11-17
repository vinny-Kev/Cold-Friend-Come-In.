package com.coldfriend.creature;

import com.coldfriend.entity.EntityId;
import com.coldfriend.player.PlayerState;
import com.coldfriend.time.DayPhase;
import com.coldfriend.world.gen.RandomStreams;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.query.WorldCollision;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;

/**
 * The creature: its state, senses, memory, navigation and brain, run once per fixed simulation step. Owned by
 * {@link com.coldfriend.world.WorldSimulation}, which calls {@link #step} after the player and the clock.
 *
 * <p>Order inside a step: move along the route; hear this step's sounds; every few steps look at the players; every
 * second, forget what has decayed and decide. Hearing runs on every step because sense events are only kept for one
 * step; sight runs less often because it is the expensive check.
 */
public final class CreatureSimulation {

    /**
     * The creature's identity. Items number from 1 upward and the creature is not an item, so it has a fixed id far
     * above anything the item counter will reach. Keeping the counter untouched keeps item ids stable.
     */
    public static final EntityId ID = new EntityId(1_000_000_000L);

    private final CreatureState creature;
    private final Memory memory = new Memory();
    private final Perception perception;
    private final CreatureNavigator navigator;
    private final CreatureBrain brain;
    private long steps;

    private CreatureSimulation(CreatureState creature, Perception perception, CreatureNavigator navigator,
            CreatureBrain brain) {
        this.creature = creature;
        this.perception = perception;
        this.navigator = navigator;
        this.brain = brain;
    }

    /**
     * Places a creature somewhere it may start: far from the cabin, outside the village, on ground it can stand on.
     * The spot is chosen from the world seed, so the same seed always starts the creature in the same place.
     *
     * @param world the generated world
     * @param collision scenery and buildings
     * @param id the creature's identity
     * @param streams the world's random streams
     * @return the creature, or empty if no spot was found (a world with no valid start is reported by the caller)
     */
    public static Optional<CreatureSimulation> spawn(WorldData world, WorldCollision collision, EntityId id,
            RandomStreams streams) {
        Territory territory = new Territory(world, collision);
        SplittableRandom random = streams.random("creature.spawn");
        float cabinX = world.cabin().footprint().x();
        float cabinZ = world.cabin().footprint().z();
        for (int attempt = 0; attempt < CreatureParameters.SPAWN_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * 2.0 * Math.PI;
            double span = CreatureParameters.SPAWN_MAX_DISTANCE - CreatureParameters.SPAWN_MIN_DISTANCE;
            double distance = CreatureParameters.SPAWN_MIN_DISTANCE + random.nextDouble() * span;
            float x = cabinX + (float) (Math.sin(angle) * distance);
            float z = cabinZ + (float) (Math.cos(angle) * distance);
            if (territory.spawnAllowed(x, z) && territory.standable(x, z)) {
                float heading = (float) (random.nextDouble() * 2.0 * Math.PI);
                CreatureState creature = new CreatureState(id, x, z, heading);
                CreatureNavigator navigator = new CreatureNavigator(world, collision);
                CreatureBrain brain = new CreatureBrain(territory, navigator, streams.random("creature.wander"));
                Perception perception = new Perception(world, collision, streams.random("creature.hearing"));
                return Optional.of(new CreatureSimulation(creature, perception, navigator, brain));
            }
        }
        return Optional.empty();
    }

    /**
     * Advances the creature by one fixed step.
     *
     * @param dt step length, in seconds
     * @param players every player; the creature may look at them, but its decisions never see them
     * @param senses what the players did in this step, as heard
     * @param phase the time of day
     * @param minute game time, in minutes
     */
    public void step(float dt, Collection<PlayerState> players, List<SenseEvent> senses, DayPhase phase,
            double minute) {
        steps++;
        navigator.step(creature, CreatureParameters.WANDER_SPEED, dt);
        perception.hear(creature, senses, phase, memory);
        if (steps % CreatureParameters.PERCEPTION_EVERY_STEPS == 0) {
            for (PlayerState player : players) {
                perception.look(creature, player, phase, memory, minute);
            }
        }
        if (steps == 1 || steps % CreatureParameters.BRAIN_EVERY_STEPS == 0) {
            memory.forgetExpired(minute);
            brain.think(creature, minute);
        }
    }

    /** @return a snapshot for the debug overlay */
    public CreatureStatus status() {
        return new CreatureStatus(creature.x(), creature.z(), creature.heading(), creature.mode(), memory.size(),
                navigator.remaining());
    }

    /** @return the creature's identity */
    public EntityId id() {
        return creature.id();
    }

    /** @return what the creature remembers; read-only in spirit: only perception writes to it */
    public Memory memory() {
        return memory;
    }
}
