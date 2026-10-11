package sashwind.mc.plugin.ffcraft.model;

import java.util.List;
import java.util.UUID;

public record ServerVideoScreen(
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
    public ServerVideoScreen(UUID id, UUID playerId, String name, String dimension,
                             List<ScreenVertex> vertices, UvTransform uvTransform, ScreenChannelState channelState) {
        this(id, playerId, name, dimension, vertices, uvTransform, channelState, 0, 5.0);
    }

    public ServerVideoScreen withName(String newName) {
        return new ServerVideoScreen(id, playerId, newName, dimension, vertices, uvTransform, channelState, screenType, radius);
    }

    public ServerVideoScreen withUvTransform(UvTransform newUv) {
        return new ServerVideoScreen(id, playerId, name, dimension, vertices, newUv, channelState, screenType, radius);
    }

    public ServerVideoScreen withChannelState(ScreenChannelState newChannel) {
        return new ServerVideoScreen(id, playerId, name, dimension, vertices, uvTransform, newChannel, screenType, radius);
    }

    public ServerVideoScreen withVertices(List<ScreenVertex> newVertices) {
        return new ServerVideoScreen(id, playerId, name, dimension, newVertices, uvTransform, channelState, screenType, radius);
    }

    public ServerVideoScreen withRadius(double newRadius) {
        return new ServerVideoScreen(id, playerId, name, dimension, vertices, uvTransform, channelState, screenType, newRadius);
    }
}
