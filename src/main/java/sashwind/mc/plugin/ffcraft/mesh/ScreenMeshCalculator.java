package sashwind.mc.plugin.ffcraft.mesh;

import earcut4j.Earcut;
import org.joml.Vector2d;
import org.joml.Vector3d;
import sashwind.mc.plugin.ffcraft.model.ScreenVertex;
import sashwind.mc.plugin.ffcraft.model.ServerVideoScreen;
import sashwind.mc.plugin.ffcraft.model.UvTransform;
import sashwind.mc.plugin.ffcraft.model.mesh.ComputedScreenMesh;
import sashwind.mc.plugin.ffcraft.model.mesh.ComputedVertex;
import sashwind.mc.plugin.ffcraft.model.mesh.TriangleFace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility for computing screen mesh topology, decomposition into triangles,
 * face orientation (normals and winding order), and final UV coordinates.
 */
public final class ScreenMeshCalculator {

    private ScreenMeshCalculator() {}

    /**
     * Computes the complete mesh data from a ServerVideoScreen.
     */
    public static ComputedScreenMesh compute(ServerVideoScreen screen) {
        return compute(screen.vertices(), screen.uvTransform());
    }

    /**
     * Computes the complete mesh data from raw vertices and UV transformation parameters.
     */
    public static ComputedScreenMesh compute(List<ScreenVertex> rawVertices, UvTransform uvTransform) {
        if (rawVertices == null || rawVertices.size() < 3) {
            return new ComputedScreenMesh(
                rawVertices != null ? rawVertices : List.of(),
                List.of(),
                List.of(),
                0, 1, 0,
                false,
                uvTransform != null ? uvTransform : UvTransform.createDefault(),
                List.of()
            );
        }

        UvTransform finalUvTransform = uvTransform != null ? uvTransform : UvTransform.createDefault();

        // 1. Convert to 3D vectors
        List<Vector3d> points = new ArrayList<>();
        double[] playerAngles = new double[rawVertices.size() * 2];
        for (int i = 0; i < rawVertices.size(); i++) {
            ScreenVertex v = rawVertices.get(i);
            points.add(new Vector3d(v.x(), v.y(), v.z()));
            playerAngles[i * 2] = v.pitch();
            playerAngles[i * 2 + 1] = v.yaw();
        }

        // 2. Normal calculation and auto-reverse winding order based on player view angles
        Vector3d normal = calculateNormal(points.get(0), points.get(1), points.get(2));
        boolean shouldReverse = shouldReverse(points, playerAngles, normal);
        if (shouldReverse) {
            Collections.reverse(points);
            // Re-calculate normal for reversed winding order
            normal = calculateNormal(points.get(0), points.get(1), points.get(2));
        }

        // 3. Project 3D points onto plane local 2D metric axes (U right, V down)
        Vector3d origin = points.get(0);
        Vector3d[] axes = calculatePlaneAxes(normal);
        Vector3d uAxis = axes[0];
        Vector3d vAxis = axes[1];

        double[] raw2D = new double[points.size() * 2];
        double minU = Double.MAX_VALUE, maxU = -Double.MAX_VALUE;
        double minV = Double.MAX_VALUE, maxV = -Double.MAX_VALUE;

        for (int i = 0; i < points.size(); i++) {
            Vector3d rel = new Vector3d(points.get(i)).sub(origin);
            double pu = rel.dot(uAxis);
            double pv = rel.dot(vAxis);
            raw2D[2 * i] = pu;
            raw2D[2 * i + 1] = pv;

            minU = Math.min(minU, pu);
            maxU = Math.max(maxU, pu);
            minV = Math.min(minV, pv);
            maxV = Math.max(maxV, pv);
        }

        // 4. Triangulation using Earcut library
        List<Integer> triangleIndices = Earcut.earcut(raw2D, null, 2);

        // 5. Calculate UVs with rigid transformation (offset, scale, rotation, flip)
        double width = Math.max(1e-4, maxU - minU);
        double height = Math.max(1e-4, maxV - minV);
        double centerU = (minU + maxU) / 2.0;
        double centerV = (minV + maxV) / 2.0;

        List<ComputedVertex> computedVertices = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            Vector3d p = points.get(i);
            double pu = raw2D[2 * i];
            double pv = raw2D[2 * i + 1];

            // Normalize relative to polygon center [-0.5, 0.5]
            double normU = (pu - centerU) / width;
            double normV = (pv - centerV) / height;

            Vector2d transformedUV = transformRigid(
                normU, normV,
                finalUvTransform.offsetU(), finalUvTransform.offsetV(),
                finalUvTransform.scaleU(), finalUvTransform.scaleV(),
                finalUvTransform.rotationDegrees(),
                finalUvTransform.flipU(), finalUvTransform.flipV()
            );

            // Shift center to [0.0, 1.0] texture space
            double finalU = transformedUV.x + 0.5;
            double finalV = transformedUV.y + 0.5;

            computedVertices.add(new ComputedVertex(
                p.x, p.y, p.z,
                finalU, finalV,
                normal.x, normal.y, normal.z
            ));
        }

