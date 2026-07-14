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
import sashwind.mc.plugin.ffcraft.lang.Messages;
import sashwind.mc.plugin.ffcraft.model.PlaybackStatus;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;
import sashwind.mc.plugin.ffcraft.network.Networking;
import sashwind.mc.plugin.ffcraft.service.VideoPlayerService;

import java.util.Objects;

/**
 * FFCraft — Minecraft video player plugin (Spigot/Bukkit).
 */
public final class FFCraft extends JavaPlugin implements Listener {

    private VideoPlayerSavedData savedData;
    private VideoPlayerService service;
    private Networking networking;
    private BukkitTask progressTask;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        // Init i18n — copies lang files to plugins/FFCraft/lang/ on first run
        Messages.init(this);

        savedData = new VideoPlayerSavedData(this);
        savedData.load();

        service = new VideoPlayerService(savedData,
            () -> getServer().getScheduler().runTaskAsynchronously(this, savedData::saveIfDirty));

        networking = new Networking(this, service);
        networking.register();

        FFCraftCommands commands = new FFCraftCommands(service, networking);
        Objects.requireNonNull(getCommand("ffcraft")).setExecutor(commands);
        Objects.requireNonNull(getCommand("ffcraft")).setTabCompleter(commands);

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
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> savedData.saveIfDirty(), 6000L, 6000L);

        // Full state sync (every 5 seconds)
        Bukkit.getScheduler().runTaskTimer(this, () -> networking.syncAll(), 100L, 100L);

        getLogger().info(Messages.get("log.plugin_enabled", service.players().size()));
    }

    @Override
    public void onDisable() {
        if (progressTask != null) progressTask.cancel();
        if (service != null) service.stopAllPlayback();
        if (networking != null) networking.unregister();
        if (savedData != null) savedData.save();
        getLogger().info(Messages.get("log.plugin_disabled"));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) networking.syncTo(player);
        }, 20L);
    }
}
