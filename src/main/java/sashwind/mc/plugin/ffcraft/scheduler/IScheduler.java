package sashwind.mc.plugin.ffcraft.scheduler;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Common scheduling abstraction covering Folia and Bukkit/Spigot models.
 */
public interface IScheduler {

    // ── Global Region ──────────────────────────────────────

    Task runGlobal(Plugin plugin, Runnable runnable);

    Task runGlobalLater(Plugin plugin, Runnable runnable, long delayTicks);

    Task runGlobalTimer(Plugin plugin, Runnable runnable, long initialDelayTicks, long periodTicks);

    // ── Asynchronous ───────────────────────────────────────

    Task runAsync(Plugin plugin, Runnable runnable);

    Task runAsyncLater(Plugin plugin, Runnable runnable, long delayTicks);

    Task runAsyncTimer(Plugin plugin, Runnable runnable, long initialDelayTicks, long periodTicks);

    // ── Entity / Player ────────────────────────────────────

    Task runForEntity(Plugin plugin, Entity entity, Runnable runnable, Runnable retired);

    Task runForEntityLater(Plugin plugin, Entity entity, Runnable runnable, Runnable retired, long delayTicks);

    Task runForEntityTimer(Plugin plugin, Entity entity, Runnable runnable, Runnable retired, long initialDelayTicks, long periodTicks);

    // ── Region / Location ──────────────────────────────────

    Task runAtLocation(Plugin plugin, Location location, Runnable runnable);

    Task runAtLocationLater(Plugin plugin, Location location, Runnable runnable, long delayTicks);

    Task runAtLocationTimer(Plugin plugin, Location location, Runnable runnable, long initialDelayTicks, long periodTicks);

    Task runAtLocation(Plugin plugin, World world, int chunkX, int chunkZ, Runnable runnable);

    Task runAtLocationLater(Plugin plugin, World world, int chunkX, int chunkZ, Runnable runnable, long delayTicks);

    Task runAtLocationTimer(Plugin plugin, World world, int chunkX, int chunkZ, Runnable runnable, long initialDelayTicks, long periodTicks);

    // ── Cancellation ───────────────────────────────────────

    void cancelTasks(Plugin plugin);
}
