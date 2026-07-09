package sashwind.mc.plugin.ffcraft.model;

import java.util.List;

public record VideoPlayerSnapshot(List<VideoPlayerData> players) {
    public static VideoPlayerSnapshot empty() {
        return new VideoPlayerSnapshot(List.of());
    }
}
