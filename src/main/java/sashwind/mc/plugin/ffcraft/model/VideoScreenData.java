package sashwind.mc.plugin.ffcraft.model;

import java.util.List;
import java.util.UUID;

public record VideoScreenData(
    UUID id,
    UUID playerId,
    String name,
    String dimension,
    List<ScreenVertex> vertices,
    UvTransform uvTransform,
    ScreenChannelState channelState,
    int screenType,
    double radius
) {
    public static final int TYPE_POLYGON = 0;
    public static final int TYPE_SPHERE = 1;

    public VideoScreenData(UUID id, UUID playerId, String name, String dimension,
                           List<ScreenVertex> vertices, UvTransform uvTransform, ScreenChannelState channelState) {
        this(id, playerId, name, dimension, vertices, uvTransform, channelState, TYPE_POLYGON, 5.0);
    }

    public boolean isSphere() {
        return screenType == TYPE_SPHERE;
    }
}
