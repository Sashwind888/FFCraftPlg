package sashwind.mc.plugin.ffcraft.api;

import org.bukkit.entity.Player;
import sashwind.mc.plugin.ffcraft.model.*;
import sashwind.mc.plugin.ffcraft.network.Networking;
import sashwind.mc.plugin.ffcraft.service.VideoPlayerService;

import java.util.*;

public class FFCraftAPIImpl implements FFCraftAPI {

    private final VideoPlayerService service;
    private final Networking networking;

    public FFCraftAPIImpl(VideoPlayerService service, Networking networking) {
        this.service = Objects.requireNonNull(service, "service must not be null");
        this.networking = Objects.requireNonNull(networking, "networking must not be null");
    }

    // ==================== Player Query & Management ====================

    @Override
    public List<ServerVideoPlayer> getPlayers() {
        return Collections.unmodifiableList(service.players());
    }

    @Override
    public Optional<ServerVideoPlayer> getPlayer(UUID playerId) {
        return service.findPlayer(playerId);
    }

    @Override
    public Optional<ServerVideoPlayer> getPlayerByName(String name) {
        return service.findPlayerByName(name);
    }

    @Override
    public ServerVideoPlayer createPlayer(String name, boolean isPublic, UUID owner) {
        UUID finalOwner = owner != null ? owner : new UUID(0L, 0L);
        ServerVideoPlayer created = service.createPlayerSystem(null, name, isPublic, finalOwner);
        networking.syncAll();
        return created;
    }

    @Override
    public void deletePlayer(UUID playerId) {
        service.deletePlayerSystem(null, playerId);
        networking.syncAll();
    }

    @Override
    public ServerVideoPlayer renamePlayer(UUID playerId, String newName) {
        ServerVideoPlayer updated = service.renamePlayerSystem(null, playerId, newName);
        networking.syncAll();
        return updated;
    }

    @Override
    public ServerVideoPlayer setPublic(UUID playerId, boolean isPublic) {
        ServerVideoPlayer updated = service.setPublicSystem(null, playerId, isPublic);
        networking.syncAll();
        return updated;
    }

    // ==================== Playback Control ====================

    @Override
    public void play(UUID playerId) {
        ServerVideoPlayer player = getPlayerOrThrow(playerId);
        PlaybackState current = player.playbackState();
        PlaybackState updated = current.withStatus(PlaybackStatus.PLAYING);
        service.setPlaybackStateSystem(null, playerId, updated);
        networking.syncAll();
    }

    @Override
    public void pause(UUID playerId) {
        ServerVideoPlayer player = getPlayerOrThrow(playerId);
        PlaybackState current = player.playbackState();
        PlaybackState updated = current.withStatus(PlaybackStatus.PAUSED);
        service.setPlaybackStateSystem(null, playerId, updated);
        networking.syncAll();
    }

    @Override
    public void stop(UUID playerId) {
        ServerVideoPlayer player = getPlayerOrThrow(playerId);
        PlaybackState current = player.playbackState();
        PlaybackState updated = current.withStatus(PlaybackStatus.STOPPED).withProgress(0);
        service.setPlaybackStateSystem(null, playerId, updated);
        networking.syncAll();
    }

    @Override
    public void seek(UUID playerId, int seconds) {
        service.seekSystem(null, playerId, seconds);
        ServerVideoPlayer player = getPlayerOrThrow(playerId);
        networking.syncProgress(playerId, player.playbackState().status(), player.playbackState().currentIndex(), seconds);
    }

    @Override
    public void setVolume(UUID playerId, int volume) {
        ServerVideoPlayer player = getPlayerOrThrow(playerId);
        int clamped = Math.max(0, Math.min(100, volume));
        PlaybackState updated = player.playbackState().withVolume(clamped);
        service.setPlaybackStateSystem(null, playerId, updated);
        networking.syncAll();
    }

    @Override
    public void playIndex(UUID playerId, int index) {
        ServerVideoPlayer player = getPlayerOrThrow(playerId);
        if (index < 0 || index >= player.playlist().size()) {
            throw new IndexOutOfBoundsException("Invalid video playlist index: " + index);
        }
        PlaybackState current = player.playbackState();
        PlaybackState updated = new PlaybackState(
            PlaybackStatus.PLAYING,
            current.mode(),
            index,
            0,
            current.volume(),
            System.currentTimeMillis() / 1000
        );
        service.setPlaybackStateSystem(null, playerId, updated);
        networking.syncAll();
    }