        // 6. Build triangle faces (composition and orientation)
        List<TriangleFace> faces = new ArrayList<>();
        for (int i = 0; i < triangleIndices.size(); i += 3) {
            int idxA = triangleIndices.get(i);
            int idxB = triangleIndices.get(i + 1);
            int idxC = triangleIndices.get(i + 2);

            ComputedVertex a = computedVertices.get(idxA);
            ComputedVertex b = computedVertices.get(idxB);
            ComputedVertex c = computedVertices.get(idxC);

            faces.add(new TriangleFace(
                idxA, idxB, idxC,
                a, b, c,
                normal.x, normal.y, normal.z
            ));
        }

        return new ComputedScreenMesh(
            rawVertices,
            faces,
            triangleIndices,
            normal.x, normal.y, normal.z,
            shouldReverse,
            finalUvTransform,
            computedVertices
        );
    }

    // ==================== Math Helpers ====================

    private static Vector3d calculateNormal(Vector3d a, Vector3d b, Vector3d c) {
        Vector3d ab = new Vector3d(b).sub(a);
        Vector3d ac = new Vector3d(c).sub(a);
        Vector3d normal = ab.cross(ac);
        if (normal.lengthSquared() < 1e-8) {
            return new Vector3d(0, 1, 0);
        }
        return normal.normalize();
    }

    private static Vector3d[] calculatePlaneAxes(Vector3d normal) {
        Vector3d worldDown = new Vector3d(0, -1, 0);
        double dot = worldDown.dot(normal);
        Vector3d uAxis, vAxis;

        if (Math.abs(dot) < 0.95) {
            vAxis = new Vector3d(worldDown).sub(new Vector3d(normal).mul(dot)).normalize();
            uAxis = new Vector3d(vAxis).cross(normal).normalize();
        } else {
            Vector3d refForward = normal.y > 0 ? new Vector3d(0, 0, 1) : new Vector3d(0, 0, -1);
            vAxis = new Vector3d(refForward).sub(new Vector3d(normal).mul(refForward.dot(normal))).normalize();
            uAxis = new Vector3d(vAxis).cross(normal).normalize();
        }
        return new Vector3d[]{uAxis, vAxis};
    }

    private static boolean shouldReverse(List<Vector3d> points, double[] angles, Vector3d normal) {
        if (points.size() < 3 || angles.length < 2) return false;
        double sumPitch = 0, sumYaw = 0;
        for (int i = 0; i < angles.length; i += 2) {
            sumPitch += angles[i];
            sumYaw += angles[i + 1];
        }
        double avgPitch = sumPitch / (angles.length / 2.0);
        double avgYaw = sumYaw / (angles.length / 2.0);

        float radPitch = (float) Math.toRadians(avgPitch);
        float radYaw = (float) Math.toRadians(avgYaw);
        double x = -Math.sin(radYaw) * Math.cos(radPitch);
        double y = -Math.sin(radPitch);
        double z =  Math.cos(radYaw) * Math.cos(radPitch);
        Vector3d targetDir = new Vector3d(x, y, z).normalize();

        return normal.dot(targetDir) > 0;
    }

    private static Vector2d transformRigid(double normU, double normV,
                                           double offU, double offV,
                                           double scaleU, double scaleV,
                                           double rotDeg,
                                           boolean flipU, boolean flipV) {
        double ox = normU;
        double oy = normV;
        if (flipU) ox = -ox;
        if (flipV) oy = -oy;

        ox *= scaleU;
        oy *= scaleV;

        double rad = Math.toRadians(rotDeg);
        double cosA = Math.cos(rad);
        double sinA = Math.sin(rad);

        double rx = cosA * ox - sinA * oy;
        double ry = sinA * ox + cosA * oy;

        rx += offU;
        ry += offV;

        return new Vector2d(rx, ry);
    }
}
