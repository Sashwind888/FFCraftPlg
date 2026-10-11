package sashwind.mc.plugin.ffcraft.mesh;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sashwind.mc.plugin.ffcraft.model.ScreenVertex;
import sashwind.mc.plugin.ffcraft.model.UvTransform;
import sashwind.mc.plugin.ffcraft.model.mesh.ComputedScreenMesh;
import sashwind.mc.plugin.ffcraft.model.mesh.ComputedVertex;
import sashwind.mc.plugin.ffcraft.model.mesh.TriangleFace;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("屏幕几何网格剖分与数据计算测试")
public class ScreenMeshCalculatorTest {

    @Test
    @DisplayName("测试矩形屏幕：验证面拆分、点顺序与面的组成")
    void testQuadScreenMesh() {
        // 创建一个立在世界 XY 平面上的 4x2 矩形屏幕 (Z = 0)
        // 4个点：左下(0,0,0) -> 右下(4,0,0) -> 右上(4,2,0) -> 左上(0,2,0)
        // 视线 pitch=0, yaw=180 (从正面朝南看)
        List<ScreenVertex> vertices = List.of(
            new ScreenVertex(0, 0, 0, 0, 180),
            new ScreenVertex(4, 0, 0, 0, 180),
            new ScreenVertex(4, 2, 0, 0, 180),
            new ScreenVertex(0, 2, 0, 0, 180)
        );

        UvTransform uvTransform = UvTransform.createDefault();
        ComputedScreenMesh mesh = ScreenMeshCalculator.compute(vertices, uvTransform);

        // 1. 验证原始数据完整性
        assertEquals(4, mesh.getRawVertices().size());
        assertEquals(4, mesh.rawVertices().get(1).x());

        // 2. 验证三角面拆分（4边形必须拆成2个三角形）
        assertEquals(2, mesh.getFaceCount(), "四边形应该被精确拆分成 2 个三角形面");
        assertEquals(6, mesh.getTriangleIndices().size(), "2 个三角形应该有 6 个索引");

        // 3. 验证面的组成 (每个面包含3个点和有效的法向量)
        for (TriangleFace face : mesh.getFaces()) {
            assertNotNull(face.a());
            assertNotNull(face.b());
            assertNotNull(face.c());
            assertTrue(face.hasValidNormal(), "每个三角面都必须有有效的法向量");
            // 验证点索引在合法范围内
            assertTrue(face.indexA() >= 0 && face.indexA() < 4);
            assertTrue(face.indexB() >= 0 && face.indexB() < 4);
            assertTrue(face.indexC() >= 0 && face.indexC() < 4);
        }

        // 4. 验证法向量朝向（Z轴朝向）
        assertEquals(0.0, mesh.normalX(), 1e-3);
        assertEquals(0.0, mesh.normalY(), 1e-3);
        assertEquals(1.0, Math.abs(mesh.normalZ()), 1e-3, "主法向量应垂直于 XY 平面");

        // 5. 验证 UV 数据范围
        assertEquals(4, mesh.getComputedVertices().size());
        for (ComputedVertex v : mesh.getComputedVertices()) {
            assertTrue(v.u() >= -0.1 && v.u() <= 1.1, "默认UV应该在0~1附近: " + v.u());
            assertTrue(v.v() >= -0.1 && v.v() <= 1.1, "默认UV应该在0~1附近: " + v.v());
        }
    }

    @Test
    @DisplayName("测试凹多边形(L型屏幕)耳切三角剖分")
    void testConcavePolygonMesh() {
        // L 形多边形 (6个顶点)
        List<ScreenVertex> lShape = List.of(
            new ScreenVertex(0, 0, 0, 0, 0),
            new ScreenVertex(3, 0, 0, 0, 0),
            new ScreenVertex(3, 1, 0, 0, 0),
            new ScreenVertex(1, 1, 0, 0, 0),
            new ScreenVertex(1, 3, 0, 0, 0),
            new ScreenVertex(0, 3, 0, 0, 0)
        );

        ComputedScreenMesh mesh = ScreenMeshCalculator.compute(lShape, UvTransform.createDefault());

        // 6边形耳切三角剖分必须生成 6 - 2 = 4 个三角形
        assertEquals(4, mesh.getFaceCount(), "6 边形凹多边形应该拆分成 4 个三角面");
        assertEquals(12, mesh.getTriangleIndices().size());
    }

    @Test
    @DisplayName("测试 UV 变换参数应用：旋转、镜像与缩放")
    void testUvTransformEffects() {
        List<ScreenVertex> vertices = List.of(
            new ScreenVertex(0, 0, 0, 0, 0),
            new ScreenVertex(2, 0, 0, 0, 0),
            new ScreenVertex(2, 2, 0, 0, 0),
            new ScreenVertex(0, 2, 0, 0, 0)
        );

        // 默认 UV
        ComputedScreenMesh defaultMesh = ScreenMeshCalculator.compute(vertices, UvTransform.createDefault());
        double defaultU0 = defaultMesh.getComputedVertices().get(0).u();

        // 水平翻转 flipU
        UvTransform flippedTransform = new UvTransform(0, 0, 1, 1, 0, true, false, false);
        ComputedScreenMesh flippedMesh = ScreenMeshCalculator.compute(vertices, flippedTransform);
        double flippedU0 = flippedMesh.getComputedVertices().get(0).u();

        // 验证水平翻转后 U 坐标发生了对称镜像变化
        assertNotEquals(defaultU0, flippedU0, 1e-4, "水平翻转后 U 坐标应该发生镜像改变");

        // 缩放 2 倍
        UvTransform scaledTransform = new UvTransform(0, 0, 2, 2, 0, false, false, false);
        ComputedScreenMesh scaledMesh = ScreenMeshCalculator.compute(vertices, scaledTransform);
        double spanDefault = Math.abs(defaultMesh.getComputedVertices().get(1).u() - defaultU0);
        double spanScaled = Math.abs(scaledMesh.getComputedVertices().get(1).u() - scaledMesh.getComputedVertices().get(0).u());

        assertEquals(spanDefault * 2.0, spanScaled, 1e-3, "缩放2倍后UV跨度应该扩大2倍");
    }

    @Test
    @DisplayName("测试边界异常情况：少于3个顶点")
    void testDegenerateVertices() {
        // 空列表
        ComputedScreenMesh emptyMesh = ScreenMeshCalculator.compute(List.of(), null);
        assertEquals(0, emptyMesh.getFaceCount());
        assertEquals(0, emptyMesh.getRawVertices().size());

        // 仅2个点（不足以构成面）
        List<ScreenVertex> twoPoints = List.of(
            new ScreenVertex(0, 0, 0, 0, 0),
            new ScreenVertex(1, 1, 1, 0, 0)
        );
        ComputedScreenMesh smallMesh = ScreenMeshCalculator.compute(twoPoints, null);
        assertEquals(0, smallMesh.getFaceCount());
        assertEquals(2, smallMesh.getRawVertices().size());
    }
}
