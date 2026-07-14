package sashwind.mc.plugin.ffcraft.data;

import com.google.gson.*;
import sashwind.mc.plugin.ffcraft.model.*;

import java.util.*;

/**
 * Handles JSON serialization/deserialization for all VideoPlayer data models.
 */
public final class CodecHelper {

    private static final Gson GSON;

    static {
        GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
    }

    private CodecHelper() {}

    // ==================== PlaybackState ====================

    public static JsonObject encodePlaybackState(PlaybackState state) {
        JsonObject obj = new JsonObject();
        obj.addProperty("status", state.status().ordinal());
        obj.addProperty("mode", state.mode().ordinal());
        obj.addProperty("currentIndex", state.currentIndex());
        obj.addProperty("progressSeconds", state.progressSeconds());
        obj.addProperty("volume", state.volume());
        obj.addProperty("lastUpdatedEpochSeconds", state.lastUpdatedEpochSeconds());
        return obj;
    }

    public static PlaybackState decodePlaybackState(JsonObject obj) {
        return new PlaybackState(
            PlaybackStatus.fromOrdinal(obj.get("status").getAsInt()),
            PlaybackMode.fromOrdinal(obj.get("mode").getAsInt()),
            obj.get("currentIndex").getAsInt(),
            obj.get("progressSeconds").getAsInt(),
            obj.get("volume").getAsInt(),
            obj.get("lastUpdatedEpochSeconds").getAsLong()
        );
    }

    // ==================== VideoSource ====================

    public static JsonObject encodeVideoSource(VideoSource source) {
        JsonObject obj = new JsonObject();
        obj.addProperty("url", source.url());
        obj.addProperty("targetWidth", source.targetWidth());
        obj.addProperty("targetHeight", source.targetHeight());
        if (source.targetFps() != null) {
            obj.addProperty("targetFps", source.targetFps());
        }
        obj.addProperty("originalWidth", source.originalWidth());
        obj.addProperty("originalHeight", source.originalHeight());
        return obj;
    }

    public static VideoSource decodeVideoSource(JsonObject obj) {
        Integer fps = obj.has("targetFps") && !obj.get("targetFps").isJsonNull()
            ? obj.get("targetFps").getAsInt() : null;
        return new VideoSource(
            obj.get("url").getAsString(),
            obj.get("targetWidth").getAsInt(),
            obj.get("targetHeight").getAsInt(),
            fps,
            obj.get("originalWidth").getAsInt(),
            obj.get("originalHeight").getAsInt()
        );
    }

    // ==================== ScreenVertex ====================

    public static JsonObject encodeScreenVertex(ScreenVertex vertex) {
        JsonObject obj = new JsonObject();
        obj.addProperty("x", vertex.x());
        obj.addProperty("y", vertex.y());
        obj.addProperty("z", vertex.z());
        obj.addProperty("pitch", vertex.pitch());
        obj.addProperty("yaw", vertex.yaw());
        return obj;
    }

    public static ScreenVertex decodeScreenVertex(JsonObject obj) {
        return new ScreenVertex(
            obj.get("x").getAsDouble(),
            obj.get("y").getAsDouble(),
            obj.get("z").getAsDouble(),
            obj.get("pitch").getAsDouble(),
            obj.get("yaw").getAsDouble()
        );
    }

    // ==================== UvTransform ====================

    public static JsonObject encodeUvTransform(UvTransform uv) {
        JsonObject obj = new JsonObject();
        obj.addProperty("offsetU", uv.offsetU());
        obj.addProperty("offsetV", uv.offsetV());
        obj.addProperty("scaleU", uv.scaleU());
        obj.addProperty("scaleV", uv.scaleV());
        obj.addProperty("rotationDegrees", uv.rotationDegrees());
        obj.addProperty("flipU", uv.flipU());
        obj.addProperty("flipV", uv.flipV());
        return obj;
    }

