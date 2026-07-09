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
    ScreenChannelState channelState
) {}
