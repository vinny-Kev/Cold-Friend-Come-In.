package com.coldfriend.environment;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.Buffer;

/** Frees the direct (off-heap) buffers of meshes this package builds, which the garbage collector does not. */
public final class MeshCleanup {

    private MeshCleanup() {
    }

    /**
     * Destroys every direct vertex buffer of a mesh. The mesh must not be rendered afterwards.
     *
     * @param mesh mesh to release
     */
    public static void release(Mesh mesh) {
        for (VertexBuffer buffer : mesh.getBufferList()) {
            Buffer data = buffer.getData();
            if (data != null && data.isDirect()) {
                BufferUtils.destroyDirectBuffer(data);
            }
        }
    }
}
