package sashwind.mc.plugin.ffcraft.model.mesh;

/**
 * Represents a single computed 3D vertex with world position, UV mapping coordinates, and normal vector.
 */
public record ComputedVertex(
    double x,
    double y,
    double z,
    double u,
    double v,
    double nx,
    double ny,
    double nz
) {
    public ComputedVertex(double x, double y, double z, double u, double v) {
        this(x, y, z, u, v, 0.0, 0.0, 1.0);
    }
}