    public static UvTransform decodeUvTransform(JsonObject obj) {
        return new UvTransform(
            obj.get("offsetU").getAsDouble(),
            obj.get("offsetV").getAsDouble(),
            obj.get("scaleU").getAsDouble(),
            obj.get("scaleV").getAsDouble(),
            obj.get("rotationDegrees").getAsDouble(),
            obj.get("flipU").getAsBoolean(),
            obj.get("flipV").getAsBoolean()
        );
    }

    // ==================== ScreenChannelState ====================

    public static JsonObject encodeScreenChannelState(ScreenChannelState state) {
        JsonObject obj = new JsonObject();
        obj.addProperty("leftEnabled", state.leftEnabled());
        obj.addProperty("rightEnabled", state.rightEnabled());
        return obj;
    }

    public static ScreenChannelState decodeScreenChannelState(JsonObject obj) {
        return new ScreenChannelState(
            obj.get("leftEnabled").getAsBoolean(),
            obj.get("rightEnabled").getAsBoolean()
        );
    }

    // ==================== ServerVideoScreen ====================

    public static JsonObject encodeServerVideoScreen(ServerVideoScreen screen) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", screen.id().toString());
        obj.addProperty("playerId", screen.playerId().toString());
        obj.addProperty("name", screen.name());
        obj.addProperty("dimension", screen.dimension());

