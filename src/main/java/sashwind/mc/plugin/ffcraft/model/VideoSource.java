package sashwind.mc.plugin.ffcraft.model;

public record VideoSource(
    String url,
    int targetWidth,
    int targetHeight,
    Integer targetFps,
    int originalWidth,
    int originalHeight
) {
    public VideoSource(String url, int targetWidth, int targetHeight, Integer targetFps) {
        this(url, targetWidth, targetHeight, targetFps, 0, 0);
    }

    public VideoSource withOriginalResolution(int width, int height) {
        return new VideoSource(url, targetWidth, targetHeight, targetFps, width, height);
    }
}
