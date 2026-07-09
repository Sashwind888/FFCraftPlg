package sashwind.mc.plugin.ffcraft.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.java.JavaPlugin;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 * Persistence layer using JSON files.
 * Saved to plugins/FFCraft/data.json
 */
public class VideoPlayerSavedData {

    private final JavaPlugin plugin;
    private final Path dataFile;
    private final List<ServerVideoPlayer> players;
    private boolean dirty = false;

    public VideoPlayerSavedData(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = plugin.getDataFolder().toPath().resolve("data.json");
        this.players = new CopyOnWriteArrayList<>();
    }

    public void load() {
        if (!Files.exists(dataFile)) {
            plugin.getLogger().info("No saved data found, starting fresh.");
            return;
        }

        try (Reader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray playersArr = root.getAsJsonArray("players");
            List<ServerVideoPlayer> loaded = new ArrayList<>();
            for (JsonElement e : playersArr) {
                loaded.add(CodecHelper.decodeServerVideoPlayer(e.getAsJsonObject()));
            }
            players.clear();
            players.addAll(loaded);
            plugin.getLogger().info("Loaded " + players.size() + " video player(s) from saved data.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load saved data", e);
        }
    }

    public void save() {
        if (!dirty) return;

        try {
            if (!Files.exists(dataFile.getParent())) {
                Files.createDirectories(dataFile.getParent());
            }

            JsonObject root = new JsonObject();
            JsonArray playersArr = new JsonArray();
            for (ServerVideoPlayer player : players) {
                playersArr.add(CodecHelper.encodeServerVideoPlayer(player));
            }
            root.add("players", playersArr);

            try (Writer writer = Files.newBufferedWriter(dataFile, StandardCharsets.UTF_8)) {
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                writer.write(gson.toJson(root));
            }

            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save data", e);
        }
    }

    public void setDirty() {
        this.dirty = true;
    }

    public List<ServerVideoPlayer> players() {
        return players;
    }

    public void saveIfDirty() {
        if (dirty) {
            save();
        }
    }
}
