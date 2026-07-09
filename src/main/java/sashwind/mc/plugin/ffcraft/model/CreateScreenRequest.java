package sashwind.mc.plugin.ffcraft.model;

import java.util.List;
import java.util.UUID;

public record CreateScreenRequest(
    UUID playerId,
    String name,
    String dimension,
    List<ScreenVertex> vertices
) {}
