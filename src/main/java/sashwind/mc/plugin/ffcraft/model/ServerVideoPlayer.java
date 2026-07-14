package sashwind.mc.plugin.ffcraft.model;

import java.util.*;

/**
 * Server-side video player record (immutable).
 *
 * creator       — the player who created this player (owner). Has full manage permissions.
 * controlUsers  — playback-only users. NOT sent to clients. Persisted server-side.
 */
public record ServerVideoPlayer(
    UUID id,
    String name,
    boolean isPublic,
    UUID creator,
    Set<UUID> controlUsers,
    List<VideoSource> playlist,
    PlaybackState playbackState,
    List<ServerVideoScreen> screens
) {
    public ServerVideoPlayer withName(String newName) {
        return new ServerVideoPlayer(id, newName, isPublic, creator, controlUsers, playlist, playbackState, screens);
    }

    public ServerVideoPlayer withPublic(boolean isPublic) {
        return new ServerVideoPlayer(id, name, isPublic, creator, controlUsers, playlist, playbackState, screens);
    }

    public ServerVideoPlayer withPlaybackState(PlaybackState newState) {
        return new ServerVideoPlayer(id, name, isPublic, creator, controlUsers, playlist, newState, screens);
    }

    public ServerVideoPlayer withPlaylist(List<VideoSource> newPlaylist) {
        return new ServerVideoPlayer(id, name, isPublic, creator, controlUsers, newPlaylist, playbackState, screens);
    }

    public ServerVideoPlayer withScreens(List<ServerVideoScreen> newScreens) {
        return new ServerVideoPlayer(id, name, isPublic, creator, controlUsers, playlist, playbackState, newScreens);
    }

    public ServerVideoPlayer withControlUsers(Set<UUID> newControlUsers) {
        return new ServerVideoPlayer(id, name, isPublic, creator, newControlUsers, playlist, playbackState, screens);
    }

    public ServerVideoPlayer grantControl(UUID user) {
        Set<UUID> updated = new HashSet<>(controlUsers);
        updated.add(user);
        return withControlUsers(Collections.unmodifiableSet(updated));
    }

    public ServerVideoPlayer revokeControl(UUID user) {
        Set<UUID> updated = new HashSet<>(controlUsers);
        updated.remove(user);
        return withControlUsers(Collections.unmodifiableSet(updated));
    }

    public void validate() {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(creator, "creator must not be null");
        Objects.requireNonNull(playbackState, "playbackState must not be null");
    }

    public static ServerVideoPlayer create(String name, boolean isPublic, UUID creator) {
        return new ServerVideoPlayer(
            UUID.randomUUID(),
            name,
            isPublic,
            creator,
            Collections.emptySet(),
            new ArrayList<>(),
            PlaybackState.createDefault(),
            new ArrayList<>()
        );
    }
}
