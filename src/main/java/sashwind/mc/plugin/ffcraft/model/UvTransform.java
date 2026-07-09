package sashwind.mc.plugin.ffcraft.model;

public record UvTransform(
    double offsetU,
    double offsetV,
    double scaleU,
    double scaleV,
    double rotationDegrees,
    boolean flipU,
    boolean flipV
) {
    public static UvTransform createDefault() {
        return new UvTransform(0.0, 0.0, 1.0, 1.0, 0.0, false, true);
    }
}
