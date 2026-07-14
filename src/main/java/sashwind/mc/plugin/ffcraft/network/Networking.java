package sashwind.mc.plugin.ffcraft.network;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;
import sashwind.mc.plugin.ffcraft.data.CodecHelper;
import sashwind.mc.plugin.ffcraft.model.*;
import sashwind.mc.plugin.ffcraft.service.VideoPlayerService;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Plugin messaging channel communication with the FFCraft client mod.
 * Protocol: JSON over "ffcraft:main" channel. Field names MUST stay compatible
 * with the existing client mod — do not rename or remove fields it expects.
 */
public class Networking implements PluginMessageListener {

    public static final String CHANNEL = "ffcraft:main";

    private final JavaPlugin plugin;
    private final VideoPlayerService service;

    public Networking(JavaPlugin plugin, VideoPlayerService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void register() {
        Messenger messenger = plugin.getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(plugin, CHANNEL);
        messenger.registerIncomingPluginChannel(plugin, CHANNEL, this);
        plugin.getLogger().info("Registered plugin messaging channel: " + CHANNEL);
    }

    public void unregister() {
        Messenger messenger = plugin.getServer().getMessenger();
        messenger.unregisterIncomingPluginChannel(plugin, CHANNEL);
        messenger.unregisterOutgoingPluginChannel(plugin, CHANNEL);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) return;

        try {
            String json = new String(message, java.nio.charset.StandardCharsets.UTF_8);
            JsonObject packet = CodecHelper.parsePacket(json);
            String type = CodecHelper.getType(packet);
            JsonObject data = CodecHelper.getData(packet);

            handlePacket(player, type, data);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to handle plugin message from " + player.getName(), e);
            sendError(player, "Internal error: " + e.getMessage());
        }
    }

    private void handlePacket(Player player, String type, JsonObject data) {
        try {
            switch (type) {
                case CodecHelper.TYPE_REQUEST_PLAYERS -> handleRequestPlayers(player);
                case CodecHelper.TYPE_CREATE_PLAYER -> handleCreatePlayer(player, data);
                case CodecHelper.TYPE_CREATE_SCREEN -> handleCreateScreen(player, data);
                case CodecHelper.TYPE_DELETE_PLAYER -> handleDeletePlayer(player, data);
                case CodecHelper.TYPE_DELETE_SCREEN -> handleDeleteScreen(player, data);
                case CodecHelper.TYPE_RENAME_PLAYER -> handleRenamePlayer(player, data);
                case CodecHelper.TYPE_RENAME_SCREEN -> handleRenameScreen(player, data);
                case CodecHelper.TYPE_UPDATE_PLAYBACK -> handleUpdatePlayback(player, data);
                case CodecHelper.TYPE_SEEK -> handleSeek(player, data);
                case CodecHelper.TYPE_UPDATE_SCREEN_UV -> handleUpdateScreenUv(player, data);
                case CodecHelper.TYPE_UPDATE_SCREEN_CHANNEL -> handleUpdateScreenChannel(player, data);
                case CodecHelper.TYPE_ADD_VIDEO -> handleAddVideo(player, data);
                case CodecHelper.TYPE_REMOVE_VIDEO -> handleRemoveVideo(player, data);
                case CodecHelper.TYPE_MOVE_VIDEO -> handleMoveVideo(player, data);
                case CodecHelper.TYPE_GRANT_CONTROL -> handleGrantControl(player, data);
                case CodecHelper.TYPE_REVOKE_CONTROL -> handleRevokeControl(player, data);
                default -> plugin.getLogger().warning("Unknown packet type: " + type);
            }
        } catch (SecurityException e) {
            sendError(player, e.getMessage());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error handling packet type: " + type, e);
            sendError(player, "Error: " + e.getMessage());
        }
    }

    // ── Handlers ──────────────────────────────────────

    private void handleRequestPlayers(Player player) {
        syncTo(player);
    }

    private void handleCreatePlayer(Player player, JsonObject data) {
        String name = data.get("name").getAsString();
        boolean isPublic = data.has("isPublic") && data.get("isPublic").getAsBoolean();
        service.createPlayer(player, new CreatePlayerRequest(name, isPublic));
        syncAll();
    }

    private void handleCreateScreen(Player player, JsonObject data) {
        UUID playerId = UUID.fromString(data.get("playerId").getAsString());
        String name = data.get("name").getAsString();
        String dimension = data.has("dimension") ? data.get("dimension").getAsString() : player.getWorld().getName();

        List<ScreenVertex> vertices = new java.util.ArrayList<>();
        for (var e : data.getAsJsonArray("vertices")) {
            vertices.add(CodecHelper.decodeScreenVertex(e.getAsJsonObject()));
        }

        service.createScreen(player, new CreateScreenRequest(playerId, name, dimension, vertices));
        syncAll();
    }

    private void handleDeletePlayer(Player player, JsonObject data) {
        UUID playerId = UUID.fromString(data.get("playerId").getAsString());
        service.deletePlayer(player, playerId);
        syncAll();
    }

    private void handleDeleteScreen(Player player, JsonObject data) {
        UUID playerId = UUID.fromString(data.get("playerId").getAsString());
        UUID screenId = UUID.fromString(data.get("screenId").getAsString());
        service.deleteScreen(player, playerId, screenId);
        syncAll();
    }

    private void handleRenamePlayer(Player player, JsonObject data) {
        UUID playerId = UUID.fromString(data.get("playerId").getAsString());
        String newName = data.get("newName").getAsString();
        service.renamePlayer(player, playerId, newName);
        syncAll();
    }

