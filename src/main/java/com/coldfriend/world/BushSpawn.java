package com.coldfriend.world;

/**
 * Where one bush stands. Pure data: no mesh, no engine types.
 *
 * @param x world x
 * @param y ground height at the bush
 * @param z world z
 * @param yaw rotation about the vertical axis, radians
 * @param scale size multiplier
 * @param variant which bush shape to use, 0 to {@link BushPlacer#VARIANTS} - 1
 * @param tint brightness multiplier for colour variation
 */
public record BushSpawn(float x, float y, float z, float yaw, float scale, int variant, float tint) {
}
