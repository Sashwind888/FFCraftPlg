package sashwind.mc.plugin.ffcraft.service;

import org.bukkit.entity.Player;
import sashwind.mc.plugin.ffcraft.data.VideoPlayerSavedData;
import sashwind.mc.plugin.ffcraft.model.*;

import java.util.*;

/**
 * Core service managing all VideoPlayer business logic.
 */
public class VideoPlayerService {

    private final VideoPlayerSavedData savedData;

    public VideoPlayerService(VideoPlayerSavedData savedData) {
        this.savedData = savedData;
    }

    // ==================== Accessors ====================

    public List<ServerVideoPlayer> players() {
        return savedData.players();
    }

    public VideoPlayerSnapshot snapshot() {
        List<VideoPlayerData> dataList = new ArrayList<>();
        for (ServerVideoPlayer svp : savedData.players()) {
            dataList.add(toVideoPlayerData(svp));
        }
        return new VideoPlayerSnapshot(Collections.unmodifiableList(dataList));
    }

    public Optional<ServerVideoPlayer> findPlayer(UUID playerId) {
        return savedData.players().stream()
            .filter(p -> p.id().equals(playerId))
            .findFirst();
    }

    private int findPlayerIndex(UUID playerId) {
        List<ServerVideoPlayer> list = savedData.players();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(playerId)) {
                return i;
            }
        }
        return -1;
    }

    // ==================== Player Management ====================

    public ServerVideoPlayer createPlayer(Player actor, CreatePlayerRequest request) {
        if (!Permissions.canAdmin(actor)) {
            throw new SecurityException("You do not have permission to create video players.");
        }

        ServerVideoPlayer player = ServerVideoPlayer.create(request.name(), request.isPublic());
        savedData.players().add(player);
        savedData.setDirty();
        return player;
    }

    public void deletePlayer(Player actor, UUID playerId) {
        if (!Permissions.canAdmin(actor)) {
            throw new SecurityException("You do not have permission to delete video players.");
        }

        int index = findPlayerIndex(playerId);
        if (index < 0) {
            throw new NoSuchElementException("Video player not found: " + playerId);
        }

        savedData.players().remove(index);
        savedData.setDirty();
    }

    public ServerVideoPlayer renamePlayer(Player actor, UUID playerId, String newName) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));

        if (!Permissions.canEdit(actor, player)) {
            throw new SecurityException("You do not have permission to edit this video player.");
        }

        int index = findPlayerIndex(playerId);
        ServerVideoPlayer updated = player.withName(newName);
        savedData.players().set(index, updated);
        savedData.setDirty();
        return updated;
    }

    public ServerVideoPlayer setPublic(Player actor, UUID playerId, boolean isPublic) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));

        if (!Permissions.canAdmin(actor)) {
            throw new SecurityException("You do not have permission to change this player's public status.");
        }

        int index = findPlayerIndex(playerId);
        ServerVideoPlayer updated = player.withPublic(isPublic);
        savedData.players().set(index, updated);
        savedData.setDirty();
        return updated;
    }

    // ==================== Screen Management ====================

    public ServerVideoScreen createScreen(Player actor, CreateScreenRequest request) {
        ServerVideoPlayer player = findPlayer(request.playerId())
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + request.playerId()));

        if (!Permissions.canEdit(actor, player)) {
            throw new SecurityException("You do not have permission to add screens to this video player.");
        }

        if (request.vertices().size() < 3 || request.vertices().size() > 64) {
            throw new IllegalArgumentException("Screen must have between 3 and 64 vertices, got " + request.vertices().size());
        }

        ServerVideoScreen screen = new ServerVideoScreen(
            UUID.randomUUID(),
            request.playerId(),
            request.name(),
            request.dimension(),
            request.vertices(),
            UvTransform.createDefault(),
            ScreenChannelState.createDefault()
        );

        List<ServerVideoScreen> newScreens = new ArrayList<>(player.screens());
        newScreens.add(screen);

        int index = findPlayerIndex(request.playerId());
        savedData.players().set(index, player.withScreens(newScreens));
        savedData.setDirty();
        return screen;
    }

    public void deleteScreen(Player actor, UUID playerId, UUID screenId) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));

        if (!Permissions.canEdit(actor, player)) {
            throw new SecurityException("You do not have permission to remove screens from this video player.");
        }

        List<ServerVideoScreen> newScreens = new ArrayList<>(player.screens());
        boolean removed = newScreens.removeIf(s -> s.id().equals(screenId));
        if (!removed) {
            throw new NoSuchElementException("Screen not found: " + screenId);
        }

        int index = findPlayerIndex(playerId);
        savedData.players().set(index, player.withScreens(newScreens));
        savedData.setDirty();
    }

    public ServerVideoScreen renameScreen(Player actor, UUID playerId, UUID screenId, String newName) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));

        if (!Permissions.canEdit(actor, player)) {
            throw new SecurityException("You do not have permission to edit screens on this video player.");
        }

        List<ServerVideoScreen> screens = player.screens();
        int screenIdx = -1;
        for (int i = 0; i < screens.size(); i++) {
            if (screens.get(i).id().equals(screenId)) {
                screenIdx = i;
                break;
            }
        }
        if (screenIdx < 0) {
            throw new NoSuchElementException("Screen not found: " + screenId);
        }

        List<ServerVideoScreen> newScreens = new ArrayList<>(screens);
        newScreens.set(screenIdx, screens.get(screenIdx).withName(newName));

        int playerIdx = findPlayerIndex(playerId);
        savedData.players().set(playerIdx, player.withScreens(newScreens));
        savedData.setDirty();
        return newScreens.get(screenIdx);
    }

    public ServerVideoScreen updateScreenUv(Player actor, UUID playerId, UUID screenId, UvTransform uvTransform) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));

        if (!Permissions.canEdit(actor, player)) {
            throw new SecurityException("You do not have permission to edit screens on this video player.");
        }

        List<ServerVideoScreen> screens = player.screens();
        int screenIdx = -1;
        for (int i = 0; i < screens.size(); i++) {
            if (screens.get(i).id().equals(screenId)) {
                screenIdx = i;
                break;
            }
        }
        if (screenIdx < 0) {
            throw new NoSuchElementException("Screen not found: " + screenId);
        }

        List<ServerVideoScreen> newScreens = new ArrayList<>(screens);
        newScreens.set(screenIdx, screens.get(screenIdx).withUvTransform(uvTransform));

        int playerIdx = findPlayerIndex(playerId);
        savedData.players().set(playerIdx, player.withScreens(newScreens));
        savedData.setDirty();
        return newScreens.get(screenIdx);
    }

    public ServerVideoScreen updateScreenChannel(Player actor, UUID playerId, UUID screenId, ScreenChannelState channelState) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));

        if (!Permissions.canEdit(actor, player)) {
            throw new SecurityException("You do not have permission to edit screens on this video player.");
        }

        List<ServerVideoScreen> screens = player.screens();
        int screenIdx = -1;
        for (int i = 0; i < screens.size(); i++) {
            if (screens.get(i).id().equals(screenId)) {
                screenIdx = i;
                break;
            }
        }
        if (screenIdx < 0) {
            throw new NoSuchElementException("Screen not found: " + screenId);
        }

        List<ServerVideoScreen> newScreens = new ArrayList<>(screens);
        newScreens.set(screenIdx, screens.get(screenIdx).withChannelState(channelState));

        int playerIdx = findPlayerIndex(playerId);
        savedData.players().set(playerIdx, player.withScreens(newScreens));
        savedData.setDirty();
        return newScreens.get(screenIdx);
    }

    // ==================== Playlist Management ====================

    public ServerVideoPlayer addVideo(Player actor, UUID playerId, VideoSource videoSource) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));

        if (!Permissions.canEdit(actor, player)) {
            throw new SecurityException("You do not have permission to edit this video player's playlist.");
        }

        List<VideoSource> newPlaylist = new ArrayList<>(player.playlist());
        newPlaylist.add(videoSource);

        int index = findPlayerIndex(playerId);
        ServerVideoPlayer updated = player.withPlaylist(newPlaylist);
        savedData.players().set(index, updated);
        savedData.setDirty();

        // TODO: Trigger async video probe resolution (probeVideoResolution)

        return updated;
    }

    public ServerVideoPlayer removeVideo(Player actor, UUID playerId, int videoIndex) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));

        if (!Permissions.canEdit(actor, player)) {
            throw new SecurityException("You do not have permission to edit this video player's playlist.");
        }

        if (videoIndex < 0 || videoIndex >= player.playlist().size()) {
            throw new IndexOutOfBoundsException("Video index out of bounds: " + videoIndex);
        }

        List<VideoSource> newPlaylist = new ArrayList<>(player.playlist());
        newPlaylist.remove(videoIndex);

        int idx = findPlayerIndex(playerId);
        ServerVideoPlayer updated = player.withPlaylist(newPlaylist);
        savedData.players().set(idx, updated);
        savedData.setDirty();
        return updated;
    }

    public ServerVideoPlayer moveVideo(Player actor, UUID playerId, int fromIndex, int toIndex) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));

        if (!Permissions.canEdit(actor, player)) {
            throw new SecurityException("You do not have permission to edit this video player's playlist.");
        }

        List<VideoSource> playlist = player.playlist();
        if (fromIndex < 0 || fromIndex >= playlist.size() || toIndex < 0 || toIndex >= playlist.size()) {
            throw new IndexOutOfBoundsException("Index out of bounds: from=" + fromIndex + " to=" + toIndex);
        }

        List<VideoSource> newPlaylist = new ArrayList<>(playlist);
        VideoSource moved = newPlaylist.remove(fromIndex);
        newPlaylist.add(toIndex, moved);

        int idx = findPlayerIndex(playerId);
        ServerVideoPlayer updated = player.withPlaylist(newPlaylist);
        savedData.players().set(idx, updated);
        savedData.setDirty();
        return updated;
    }

    // ==================== Playback Control ====================

    public ServerVideoPlayer setPlaybackState(UUID playerId, PlaybackState newState) {
        int index = findPlayerIndex(playerId);
        if (index < 0) {
            throw new NoSuchElementException("Video player not found: " + playerId);
        }

        ServerVideoPlayer player = savedData.players().get(index);
        savedData.players().set(index, player.withPlaybackState(newState));
        savedData.setDirty();
        return savedData.players().get(index);
    }

    public ServerVideoPlayer seek(Player actor, UUID playerId, int seekSeconds) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));

        if (!Permissions.canEdit(actor, player)) {
            throw new SecurityException("You do not have permission to control this video player.");
        }

        PlaybackState current = player.playbackState();
        PlaybackState newState = new PlaybackState(
            current.status(),
            current.mode(),
            current.currentIndex(),
            seekSeconds,
            current.volume(),
            System.currentTimeMillis() / 1000
        );

        return setPlaybackState(playerId, newState);
    }

    public void stopAllPlayback() {
        for (int i = 0; i < savedData.players().size(); i++) {
            ServerVideoPlayer player = savedData.players().get(i);
            PlaybackState stopped = player.playbackState().withStatus(PlaybackStatus.STOPPED);
            savedData.players().set(i, player.withPlaybackState(stopped));
        }
        savedData.setDirty();
    }

    public void tickProgress() {
        boolean changed = false;
        for (int i = 0; i < savedData.players().size(); i++) {
            ServerVideoPlayer player = savedData.players().get(i);
            if (player.playbackState().status() == PlaybackStatus.PLAYING) {
                savedData.players().set(i, player.withPlaybackState(player.playbackState().tickProgress()));
                changed = true;
            }
        }
        if (changed) {
            savedData.setDirty();
        }
    }

    // ==================== Mapper ====================

    public static VideoPlayerData toVideoPlayerData(ServerVideoPlayer svp) {
        List<VideoScreenData> screenData = new ArrayList<>();
        for (ServerVideoScreen screen : svp.screens()) {
            screenData.add(new VideoScreenData(
                screen.id(),
                screen.playerId(),
                screen.name(),
                screen.dimension(),
                screen.vertices(),
                screen.uvTransform(),
                screen.channelState()
            ));
        }
        return new VideoPlayerData(
            svp.id(),
            svp.name(),
            svp.isPublic(),
            svp.editors(),
            svp.playlist(),
            svp.playbackState(),
            screenData
        );
    }
}
