package sashwind.mc.plugin.ffcraft.model;

import java.util.*;

public record ServerVideoPlayer(
    UUID id,
    String name,
    boolean isPublic,
    Set<UUID> editors,
    List<VideoSource> playlist,
    PlaybackState playbackState,
    List<ServerVideoScreen> screens
) {
    public ServerVideoPlayer withName(String newName) {
        return new ServerVideoPlayer(id, newName, isPublic, editors, playlist, playbackState, screens);
    }

    public ServerVideoPlayer withPublic(boolean isPublic) {
        return new ServerVideoPlayer(id, name, isPublic, editors, playlist, playbackState, screens);
    }

    public ServerVideoPlayer withPlaybackState(PlaybackState newState) {
        return new ServerVideoPlayer(id, name, isPublic, editors, playlist, newState, screens);
    }

    public ServerVideoPlayer withPlaylist(List<VideoSource> newPlaylist) {
        return new ServerVideoPlayer(id, name, isPublic, editors, newPlaylist, playbackState, screens);
    }

    public ServerVideoPlayer withScreens(List<ServerVideoScreen> newScreens) {
        return new ServerVideoPlayer(id, name, isPublic, editors, playlist, playbackState, newScreens);
    }

    public ServerVideoPlayer withEditors(Set<UUID> newEditors) {
        return new ServerVideoPlayer(id, name, isPublic, newEditors, playlist, playbackState, screens);
    }

    public ServerVideoPlayer addEditor(UUID editor) {
        Set<UUID> newEditors = new HashSet<>(editors);
        newEditors.add(editor);
        return withEditors(Collections.unmodifiableSet(newEditors));
    }

    public ServerVideoPlayer removeEditor(UUID editor) {
        Set<UUID> newEditors = new HashSet<>(editors);
        newEditors.remove(editor);
        return withEditors(Collections.unmodifiableSet(newEditors));
    }

    public void validate() {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(playbackState, "playbackState must not be null");
    }

    public static ServerVideoPlayer create(String name, boolean isPublic) {
        return new ServerVideoPlayer(
            UUID.randomUUID(),
            name,
            isPublic,
            Collections.emptySet(),
            new ArrayList<>(),
            PlaybackState.createDefault(),
            new ArrayList<>()
        );
    }
}
