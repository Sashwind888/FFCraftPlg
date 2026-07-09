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
    ScreenChannelState channelState
) {
    public ServerVideoScreen withName(String newName) {
        return new ServerVideoScreen(id, playerId, newName, dimension, vertices, uvTransform, channelState);
    }

    public ServerVideoScreen withUvTransform(UvTransform newUv) {
        return new ServerVideoScreen(id, playerId, name, dimension, vertices, newUv, channelState);
    }

    public ServerVideoScreen withChannelState(ScreenChannelState newChannel) {
        return new ServerVideoScreen(id, playerId, name, dimension, vertices, uvTransform, newChannel);
    }

    public ServerVideoScreen withVertices(List<ScreenVertex> newVertices) {
        return new ServerVideoScreen(id, playerId, name, dimension, newVertices, uvTransform, channelState);
    }
}
