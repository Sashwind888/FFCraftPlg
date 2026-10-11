package sashwind.mc.plugin.ffcraft.model.mesh;

/**
 * Represents a single triangle face decomposed from the screen polygon/mesh.
 * Contains vertex composition (a, b, c), winding order (indices), and face normal orientation.
 */
public record TriangleFace(
    int indexA,
    int indexB,
    int indexC,
    ComputedVertex a,
    ComputedVertex b,
    ComputedVertex c,
    double normalX,
    double normalY,
    double normalZ
) {
    /**
     * Checks if the normal vector is non-zero.
     */
    public boolean hasValidNormal() {
        return (normalX * normalX + normalY * normalY + normalZ * normalZ) > 1e-6;
    }
}
