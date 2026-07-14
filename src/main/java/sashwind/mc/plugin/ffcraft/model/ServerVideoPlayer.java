package sashwind.mc.plugin.ffcraft.model;

import java.util.*;

/**
 * Server-side video player record (immutable).
 *
 * editors       — management-level users (admin-equivalent for this player).
 *                 The creator is always added to editors on createPlayer().
 * controlUsers  — playback-only users. NOT sent to clients. Persisted server-side.
 */
public record ServerVideoPlayer(
    UUID id,
    String name,
    boolean isPublic,
    Set<UUID> editors,
    Set<UUID> controlUsers,
    List<VideoSource> playlist,
    PlaybackState playbackState,
    List<ServerVideoScreen> screens
) {
    public ServerVideoPlayer withName(String newName) {
        return new ServerVideoPlayer(id, newName, isPublic, editors, controlUsers, playlist, playbackState, screens);
    }

    public ServerVideoPlayer withPublic(boolean isPublic) {
        return new ServerVideoPlayer(id, name, isPublic, editors, controlUsers, playlist, playbackState, screens);
    }

    public ServerVideoPlayer withPlaybackState(PlaybackState newState) {
        return new ServerVideoPlayer(id, name, isPublic, editors, controlUsers, playlist, newState, screens);
    }

    public ServerVideoPlayer withPlaylist(List<VideoSource> newPlaylist) {
        return new ServerVideoPlayer(id, name, isPublic, editors, controlUsers, newPlaylist, playbackState, screens);
    }

    public ServerVideoPlayer withScreens(List<ServerVideoScreen> newScreens) {
        return new ServerVideoPlayer(id, name, isPublic, editors, controlUsers, playlist, playbackState, newScreens);
    }

    public ServerVideoPlayer withEditors(Set<UUID> newEditors) {
        return new ServerVideoPlayer(id, name, isPublic, newEditors, controlUsers, playlist, playbackState, screens);
    }

    public ServerVideoPlayer withControlUsers(Set<UUID> newControlUsers) {
        return new ServerVideoPlayer(id, name, isPublic, editors, newControlUsers, playlist, playbackState, screens);
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
        Objects.requireNonNull(playbackState, "playbackState must not be null");
    }

    public static ServerVideoPlayer create(String name, boolean isPublic, UUID creator) {
        Set<UUID> editors = new LinkedHashSet<>();
        editors.add(creator);
        return new ServerVideoPlayer(
            UUID.randomUUID(),
            name,
            isPublic,
            Collections.unmodifiableSet(editors),
            Collections.emptySet(),
            new ArrayList<>(),
            PlaybackState.createDefault(),
            new ArrayList<>()
        );
    }
}
