package sashwind.mc.plugin.ffcraft.model;

public record UvTransform(
    double offsetU,
    double offsetV,
    double scaleU,
    double scaleV,
    double rotationDegrees,
    boolean flipU,
    boolean flipV,
    boolean is3D
) {
    public static UvTransform createDefault() {
        return new UvTransform(0.0, 0.0, 1.0, 1.0, 0.0, false, true, false);
    }

    public static UvTransform createDefault(int screenType) {
        // 球形屏幕 (screenType == 1) 默认开启水平翻转 (flipU = true)
        boolean flipU = (screenType == 1);
        return new UvTransform(0.0, 0.0, 1.0, 1.0, 0.0, flipU, true, false);
    }

    public UvTransform(double offsetU, double offsetV, double scaleU, double scaleV, double rotationDegrees, boolean flipU, boolean flipV) {
        this(offsetU, offsetV, scaleU, scaleV, rotationDegrees, flipU, flipV, false);
    }
}
