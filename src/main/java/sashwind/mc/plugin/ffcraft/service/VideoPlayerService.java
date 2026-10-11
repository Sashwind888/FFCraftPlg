package sashwind.mc.plugin.ffcraft.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import sashwind.mc.plugin.ffcraft.data.VideoPlayerSavedData;
import sashwind.mc.plugin.ffcraft.event.*;
import sashwind.mc.plugin.ffcraft.lang.Messages;
import sashwind.mc.plugin.ffcraft.model.*;

import java.util.*;

public class VideoPlayerService {

    private final VideoPlayerSavedData savedData;
    private final Runnable saveCallback;

    public VideoPlayerService(VideoPlayerSavedData savedData, Runnable saveCallback) {
        this.savedData = savedData;
        this.saveCallback = saveCallback;
    }

    /** Mark dirty and schedule an async save. */
    private void markDirty() {
        savedData.setDirty();
        saveCallback.run();
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

    /** Snapshot filtered: private players hidden from unauthorized viewers. */
    public VideoPlayerSnapshot snapshotFor(Player viewer) {
        List<VideoPlayerData> list = new ArrayList<>();
        for (ServerVideoPlayer svp : savedData.players()) {
            if (Permissions.canView(viewer, svp)) {
                list.add(toVideoPlayerData(svp));
            }
        }
        return new VideoPlayerSnapshot(Collections.unmodifiableList(list));
    }

    public Optional<ServerVideoPlayer> findPlayer(UUID playerId) {
        return savedData.players().stream()
            .filter(p -> p.id().equals(playerId))
            .findFirst();
    }

    public Optional<ServerVideoPlayer> findPlayerByName(String name) {
        return savedData.players().stream()
            .filter(p -> p.name().equalsIgnoreCase(name))
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

    /** Creates a player with actor permission check. */
    public ServerVideoPlayer createPlayer(Player actor, CreatePlayerRequest request) {
        if (!Permissions.canCreate(actor, request.isPublic())) {
            throw new SecurityException(Messages.get(
                request.isPublic() ? "perm.create.public" : "perm.create.private", actor));
        }
        return createPlayerSystem(actor, request.name(), request.isPublic(), actor.getUniqueId());
    }

    /** Creates a player without permission check (for API, console, or automation). */
    public ServerVideoPlayer createPlayerSystem(Player actor, String name, boolean isPublic, UUID owner) {
        ServerVideoPlayer player = ServerVideoPlayer.create(name, isPublic, owner);
        VideoPlayerCreateEvent event = new VideoPlayerCreateEvent(player, actor, name, isPublic, owner);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            throw new IllegalStateException("VideoPlayer creation cancelled by event listener");
        }

        savedData.players().add(player);
        markDirty();
        return player;
    }

    /** Deletes a player with actor permission check. */
    public void deletePlayer(Player actor, UUID playerId) {
        if (!Permissions.canDelete(actor)) {
            throw new SecurityException(Messages.get("perm.delete", actor));
        }
        deletePlayerSystem(actor, playerId);
    }

    /** Deletes a player without permission check. */
    public void deletePlayerSystem(Player actor, UUID playerId) {
        int index = findPlayerIndex(playerId);
        if (index < 0) {
            throw new NoSuchElementException(actor != null ? Messages.get("err.player_not_found", actor, playerId) : "Video player not found: " + playerId);
        }

        ServerVideoPlayer player = savedData.players().get(index);
        VideoPlayerDeleteEvent event = new VideoPlayerDeleteEvent(player, actor);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            throw new IllegalStateException("VideoPlayer deletion cancelled by event listener");
        }

        savedData.players().remove(index);
        markDirty();
    }

    /** Renames a player with actor permission check. */
    public ServerVideoPlayer renamePlayer(Player actor, UUID playerId, String newName) {
        requireManage(actor, playerId);
        return renamePlayerSystem(actor, playerId, newName);
    }

    /** Renames a player without permission check. */
    public ServerVideoPlayer renamePlayerSystem(Player actor, UUID playerId, String newName) {
        int index = findPlayerIndex(playerId);
        if (index < 0) {
            throw new NoSuchElementException("Video player not found: " + playerId);
        }
        ServerVideoPlayer player = savedData.players().get(index);

        VideoPlayerRenameEvent event = new VideoPlayerRenameEvent(player, actor, player.name(), newName);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            throw new IllegalStateException("VideoPlayer rename cancelled by event listener");
        }

