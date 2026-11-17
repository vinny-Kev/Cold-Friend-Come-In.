package com.coldfriend.environment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.world.BushPlacer;
import org.junit.jupiter.api.Test;

class BushMeshBuilderTest {

    @Test
    void sameVariantGivesTheSameShape() {
        assertArrayEquals(BushMeshBuilder.template(1).positions(), BushMeshBuilder.template(1).positions());
    }

    @Test
    void variantsDiffer() {
        assertTrue(!java.util.Arrays.equals(BushMeshBuilder.template(0).positions(),
                BushMeshBuilder.template(1).positions()));
    }

    @Test
    void swayWeightGrowsWithHeightAndStaysInRange() {
        for (int variant = 0; variant < BushPlacer.VARIANTS; variant++) {
            BushMeshBuilder.Template t = BushMeshBuilder.template(variant);
            float lowestY = Float.POSITIVE_INFINITY;
            float highestY = Float.NEGATIVE_INFINITY;
            float weightAtLowest = 0f;
            float weightAtHighest = 0f;
            for (int i = 0; i < t.vertexCount(); i++) {
                float y = t.positions()[i * 3 + 1];
                float weight = t.texCoords()[i * 2];
                assertTrue(weight >= 0f && weight <= 1f, "weight in range");
                float phase = t.texCoords()[i * 2 + 1];
                assertTrue(phase >= 0f && phase < 1f, "phase in range");
                if (y < lowestY) {
                    lowestY = y;
                    weightAtLowest = weight;
                }
                if (y > highestY) {
                    highestY = y;
                    weightAtHighest = weight;
                }
            }
            assertTrue(lowestY >= 0f, "bush stands on the ground, variant " + variant);
            assertTrue(weightAtLowest < 0.05f, "roots barely move, variant " + variant);
            assertTrue(weightAtHighest > weightAtLowest + 0.2f, "tips move more than roots");
        }
    }

    @Test
    void normalsAreUnitAndFacesPointOutward() {
        BushMeshBuilder.Template t = BushMeshBuilder.template(2);
        for (int i = 0; i < t.vertexCount(); i++) {
            float nx = t.normals()[i * 3];
            float ny = t.normals()[i * 3 + 1];
            float nz = t.normals()[i * 3 + 2];
            assertEquals(1f, Math.sqrt(nx * nx + ny * ny + nz * nz), 1e-4);
        }
    }
}
