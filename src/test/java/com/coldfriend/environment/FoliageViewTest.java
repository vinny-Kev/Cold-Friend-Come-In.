package com.coldfriend.environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.world.BushPlacer;
import com.coldfriend.world.BushSpawn;
import com.coldfriend.world.GeneratedWorld;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Builds real foliage geometry headlessly (no GL context) and checks batching and cleanup. */
class FoliageViewTest {

    private static AssetManager assets() {
        return new DesktopAssetManager(true);
    }

    @Test
    void bushesAreBatchedIntoFewGeometriesSharingOneMaterial() {
        List<BushSpawn> bushes = BushPlacer.place(GeneratedWorld.generate(42L));
        FoliageView view = new FoliageView(assets(), bushes, SceneLighting.day(), Wind.breezy());

        int geometries = view.geometryCount();
        assertTrue(geometries >= 1 && geometries <= 16, "batches: " + geometries);
        assertTrue(geometries < bushes.size() / 10, "far fewer draw calls than bushes");
        Geometry first = (Geometry) view.node().getChild(0);
        int triangles = 0;
        for (Spatial child : view.node().getChildren()) {
            Geometry g = (Geometry) child;
            assertTrue(g.getMaterial() == first.getMaterial(), "one shared material");
            triangles += g.getMesh().getTriangleCount();
        }
        assertTrue(triangles > bushes.size(), "every bush contributed triangles");
        view.dispose();
    }

    @Test
    void noBushesMeansNoGeometry() {
        FoliageView view = new FoliageView(assets(), List.of(), SceneLighting.day(), Wind.calm());

        assertEquals(0, view.geometryCount());
        view.dispose();
    }

    @Test
    void disposeDetachesFromTheSceneAndEmptiesTheView() {
        Node root = new Node("root");
        FoliageView view = new FoliageView(assets(), BushPlacer.place(GeneratedWorld.generate(1L)),
                SceneLighting.day(), Wind.storm());
        root.attachChild(view.node());
        assertEquals(1, root.getQuantity());

        view.dispose();

        assertEquals(0, root.getQuantity());
        assertNull(view.node().getParent());
        assertEquals(0, view.geometryCount());
    }

    @Test
    void windAndLightingChangeWithoutRebuildingGeometry() {
        FoliageView view = new FoliageView(assets(), BushPlacer.place(GeneratedWorld.generate(1L)),
                SceneLighting.day(), Wind.calm());
        int before = view.geometryCount();

        view.setWind(Wind.storm());
        view.setLighting(SceneLighting.night());

        assertEquals(before, view.geometryCount());
        view.dispose();
    }
}