        ServerVideoPlayer updated = player.withName(event.getNewName());
        savedData.players().set(index, updated);
        markDirty();
        return updated;
    }

    /** Toggle public/private with actor permission check. */
    public ServerVideoPlayer setPublic(Player actor, UUID playerId, boolean isPublic) {
        if (!Permissions.canSetPublic(actor)) {
            throw new SecurityException(Messages.get("perm.setpublic", actor));
        }
        return setPublicSystem(actor, playerId, isPublic);
    }

    /** Toggle public/private without permission check. */
    public ServerVideoPlayer setPublicSystem(Player actor, UUID playerId, boolean isPublic) {
        int index = findPlayerIndex(playerId);
        if (index < 0) {
            throw new NoSuchElementException("Video player not found: " + playerId);
        }
        ServerVideoPlayer player = savedData.players().get(index);

        VideoPlayerVisibilityChangeEvent event = new VideoPlayerVisibilityChangeEvent(player, actor, player.isPublic(), isPublic);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            throw new IllegalStateException("VideoPlayer visibility change cancelled by event listener");
        }

        ServerVideoPlayer updated = player.withPublic(event.isNewPublic());
        savedData.players().set(index, updated);
        markDirty();
        return updated;
    }

    // ==================== Screen Management ====================

    public ServerVideoScreen createScreen(Player actor, CreateScreenRequest request) {
        requireManage(actor, request.playerId());
        return createScreenSystem(actor, request);
    }

    public ServerVideoScreen createScreenSystem(Player actor, CreateScreenRequest request) {
        ServerVideoPlayer player = findPlayer(request.playerId())
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + request.playerId()));

        int minVerts = (request.screenType() == 1) ? 1 : 3;
        if (request.vertices().size() < minVerts || request.vertices().size() > 64) {
            throw new IllegalArgumentException(actor != null
                ? Messages.get("err.screen_vertices", actor, request.vertices().size())
                : "Vertices count must be between " + minVerts + " and 64");
        }

        ServerVideoScreen screen = new ServerVideoScreen(
            UUID.randomUUID(),
            request.playerId(),
            request.name(),
            request.dimension(),
            request.vertices(),
            UvTransform.createDefault(request.screenType()),
            ScreenChannelState.createDefault(),
            request.screenType(),
            request.radius()
        );

        VideoScreenCreateEvent event = new VideoScreenCreateEvent(player, actor, screen);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            throw new IllegalStateException("VideoScreen creation cancelled by event listener");
        }

        List<ServerVideoScreen> newScreens = new ArrayList<>(player.screens());
        newScreens.add(screen);

        int index = findPlayerIndex(request.playerId());
        savedData.players().set(index, player.withScreens(newScreens));
        markDirty();
        return screen;
    }

    public void deleteScreen(Player actor, UUID playerId, UUID screenId) {
        requireManage(actor, playerId);
        deleteScreenSystem(actor, playerId, screenId);
    }

    public void deleteScreenSystem(Player actor, UUID playerId, UUID screenId) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));

        ServerVideoScreen targetScreen = player.screens().stream()
            .filter(s -> s.id().equals(screenId))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException(actor != null
                ? Messages.get("err.screen_not_found", actor, screenId)
                : "Screen not found: " + screenId));

        VideoScreenDeleteEvent event = new VideoScreenDeleteEvent(player, actor, targetScreen);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            throw new IllegalStateException("VideoScreen deletion cancelled by event listener");
        }

        List<ServerVideoScreen> newScreens = new ArrayList<>(player.screens());
        newScreens.removeIf(s -> s.id().equals(screenId));

        int index = findPlayerIndex(playerId);
        savedData.players().set(index, player.withScreens(newScreens));
        markDirty();
    }

    public ServerVideoScreen renameScreen(Player actor, UUID playerId, UUID screenId, String newName) {
        requireManage(actor, playerId);
        return renameScreenSystem(actor, playerId, screenId, newName);
    }

    public ServerVideoScreen renameScreenSystem(Player actor, UUID playerId, UUID screenId, String newName) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));

        int screenIdx = screenIndex(player, screenId);
        List<ServerVideoScreen> newScreens = new ArrayList<>(player.screens());
        newScreens.set(screenIdx, player.screens().get(screenIdx).withName(newName));

        int playerIdx = findPlayerIndex(playerId);
        savedData.players().set(playerIdx, player.withScreens(newScreens));
        markDirty();
        return newScreens.get(screenIdx);
    }

    public ServerVideoScreen updateScreenUv(Player actor, UUID playerId, UUID screenId, UvTransform uvTransform) {
        requireManage(actor, playerId);
        return updateScreenUvSystem(actor, playerId, screenId, uvTransform);
    }

    public ServerVideoScreen updateScreenUvSystem(Player actor, UUID playerId, UUID screenId, UvTransform uvTransform) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));

        int screenIdx = screenIndex(player, screenId);
        ServerVideoScreen screen = player.screens().get(screenIdx);

        VideoScreenTransformChangeEvent event = new VideoScreenTransformChangeEvent(
            player, actor, screen, uvTransform, null, null);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            throw new IllegalStateException("Screen UV update cancelled by event listener");
        }

        List<ServerVideoScreen> newScreens = new ArrayList<>(player.screens());
        newScreens.set(screenIdx, screen.withUvTransform(event.getNewUv()));

        int playerIdx = findPlayerIndex(playerId);
        savedData.players().set(playerIdx, player.withScreens(newScreens));
        markDirty();
        return newScreens.get(screenIdx);
    }

    public ServerVideoScreen updateScreenRadius(Player actor, UUID playerId, UUID screenId, double radius) {
        requireManage(actor, playerId);
        return updateScreenRadiusSystem(actor, playerId, screenId, radius);
    }

    public ServerVideoScreen updateScreenRadiusSystem(Player actor, UUID playerId, UUID screenId, double radius) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));

        int screenIdx = screenIndex(player, screenId);
        ServerVideoScreen screen = player.screens().get(screenIdx);

        VideoScreenTransformChangeEvent event = new VideoScreenTransformChangeEvent(
            player, actor, screen, null, radius, null);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            throw new IllegalStateException("Screen radius update cancelled by event listener");
        }

        List<ServerVideoScreen> newScreens = new ArrayList<>(player.screens());
        newScreens.set(screenIdx, screen.withRadius(event.getNewRadius()));

        int playerIdx = findPlayerIndex(playerId);
        savedData.players().set(playerIdx, player.withScreens(newScreens));
        markDirty();
        return newScreens.get(screenIdx);
    }

    public ServerVideoScreen updateScreenChannel(Player actor, UUID playerId, UUID screenId, ScreenChannelState channelState) {
        requireManage(actor, playerId);
        return updateScreenChannelSystem(actor, playerId, screenId, channelState);
    }

    public ServerVideoScreen updateScreenChannelSystem(Player actor, UUID playerId, UUID screenId, ScreenChannelState channelState) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));

        int screenIdx = screenIndex(player, screenId);
        ServerVideoScreen screen = player.screens().get(screenIdx);

        VideoScreenTransformChangeEvent event = new VideoScreenTransformChangeEvent(
            player, actor, screen, null, null, channelState);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            throw new IllegalStateException("Screen channel update cancelled by event listener");
        }

        List<ServerVideoScreen> newScreens = new ArrayList<>(player.screens());
        newScreens.set(screenIdx, screen.withChannelState(event.getNewChannelState()));

        int playerIdx = findPlayerIndex(playerId);
        savedData.players().set(playerIdx, player.withScreens(newScreens));
        markDirty();
        return newScreens.get(screenIdx);
    }

    // ==================== Playlist Management ====================

    public ServerVideoPlayer addVideo(Player actor, UUID playerId, VideoSource videoSource) {
        requireManage(actor, playerId);
        return addVideoSystem(actor, playerId, videoSource);
    }

    public ServerVideoPlayer addVideoSystem(Player actor, UUID playerId, VideoSource videoSource) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));

        int targetIndex = player.playlist().size();
        VideoPlaylistAddEvent event = new VideoPlaylistAddEvent(player, actor, videoSource, targetIndex);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            throw new IllegalStateException("Video add cancelled by event listener");
        }

        List<VideoSource> newPlaylist = new ArrayList<>(player.playlist());
        newPlaylist.add(event.getVideoSource());

        int index = findPlayerIndex(playerId);
        ServerVideoPlayer updated = player.withPlaylist(newPlaylist);
        savedData.players().set(index, updated);
        markDirty();
        return updated;
    }

    public ServerVideoPlayer removeVideo(Player actor, UUID playerId, int videoIndex) {
        requireManage(actor, playerId);
        return removeVideoSystem(actor, playerId, videoIndex);
    }

    public ServerVideoPlayer removeVideoSystem(Player actor, UUID playerId, int videoIndex) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));

        if (videoIndex < 0 || videoIndex >= player.playlist().size()) {
            throw new IndexOutOfBoundsException(actor != null
                ? Messages.get("err.video_index", actor, videoIndex)
                : "Video index out of bounds: " + videoIndex);
        }

        VideoSource removedSource = player.playlist().get(videoIndex);
        VideoPlaylistRemoveEvent event = new VideoPlaylistRemoveEvent(player, actor, removedSource, videoIndex);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            throw new IllegalStateException("Video remove cancelled by event listener");
        }

        List<VideoSource> newPlaylist = new ArrayList<>(player.playlist());
        newPlaylist.remove(videoIndex);

        int idx = findPlayerIndex(playerId);
        ServerVideoPlayer updated = player.withPlaylist(newPlaylist);
        savedData.players().set(idx, updated);
        markDirty();
        return updated;
    }

    public ServerVideoPlayer moveVideo(Player actor, UUID playerId, int fromIndex, int toIndex) {
        requireManage(actor, playerId);
        return moveVideoSystem(actor, playerId, fromIndex, toIndex);
    }

    public ServerVideoPlayer moveVideoSystem(Player actor, UUID playerId, int fromIndex, int toIndex) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));

        List<VideoSource> playlist = player.playlist();
        if (fromIndex < 0 || fromIndex >= playlist.size() || toIndex < 0 || toIndex >= playlist.size()) {
            throw new IndexOutOfBoundsException(actor != null
                ? Messages.get("err.index_bounds", actor, fromIndex, toIndex)
                : "Index out of bounds: " + fromIndex + " -> " + toIndex);
        }

        VideoPlaylistReorderEvent event = new VideoPlaylistReorderEvent(player, actor, fromIndex, toIndex);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            throw new IllegalStateException("Video reorder cancelled by event listener");
        }

        List<VideoSource> newPlaylist = new ArrayList<>(playlist);
        VideoSource moved = newPlaylist.remove(fromIndex);
        newPlaylist.add(toIndex, moved);

        int idx = findPlayerIndex(playerId);
        ServerVideoPlayer updated = player.withPlaylist(newPlaylist);
        savedData.players().set(idx, updated);
        markDirty();
        return updated;
    }

    // ==================== Permissions Grant/Revoke ====================

    /** Grant ffcraft.control.<playerId> to a user. Actor must be admin or owner. */
    public ServerVideoPlayer grantControl(Player actor, UUID playerId, UUID targetUser) {
        ServerVideoPlayer player = requireManage(actor, playerId);
        if (player.controlUsers().contains(targetUser)) {
            throw new IllegalArgumentException(Messages.get("cmd.grant.already_has", actor));
        }
        if (player.creator().equals(targetUser)) {
            throw new IllegalArgumentException(Messages.get("cmd.grant.is_owner", actor));
        }
        int index = findPlayerIndex(playerId);
        ServerVideoPlayer updated = player.grantControl(targetUser);
        savedData.players().set(index, updated);
        markDirty();
        return updated;
    }

    /** Revoke ffcraft.control.<playerId> from a user. Actor must be admin or owner. */
    public ServerVideoPlayer revokeControl(Player actor, UUID playerId, UUID targetUser) {
        ServerVideoPlayer player = requireManage(actor, playerId);
        if (!player.controlUsers().contains(targetUser)) {
            throw new IllegalArgumentException(Messages.get("cmd.revoke.no_control", actor));
        }
        int index = findPlayerIndex(playerId);
        ServerVideoPlayer updated = player.revokeControl(targetUser);
        savedData.players().set(index, updated);
        markDirty();
        return updated;
    }

    // ==================== Playback Control ====================

    /** Set playback state with actor permission check. */
    public ServerVideoPlayer setPlaybackState(Player actor, UUID playerId, PlaybackState newState) {
        requireControl(actor, playerId);
        return setPlaybackStateSystem(actor, playerId, newState);
    }

    /** Set playback state without permission check (for API and internal calls). */
    public ServerVideoPlayer setPlaybackStateSystem(Player actor, UUID playerId, PlaybackState newState) {
        int index = findPlayerIndex(playerId);
        if (index < 0) {
            throw new NoSuchElementException("Video player not found: " + playerId);
        }
        ServerVideoPlayer player = savedData.players().get(index);
        PlaybackState oldState = player.playbackState();

        // 1. Check TrackChange event if currentIndex changed
        if (oldState.currentIndex() != newState.currentIndex()) {
            VideoPlayerTrackChangeEvent trackEvent = new VideoPlayerTrackChangeEvent(
                player, actor, oldState.currentIndex(), newState.currentIndex()
            );
            Bukkit.getPluginManager().callEvent(trackEvent);
            if (trackEvent.isCancelled()) {
                throw new IllegalStateException("Track change cancelled by event listener");
            }
        }

        // 2. Playback state change event
        VideoPlayerPlaybackStateChangeEvent stateEvent = new VideoPlayerPlaybackStateChangeEvent(
            player, actor, oldState, newState
        );
        Bukkit.getPluginManager().callEvent(stateEvent);
        if (stateEvent.isCancelled()) {
            throw new IllegalStateException("Playback state change cancelled by event listener");
        }

        PlaybackState finalState = stateEvent.getNewState();
        ServerVideoPlayer updated = player.withPlaybackState(finalState);
        savedData.players().set(index, updated);
        markDirty();
        return updated;
    }

    /** Seek with actor permission check. */
    public ServerVideoPlayer seek(Player actor, UUID playerId, int seekSeconds) {
        requireControl(actor, playerId);
        return seekSystem(actor, playerId, seekSeconds);
    }

    /** Seek without permission check. */
    public ServerVideoPlayer seekSystem(Player actor, UUID playerId, int seekSeconds) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));

        PlaybackState current = player.playbackState();
        PlaybackState newState = new PlaybackState(
            current.status(),
            current.mode(),
            current.currentIndex(),
            seekSeconds,
            current.volume(),
            System.currentTimeMillis() / 1000
        );

        return setPlaybackStateSystem(actor, playerId, newState);
    }

    /** Internal: set playback state without permission check (for tick/stop). */
    ServerVideoPlayer setPlaybackStateInternal(UUID playerId, PlaybackState newState) {
        int index = findPlayerIndex(playerId);
        if (index < 0) return null;
        ServerVideoPlayer player = savedData.players().get(index);
        savedData.players().set(index, player.withPlaybackState(newState));
        markDirty();
        return savedData.players().get(index);
    }

    public void stopAllPlayback() {
        for (int i = 0; i < savedData.players().size(); i++) {
            ServerVideoPlayer player = savedData.players().get(i);
            PlaybackState stopped = player.playbackState().withStatus(PlaybackStatus.STOPPED);
            savedData.players().set(i, player.withPlaybackState(stopped));
        }
        markDirty();
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
            markDirty();
        }
    }

    // ==================== Permission helpers ====================

    private ServerVideoPlayer requireManage(Player actor, UUID playerId) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException(Messages.get("err.player_not_found", actor, playerId)));
        if (!Permissions.canManage(actor, player)) {
            throw new SecurityException(Messages.get("perm.manage", actor));
        }
        return player;
    }

    private ServerVideoPlayer requireControl(Player actor, UUID playerId) {
        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException(Messages.get("err.player_not_found", actor, playerId)));
        if (!Permissions.canControl(actor, player)) {
            throw new SecurityException(Messages.get("perm.control", actor));
        }
        return player;
    }

    private int screenIndex(ServerVideoPlayer player, UUID screenId) {
        List<ServerVideoScreen> screens = player.screens();
        for (int i = 0; i < screens.size(); i++) {
            if (screens.get(i).id().equals(screenId)) return i;
        }
        throw new NoSuchElementException(Messages.get("err.screen_not_found", screenId));
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
                screen.channelState(),
                screen.screenType(),
                screen.radius()
            ));
        }
        return new VideoPlayerData(
            svp.id(),
            svp.name(),
            svp.isPublic(),
            Set.of(svp.creator()), // editors = [creator] for client compat
            svp.playlist(),
            svp.playbackState(),
            screenData
        );
    }
}