    @Override
    public void next(UUID playerId) {
        ServerVideoPlayer player = getPlayerOrThrow(playerId);
        int size = player.playlist().size();
        if (size == 0) return;

        int currentIdx = player.playbackState().currentIndex();
        int nextIdx = (currentIdx + 1) % size;
        playIndex(playerId, nextIdx);
    }

    @Override
    public void previous(UUID playerId) {
        ServerVideoPlayer player = getPlayerOrThrow(playerId);
        int size = player.playlist().size();
        if (size == 0) return;

        int currentIdx = player.playbackState().currentIndex();
        int prevIdx = (currentIdx - 1 + size) % size;
        playIndex(playerId, prevIdx);
    }

    @Override
    public void setPlaybackMode(UUID playerId, PlaybackMode mode) {
        ServerVideoPlayer player = getPlayerOrThrow(playerId);
        PlaybackState updated = player.playbackState().withMode(mode);
        service.setPlaybackStateSystem(null, playerId, updated);
        networking.syncAll();
    }

    @Override
    public void setPlaybackState(UUID playerId, PlaybackState state) {
        service.setPlaybackStateSystem(null, playerId, state);
        networking.syncAll();
    }

    // ==================== Playlist Management ====================

    @Override
    public void addVideo(UUID playerId, VideoSource video) {
        service.addVideoSystem(null, playerId, video);
        networking.syncAll();
    }

    @Override
    public void removeVideo(UUID playerId, int index) {
        service.removeVideoSystem(null, playerId, index);
        networking.syncAll();
    }

    @Override
    public void moveVideo(UUID playerId, int fromIndex, int toIndex) {
        service.moveVideoSystem(null, playerId, fromIndex, toIndex);
        networking.syncAll();
    }

    @Override
    public void clearPlaylist(UUID playerId) {
        ServerVideoPlayer player = getPlayerOrThrow(playerId);
        while (!player.playlist().isEmpty()) {
            player = service.removeVideoSystem(null, playerId, 0);
        }
        networking.syncAll();
    }

    // ==================== Screen Management ====================

    @Override
    public ServerVideoScreen createScreen(CreateScreenRequest request) {
        ServerVideoScreen screen = service.createScreenSystem(null, request);
        networking.syncAll();
        return screen;
    }

    @Override
    public void deleteScreen(UUID playerId, UUID screenId) {
        service.deleteScreenSystem(null, playerId, screenId);
        networking.syncAll();
    }

    @Override
    public ServerVideoScreen renameScreen(UUID playerId, UUID screenId, String newName) {
        ServerVideoScreen screen = service.renameScreenSystem(null, playerId, screenId, newName);
        networking.syncAll();
        return screen;
    }

    @Override
    public ServerVideoScreen updateScreenUv(UUID playerId, UUID screenId, UvTransform uvTransform) {
        ServerVideoScreen screen = service.updateScreenUvSystem(null, playerId, screenId, uvTransform);
        networking.syncAll();
        return screen;
    }

    @Override
    public ServerVideoScreen updateScreenRadius(UUID playerId, UUID screenId, double radius) {
        ServerVideoScreen screen = service.updateScreenRadiusSystem(null, playerId, screenId, radius);
        networking.syncAll();
        return screen;
    }

    @Override
    public ServerVideoScreen updateScreenChannel(UUID playerId, UUID screenId, ScreenChannelState channelState) {
        ServerVideoScreen screen = service.updateScreenChannelSystem(null, playerId, screenId, channelState);
        networking.syncAll();
        return screen;
    }

    // ==================== Mesh & Topology Calculation ====================

    @Override
    public sashwind.mc.plugin.ffcraft.model.mesh.ComputedScreenMesh computeScreenMesh(ServerVideoScreen screen) {
        return sashwind.mc.plugin.ffcraft.mesh.ScreenMeshCalculator.compute(screen);
    }

    @Override
    public sashwind.mc.plugin.ffcraft.model.mesh.ComputedScreenMesh computeScreenMesh(UUID playerId, UUID screenId) {
        ServerVideoPlayer player = getPlayerOrThrow(playerId);
        ServerVideoScreen screen = player.screens().stream()
            .filter(s -> s.id().equals(screenId))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Screen not found: " + screenId));
        return computeScreenMesh(screen);
    }

    // ==================== Client Sync ====================

    @Override
    public void syncAll() {
        networking.syncAll();
    }

    @Override
    public void syncTo(Player player) {
        networking.syncTo(player);
    }

    private ServerVideoPlayer getPlayerOrThrow(UUID playerId) {
        return service.findPlayer(playerId)
            .orElseThrow(() -> new NoSuchElementException("Video player not found: " + playerId));
    }
}