    private void handleRenameScreen(Player player, JsonObject data) {
        UUID playerId = UUID.fromString(data.get("playerId").getAsString());
        UUID screenId = UUID.fromString(data.get("screenId").getAsString());
        String newName = data.get("newName").getAsString();
        service.renameScreen(player, playerId, screenId, newName);
        syncAll();
    }

    private void handleUpdatePlayback(Player player, JsonObject data) {
        UUID playerId = UUID.fromString(data.get("playerId").getAsString());
        PlaybackStatus status = PlaybackStatus.fromOrdinal(data.get("status").getAsInt());
        PlaybackMode mode = data.has("mode") ? PlaybackMode.fromOrdinal(data.get("mode").getAsInt()) : PlaybackMode.SEQUENTIAL;
        int index = data.has("currentIndex") ? data.get("currentIndex").getAsInt() : 0;
        int volume = data.has("volume") ? data.get("volume").getAsInt() : 80;

        ServerVideoPlayer svp = service.findPlayer(playerId).orElse(null);
        if (svp == null) return;

        // permission check is inside service.setPlaybackState (control level)
        int prog = svp.playbackState().progressSeconds();
        if (status == PlaybackStatus.STOPPED || index != svp.playbackState().currentIndex()) prog = 0;
        PlaybackState newState = new PlaybackState(status, mode, index, prog, volume, System.currentTimeMillis() / 1000);

        service.setPlaybackState(player, playerId, newState);
        syncAll();
    }

    private void handleSeek(Player player, JsonObject data) {
        UUID playerId = UUID.fromString(data.get("playerId").getAsString());
        int seekSeconds = data.get("seekSeconds").getAsInt();
        service.seek(player, playerId, seekSeconds);
        syncAll();
    }

    private void handleUpdateScreenUv(Player player, JsonObject data) {
        UUID playerId = UUID.fromString(data.get("playerId").getAsString());
        UUID screenId = UUID.fromString(data.get("screenId").getAsString());
        UvTransform uv = CodecHelper.decodeUvTransform(data.getAsJsonObject("uvTransform"));
        service.updateScreenUv(player, playerId, screenId, uv);
        syncAll();
    }

    private void handleUpdateScreenChannel(Player player, JsonObject data) {
        UUID playerId = UUID.fromString(data.get("playerId").getAsString());
        UUID screenId = UUID.fromString(data.get("screenId").getAsString());
        ScreenChannelState channel = CodecHelper.decodeScreenChannelState(data.getAsJsonObject("channelState"));
        service.updateScreenChannel(player, playerId, screenId, channel);
        syncAll();
    }

    private void handleAddVideo(Player player, JsonObject data) {
        UUID playerId = UUID.fromString(data.get("playerId").getAsString());
        String url = data.get("url").getAsString();
        int targetWidth = data.get("targetWidth").getAsInt();
        int targetHeight = data.get("targetHeight").getAsInt();
        Integer targetFps = data.has("targetFps") && !data.get("targetFps").isJsonNull()
            ? data.get("targetFps").getAsInt() : null;

        service.addVideo(player, playerId, new VideoSource(url, targetWidth, targetHeight, targetFps));
        syncAll();
    }

    private void handleRemoveVideo(Player player, JsonObject data) {
        UUID playerId = UUID.fromString(data.get("playerId").getAsString());
        int index = data.get("index").getAsInt();
        service.removeVideo(player, playerId, index);
        syncAll();
    }

    private void handleMoveVideo(Player player, JsonObject data) {
        UUID playerId = UUID.fromString(data.get("playerId").getAsString());
        int fromIndex = data.get("fromIndex").getAsInt();
        int toIndex = data.get("toIndex").getAsInt();
        service.moveVideo(player, playerId, fromIndex, toIndex);
        syncAll();
    }

    private void handleGrantControl(Player player, JsonObject data) {
        UUID playerId = UUID.fromString(data.get("playerId").getAsString());
        UUID target = UUID.fromString(data.get("target").getAsString());
        service.grantControl(player, playerId, target);
        syncAll();
    }

    private void handleRevokeControl(Player player, JsonObject data) {
        UUID playerId = UUID.fromString(data.get("playerId").getAsString());
        UUID target = UUID.fromString(data.get("target").getAsString());
        service.revokeControl(player, playerId, target);
        syncAll();
    }

    // ── Send (filtered per-viewer) ────────────────────

    /** Full sync (same data to everyone — all players are visible). */
    public void syncAll() {
        VideoPlayerSnapshot snapshot = service.snapshot();
        byte[] data = CodecHelper.buildSyncPlayersPacket(snapshot)
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.sendPluginMessage(plugin, CHANNEL, data);
        }
    }

    /** Full sync to a specific player. */
    public void syncTo(Player viewer) {
        byte[] data = CodecHelper.buildSyncPlayersPacket(service.snapshot())
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        viewer.sendPluginMessage(plugin, CHANNEL, data);
    }

    /** Progress update to all online players. */
    public void syncProgress(UUID playerId, PlaybackStatus status, int currentIndex, int progressSeconds) {
        byte[] data = CodecHelper.buildUpdateProgressPacket(playerId, status, currentIndex, progressSeconds)
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.sendPluginMessage(plugin, CHANNEL, data);
        }
    }

    private void sendError(Player player, String message) {
        byte[] data = CodecHelper.buildErrorPacket(message)
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        player.sendPluginMessage(plugin, CHANNEL, data);
    }
}
