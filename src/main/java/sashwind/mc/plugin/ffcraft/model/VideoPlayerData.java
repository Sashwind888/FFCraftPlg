package sashwind.mc.plugin.ffcraft.model;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Client-side view — protocol MUST stay compatible with existing client mod. */
public record VideoPlayerData(
    UUID id,
    String name,
    boolean isPublic,
    Set<UUID> editors,
    List<VideoSource> playlist,
    PlaybackState playbackState,
    List<VideoScreenData> screens
) {}
