package sashwind.mc.plugin.ffcraft.model.mesh;

import sashwind.mc.plugin.ffcraft.model.ScreenVertex;
import sashwind.mc.plugin.ffcraft.model.UvTransform;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the complete computed geometric mesh data of a video screen.
 * Divided into three categories:
 * 1. Raw Data: original positions (x, y, z) and player viewpoint angles (pitch, yaw).
 * 2. Computed Topology: triangle faces composition, point winding order (indices), and face normals.
 * 3. UV Mapping Data: UV transform configuration and computed (u, v) coordinates per vertex.
 */
public record ComputedScreenMesh(
    // 1. Raw data
    List<ScreenVertex> rawVertices,

    // 2. Computed topology & orientation
    List<TriangleFace> faces,
    List<Integer> triangleIndices,
    double normalX,
    double normalY,
    double normalZ,
    boolean reversedForViewer,

    // 3. UV mapping data
    UvTransform uvTransform,
    List<ComputedVertex> computedVertices
) {
    public List<ScreenVertex> getRawVertices() {
        return Collections.unmodifiableList(rawVertices);
    }

    public List<TriangleFace> getFaces() {
        return Collections.unmodifiableList(faces);
    }

    public List<Integer> getTriangleIndices() {
        return Collections.unmodifiableList(triangleIndices);
    }

    public List<ComputedVertex> getComputedVertices() {
        return Collections.unmodifiableList(computedVertices);
    }

    /**
     * Gets the number of decomposed triangle faces.
     */
    public int getFaceCount() {
        return faces.size();
    }
}
