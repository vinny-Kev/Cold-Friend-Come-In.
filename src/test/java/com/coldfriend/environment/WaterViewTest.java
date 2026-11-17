package com.coldfriend.environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.world.GeneratedWorld;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import org.junit.jupiter.api.Test;

/** Builds real water geometry headlessly (no GL context) and checks creation and cleanup. */
class WaterViewTest {

    private static AssetManager assets() {
        return new DesktopAssetManager(true);
    }

    @Test
    void buildsOneSharedMaterialGeometryPerBody() {
        GeneratedWorld world = GeneratedWorld.generate(42L);
        WaterView view = new WaterView(assets(), world, SceneLighting.day());

        assertEquals(world.waterBodies().size(), view.geometryCount());
        Geometry first = (Geometry) view.node().getChild(0);
        for (Spatial child : view.node().getChildren()) {
            assertTrue(((Geometry) child).getMaterial() == first.getMaterial(), "one shared material");
        }
        view.dispose();
    }

    @Test
    void meshesCarryPatternCoordinatesAndPerVertexWaterInfo() {
        GeneratedWorld world = GeneratedWorld.generate(7L);
        WaterView view = new WaterView(assets(), world, SceneLighting.night());

        for (Spatial child : view.node().getChildren()) {
            Mesh mesh = ((Geometry) child).getMesh();
            assertNotNull(mesh.getBuffer(VertexBuffer.Type.Position));
            assertNotNull(mesh.getBuffer(VertexBuffer.Type.TexCoord));
            assertNotNull(mesh.getBuffer(VertexBuffer.Type.Color));
            assertTrue(mesh.getTriangleCount() > 0);
        }
        view.dispose();
    }

    @Test
    void disposeDetachesFromTheSceneAndEmptiesTheView() {
        GeneratedWorld world = GeneratedWorld.generate(1L);
        Node root = new Node("root");
        WaterView view = new WaterView(assets(), world, SceneLighting.day());
        root.attachChild(view.node());
        assertEquals(1, root.getQuantity());

        view.dispose();

        assertEquals(0, root.getQuantity());
        assertNull(view.node().getParent());
        assertEquals(0, view.geometryCount());
    }

    @Test
    void lightingCanChangeWithoutRebuildingGeometry() {
        GeneratedWorld world = GeneratedWorld.generate(1L);
        WaterView view = new WaterView(assets(), world, SceneLighting.day());
        int before = view.geometryCount();

        view.setLighting(SceneLighting.night());

        assertEquals(before, view.geometryCount());
        view.dispose();
    }
}
