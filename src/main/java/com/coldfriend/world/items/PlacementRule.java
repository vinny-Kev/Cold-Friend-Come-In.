package com.coldfriend.world.items;

import com.coldfriend.world.model.Footprint;
import java.util.Optional;

/**
 * One rule about where an item may go. A rule looks at a {@link Spot} and either accepts it (empty) or names the
 * problem. To add a rule, write one and add it to the list in {@link PlacementValidator}.
 */
@FunctionalInterface
public interface PlacementRule {

    /**
     * The spot being checked.
     *
     * @param x world x of the item's centre
     * @param z world z of the item's centre
     * @param radius how much room the item needs around its centre
     * @param restY height its base would rest at
     * @param surface the table top it would rest on, or null if it rests on the ground or floor
     * @param playerX world x of the player asking
     * @param playerZ world z of the player asking
     */
    record Spot(float x, float z, float radius, float restY, Footprint surface, float playerX, float playerZ) {

        /** @return true if the item would rest on a raised surface rather than the ground or floor */
        public boolean onSurface() {
            return surface != null;
        }
    }

    /**
     * @param spot where the item would go
     * @return the problem, or empty if this rule is satisfied
     */
    Optional<PlacementProblem> check(Spot spot);
}
