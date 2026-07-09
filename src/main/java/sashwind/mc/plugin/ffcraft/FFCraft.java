package sashwind.mc.plugin.ffcraft;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import sashwind.mc.plugin.ffcraft.command.FFCraftCommands;
import sashwind.mc.plugin.ffcraft.data.VideoPlayerSavedData;
import sashwind.mc.plugin.ffcraft.model.PlaybackStatus;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;
import sashwind.mc.plugin.ffcraft.network.Networking;
import sashwind.mc.plugin.ffcraft.service.VideoPlayerService;

import java.util.Objects;

/**
 * FFCraft — Minecraft video player plugin (Spigot/Bukkit).
 *
 * Server-side component for the FFCraft client mod. Manages video player data,
 * handles network communication with clients, and provides administrative commands.
 *
 * Lifecycle:
 * - onEnable:  load persisted data, start progress ticker, register networking & commands
 * - onDisable: stop all playback, save data, unregister networking
 */
public final class FFCraft extends JavaPlugin implements Listener {

    private VideoPlayerSavedData savedData;
    private VideoPlayerService service;
    private Networking networking;
    private BukkitTask progressTask;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Load persisted data
        savedData = new VideoPlayerSavedData(this);
        savedData.load();

        // Create service
        service = new VideoPlayerService(savedData);

        // Register networking (plugin messaging channel)
        networking = new Networking(this, service);
        networking.register();

        // Register commands
        FFCraftCommands commands = new FFCraftCommands(service, networking);
        Objects.requireNonNull(getCommand("ffcraft")).setExecutor(commands);
        Objects.requireNonNull(getCommand("ffcraft")).setTabCompleter(commands);

        // Register event listeners
        getServer().getPluginManager().registerEvents(this, this);

        // Progress ticker (every second)
        progressTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            service.tickProgress();
            for (ServerVideoPlayer player : service.players()) {
                if (player.playbackState().status() == PlaybackStatus.PLAYING) {
                    networking.syncProgress(player.id(), player.playbackState().status(),
                            player.playbackState().currentIndex(), player.playbackState().progressSeconds());
                }
            }
        }, 20L, 20L);

        // Auto-save (every 5 minutes)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            savedData.saveIfDirty();
        }, 6000L, 6000L);

        // Full state sync (every 5 seconds)
        Bukkit.getScheduler().runTaskTimer(this, () -> networking.syncAll(), 100L, 100L);

        getLogger().info("FFCraft enabled! Loaded " + service.players().size() + " video player(s). Progress ticker: " + (progressTask != null ? "running" : "NOT STARTED"));
    }

    @Override
    public void onDisable() {
        if (progressTask != null) {
            progressTask.cancel();
        }

        if (service != null) {
            service.stopAllPlayback();
        }

        if (networking != null) {
            networking.unregister();
        }

        if (savedData != null) {
            savedData.save();
        }

        getLogger().info("FFCraft disabled. Data saved.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                networking.syncTo(player);
            }
        }, 20L);
    }
}
