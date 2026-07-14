package sashwind.mc.plugin.ffcraft.service;

import org.bukkit.entity.Player;
import sashwind.mc.plugin.ffcraft.data.VideoPlayerSavedData;
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

    /** Creates a player. The actor becomes the owner. Requires admin. */
    public ServerVideoPlayer createPlayer(Player actor, CreatePlayerRequest request) {
        if (!Permissions.canAdmin(actor)) {
            throw new SecurityException(Messages.get("perm.create", actor));
        }

        ServerVideoPlayer player = ServerVideoPlayer.create(request.name(), request.isPublic(), actor.getUniqueId());
        savedData.players().add(player);
        markDirty();
        return player;
    }

    /** Deletes a player. Admin only. */
    public void deletePlayer(Player actor, UUID playerId) {
        if (!Permissions.canAdmin(actor)) {
            throw new SecurityException(Messages.get("perm.delete", actor));
        }

        int index = findPlayerIndex(playerId);
        if (index < 0) {
            throw new NoSuchElementException(Messages.get("err.player_not_found", actor, playerId));
        }

        savedData.players().remove(index);
        markDirty();
    }

    /** Renames a player. Requires manage permission (admin or owner). */
    public ServerVideoPlayer renamePlayer(Player actor, UUID playerId, String newName) {
        ServerVideoPlayer player = requireManage(actor, playerId);

        int index = findPlayerIndex(playerId);
        ServerVideoPlayer updated = player.withName(newName);
        savedData.players().set(index, updated);
        markDirty();
        return updated;
    }

    /** Toggle public/private. Admin only. */
    public ServerVideoPlayer setPublic(Player actor, UUID playerId, boolean isPublic) {
        if (!Permissions.canAdmin(actor)) {
            throw new SecurityException(Messages.get("perm.setpublic", actor));
        }

        ServerVideoPlayer player = findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException(Messages.get("err.player_not_found", actor, playerId)));

        int index = findPlayerIndex(playerId);
        ServerVideoPlayer updated = player.withPublic(isPublic);
        savedData.players().set(index, updated);
        markDirty();
        return updated;
    }

    // ==================== Screen Management ====================

    public ServerVideoScreen createScreen(Player actor, CreateScreenRequest request) {
        ServerVideoPlayer player = requireManage(actor, request.playerId());

        if (request.vertices().size() < 3 || request.vertices().size() > 64) {
            throw new IllegalArgumentException(Messages.get("err.screen_vertices", actor, request.vertices().size()));
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
        markDirty();
        return screen;
    }

    public void deleteScreen(Player actor, UUID playerId, UUID screenId) {
        ServerVideoPlayer player = requireManage(actor, playerId);

        List<ServerVideoScreen> newScreens = new ArrayList<>(player.screens());
        boolean removed = newScreens.removeIf(s -> s.id().equals(screenId));
        if (!removed) {
            throw new NoSuchElementException(Messages.get("err.screen_not_found", actor, screenId));
        }

        int index = findPlayerIndex(playerId);
        savedData.players().set(index, player.withScreens(newScreens));
        markDirty();
    }

    public ServerVideoScreen renameScreen(Player actor, UUID playerId, UUID screenId, String newName) {
        ServerVideoPlayer player = requireManage(actor, playerId);

        int screenIdx = screenIndex(player, screenId);
        List<ServerVideoScreen> newScreens = new ArrayList<>(player.screens());
        newScreens.set(screenIdx, player.screens().get(screenIdx).withName(newName));

        int playerIdx = findPlayerIndex(playerId);
        savedData.players().set(playerIdx, player.withScreens(newScreens));
        markDirty();
        return newScreens.get(screenIdx);
    }

    public ServerVideoScreen updateScreenUv(Player actor, UUID playerId, UUID screenId, UvTransform uvTransform) {
        ServerVideoPlayer player = requireManage(actor, playerId);

        int screenIdx = screenIndex(player, screenId);
        List<ServerVideoScreen> newScreens = new ArrayList<>(player.screens());
        newScreens.set(screenIdx, player.screens().get(screenIdx).withUvTransform(uvTransform));

        int playerIdx = findPlayerIndex(playerId);
        savedData.players().set(playerIdx, player.withScreens(newScreens));
        markDirty();
        return newScreens.get(screenIdx);
    }

    public ServerVideoScreen updateScreenChannel(Player actor, UUID playerId, UUID screenId, ScreenChannelState channelState) {
        ServerVideoPlayer player = requireManage(actor, playerId);

        int screenIdx = screenIndex(player, screenId);
        List<ServerVideoScreen> newScreens = new ArrayList<>(player.screens());
        newScreens.set(screenIdx, player.screens().get(screenIdx).withChannelState(channelState));

        int playerIdx = findPlayerIndex(playerId);
        savedData.players().set(playerIdx, player.withScreens(newScreens));
        markDirty();
        return newScreens.get(screenIdx);
    }

    // ==================== Playlist Management ====================

    public ServerVideoPlayer addVideo(Player actor, UUID playerId, VideoSource videoSource) {
        ServerVideoPlayer player = requireManage(actor, playerId);

        List<VideoSource> newPlaylist = new ArrayList<>(player.playlist());
        newPlaylist.add(videoSource);

        int index = findPlayerIndex(playerId);
        ServerVideoPlayer updated = player.withPlaylist(newPlaylist);
        savedData.players().set(index, updated);
        markDirty();
        return updated;
    }

    public ServerVideoPlayer removeVideo(Player actor, UUID playerId, int videoIndex) {
        ServerVideoPlayer player = requireManage(actor, playerId);

        if (videoIndex < 0 || videoIndex >= player.playlist().size()) {
            throw new IndexOutOfBoundsException(Messages.get("err.video_index", actor, videoIndex));
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
        ServerVideoPlayer player = requireManage(actor, playerId);

        List<VideoSource> playlist = player.playlist();
        if (fromIndex < 0 || fromIndex >= playlist.size() || toIndex < 0 || toIndex >= playlist.size()) {
            throw new IndexOutOfBoundsException(Messages.get("err.index_bounds", actor, fromIndex, toIndex));
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
        // Cannot grant to owner (editors, first = owner)
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

    /** Set playback state. Requires control permission (admin/owner/controlUser). */
    public ServerVideoPlayer setPlaybackState(Player actor, UUID playerId, PlaybackState newState) {
        ServerVideoPlayer player = requireControl(actor, playerId);

        int index = findPlayerIndex(playerId);
        savedData.players().set(index, player.withPlaybackState(newState));
        markDirty();
        return savedData.players().get(index);
    }

    /** Seek. Requires control permission. */
    public ServerVideoPlayer seek(Player actor, UUID playerId, int seekSeconds) {
        ServerVideoPlayer player = requireControl(actor, playerId);

        PlaybackState current = player.playbackState();
        PlaybackState newState = new PlaybackState(
            current.status(),
            current.mode(),
            current.currentIndex(),
            seekSeconds,
            current.volume(),
            System.currentTimeMillis() / 1000
        );

        return setPlaybackState(actor, playerId, newState);
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

    private ServerVideoPlayer requireAdmin(Player actor, UUID playerId) {
        if (!Permissions.canAdmin(actor)) {
            throw new SecurityException(Messages.get("perm.create", actor));
        }
        return findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException(Messages.get("err.player_not_found", actor, playerId)));
    }

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
                screen.channelState()
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
