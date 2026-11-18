package com.coldfriend.creature;

/**
 * Something the creature noticed. Its confidence falls with age; see {@link Memory#confidence}.
 *
 * @param kind what was noticed
 * @param x approximate world x, in metres
 * @param z approximate world z, in metres
 * @param minute game time the observation was last refreshed, in minutes
 * @param startConfidence confidence when it was first made, from 0 to 1
 */
public record Observation(ObservationKind kind, float x, float z, double minute, float startConfidence) {
}