        JsonArray verticesArr = new JsonArray();
        for (ScreenVertex v : screen.vertices()) {
            verticesArr.add(encodeScreenVertex(v));
        }
        obj.add("vertices", verticesArr);
        obj.add("uvTransform", encodeUvTransform(screen.uvTransform()));
        obj.add("channelState", encodeScreenChannelState(screen.channelState()));
        return obj;
    }

    public static ServerVideoScreen decodeServerVideoScreen(JsonObject obj) {
        JsonArray verticesArr = obj.getAsJsonArray("vertices");
        List<ScreenVertex> vertices = new ArrayList<>();
        for (JsonElement e : verticesArr) {
            vertices.add(decodeScreenVertex(e.getAsJsonObject()));
        }
        return new ServerVideoScreen(
            UUID.fromString(obj.get("id").getAsString()),
            UUID.fromString(obj.get("playerId").getAsString()),
            obj.get("name").getAsString(),
            obj.get("dimension").getAsString(),
            vertices,
            decodeUvTransform(obj.getAsJsonObject("uvTransform")),
            decodeScreenChannelState(obj.getAsJsonObject("channelState"))
        );
    }

    // ==================== ServerVideoPlayer ====================

    public static JsonObject encodeServerVideoPlayer(ServerVideoPlayer player) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", player.id().toString());
        obj.addProperty("name", player.name());
        obj.addProperty("isPublic", player.isPublic());
        obj.addProperty("creator", player.creator().toString());

        // controlUsers — server-side only
        JsonArray controlArr = new JsonArray();
        for (UUID u : player.controlUsers()) {
            controlArr.add(u.toString());
        }
        obj.add("controlUsers", controlArr);

        JsonArray playlistArr = new JsonArray();
        for (VideoSource vs : player.playlist()) {
            playlistArr.add(encodeVideoSource(vs));
        }
        obj.add("playlist", playlistArr);

        obj.add("playbackState", encodePlaybackState(player.playbackState()));

        JsonArray screensArr = new JsonArray();
        for (ServerVideoScreen screen : player.screens()) {
            screensArr.add(encodeServerVideoScreen(screen));
        }
        obj.add("screens", screensArr);

        return obj;
    }

    public static ServerVideoPlayer decodeServerVideoPlayer(JsonObject obj) {
        // Backward compat: read "creator" or fall back to first element of "editors"
        UUID creator;
        if (obj.has("creator")) {
            creator = UUID.fromString(obj.get("creator").getAsString());
        } else if (obj.has("editors")) {
            JsonArray arr = obj.getAsJsonArray("editors");
            creator = arr.size() > 0 ? UUID.fromString(arr.get(0).getAsString()) : UUID.randomUUID();
        } else {
            creator = UUID.randomUUID(); // should not happen
        }

        Set<UUID> controlUsers = new HashSet<>();
        if (obj.has("controlUsers")) {
            for (JsonElement e : obj.getAsJsonArray("controlUsers")) {
                controlUsers.add(UUID.fromString(e.getAsString()));
            }
        }

        JsonArray playlistArr = obj.getAsJsonArray("playlist");
        List<VideoSource> playlist = new ArrayList<>();
        for (JsonElement e : playlistArr) {
            playlist.add(decodeVideoSource(e.getAsJsonObject()));
        }

        JsonArray screensArr = obj.getAsJsonArray("screens");
        List<ServerVideoScreen> screens = new ArrayList<>();
        for (JsonElement e : screensArr) {
            screens.add(decodeServerVideoScreen(e.getAsJsonObject()));
        }

        return new ServerVideoPlayer(
            UUID.fromString(obj.get("id").getAsString()),
            obj.get("name").getAsString(),
            obj.get("isPublic").getAsBoolean(),
            creator,
            Collections.unmodifiableSet(controlUsers),
            playlist,
            decodePlaybackState(obj.getAsJsonObject("playbackState")),
            screens
        );
    }

    // ==================== VideoPlayerData (client view) ====================

    public static JsonObject encodeVideoPlayerData(VideoPlayerData data) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", data.id().toString());
        obj.addProperty("name", data.name());
        obj.addProperty("isPublic", data.isPublic());

        // Keep "editors" in client protocol for backward compat — just contains creator
        JsonArray editorsArr = new JsonArray();
        for (UUID editor : data.editors()) {
            editorsArr.add(editor.toString());
        }
        obj.add("editors", editorsArr);

        JsonArray playlistArr = new JsonArray();
        for (VideoSource vs : data.playlist()) {
            playlistArr.add(encodeVideoSource(vs));
        }
        obj.add("playlist", playlistArr);

        obj.add("playbackState", encodePlaybackState(data.playbackState()));

        JsonArray screensArr = new JsonArray();
        for (VideoScreenData screen : data.screens()) {
            screensArr.add(encodeVideoScreenData(screen));
        }
        obj.add("screens", screensArr);

        return obj;
    }

    public static JsonObject encodeVideoScreenData(VideoScreenData screen) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", screen.id().toString());
        obj.addProperty("playerId", screen.playerId().toString());
        obj.addProperty("name", screen.name());
        obj.addProperty("dimension", screen.dimension());

        JsonArray verticesArr = new JsonArray();
        for (ScreenVertex v : screen.vertices()) {
            verticesArr.add(encodeScreenVertex(v));
        }
        obj.add("vertices", verticesArr);
        obj.add("uvTransform", encodeUvTransform(screen.uvTransform()));
        obj.add("channelState", encodeScreenChannelState(screen.channelState()));
        return obj;
    }

    // ==================== Snapshot ====================

    public static JsonObject encodeSnapshot(VideoPlayerSnapshot snapshot) {
        JsonArray playersArr = new JsonArray();
        for (VideoPlayerData data : snapshot.players()) {
            playersArr.add(encodeVideoPlayerData(data));
        }
        JsonObject obj = new JsonObject();
        obj.add("players", playersArr);
        return obj;
    }

    public static VideoPlayerData decodeVideoPlayerData(JsonObject obj) {
        JsonArray editorsArr = obj.getAsJsonArray("editors");
        Set<UUID> editors = new HashSet<>();
        for (JsonElement e : editorsArr) {
            editors.add(UUID.fromString(e.getAsString()));
        }

        JsonArray playlistArr = obj.getAsJsonArray("playlist");
        List<VideoSource> playlist = new ArrayList<>();
        for (JsonElement e : playlistArr) {
            playlist.add(decodeVideoSource(e.getAsJsonObject()));
        }

        JsonArray screensArr = obj.getAsJsonArray("screens");
        List<VideoScreenData> screens = new ArrayList<>();
        for (JsonElement e : screensArr) {
            screens.add(decodeVideoScreenData(e.getAsJsonObject()));
        }

        return new VideoPlayerData(
            UUID.fromString(obj.get("id").getAsString()),
            obj.get("name").getAsString(),
            obj.get("isPublic").getAsBoolean(),
            Collections.unmodifiableSet(editors),
            playlist,
            decodePlaybackState(obj.getAsJsonObject("playbackState")),
            screens
        );
    }

    public static VideoScreenData decodeVideoScreenData(JsonObject obj) {
        JsonArray verticesArr = obj.getAsJsonArray("vertices");
        List<ScreenVertex> vertices = new ArrayList<>();
        for (JsonElement e : verticesArr) {
            vertices.add(decodeScreenVertex(e.getAsJsonObject()));
        }
        return new VideoScreenData(
            UUID.fromString(obj.get("id").getAsString()),
            UUID.fromString(obj.get("playerId").getAsString()),
            obj.get("name").getAsString(),
            obj.get("dimension").getAsString(),
            vertices,
            decodeUvTransform(obj.getAsJsonObject("uvTransform")),
            decodeScreenChannelState(obj.getAsJsonObject("channelState"))
        );
    }

    // ==================== Packet Types (for network) ====================

    public static final String TYPE_REQUEST_PLAYERS = "request_players";
    public static final String TYPE_CREATE_PLAYER = "create_player";
    public static final String TYPE_CREATE_SCREEN = "create_screen";
    public static final String TYPE_DELETE_PLAYER = "delete_player";
    public static final String TYPE_DELETE_SCREEN = "delete_screen";
    public static final String TYPE_RENAME_PLAYER = "rename_player";
    public static final String TYPE_RENAME_SCREEN = "rename_screen";
    public static final String TYPE_UPDATE_PLAYBACK = "update_playback";
    public static final String TYPE_SEEK = "seek";
    public static final String TYPE_UPDATE_SCREEN_UV = "update_screen_uv";
    public static final String TYPE_UPDATE_SCREEN_CHANNEL = "update_screen_channel";
    public static final String TYPE_ADD_VIDEO = "add_video";
    public static final String TYPE_REMOVE_VIDEO = "remove_video";
    public static final String TYPE_MOVE_VIDEO = "move_video";

    public static final String TYPE_GRANT_CONTROL = "grant_control";
    public static final String TYPE_REVOKE_CONTROL = "revoke_control";

    public static final String TYPE_SYNC_PLAYERS = "sync_players";
    public static final String TYPE_UPDATE_PROGRESS = "update_progress";

    // ==================== Network Packet builders ====================

    public static String buildSyncPlayersPacket(VideoPlayerSnapshot snapshot) {
        JsonObject root = new JsonObject();
        root.addProperty("type", TYPE_SYNC_PLAYERS);
        root.add("data", encodeSnapshot(snapshot));
        return GSON.toJson(root);
    }

    public static String buildUpdateProgressPacket(UUID playerId, PlaybackStatus status, int currentIndex, int progressSeconds) {
        JsonObject root = new JsonObject();
        root.addProperty("type", TYPE_UPDATE_PROGRESS);
        JsonObject data = new JsonObject();
        data.addProperty("playerId", playerId.toString());
        data.addProperty("status", status.ordinal());
        data.addProperty("currentIndex", currentIndex);
        data.addProperty("progressSeconds", progressSeconds);
        root.add("data", data);
        return GSON.toJson(root);
    }

    public static JsonObject parsePacket(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    public static String getType(JsonObject packet) {
        return packet.get("type").getAsString();
    }

    public static JsonObject getData(JsonObject packet) {
        return packet.getAsJsonObject("data");
    }

    public static String buildErrorPacket(String message) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "error");
        root.addProperty("message", message);
        return GSON.toJson(root);
    }

    public static String toJson(JsonElement element) {
        return GSON.toJson(element);
    }
}
