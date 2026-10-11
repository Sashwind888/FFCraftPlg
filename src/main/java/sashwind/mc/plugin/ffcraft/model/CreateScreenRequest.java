package sashwind.mc.plugin.ffcraft.model;

import java.util.List;
import java.util.UUID;

public record CreateScreenRequest(
    UUID playerId,
    String name,
    String dimension,
    List<ScreenVertex> vertices,
    int screenType,
    double radius
) {
    public CreateScreenRequest(UUID playerId, String name, String dimension, List<ScreenVertex> vertices) {
        this(playerId, name, dimension, vertices, 0, 5.0);
    }
}
