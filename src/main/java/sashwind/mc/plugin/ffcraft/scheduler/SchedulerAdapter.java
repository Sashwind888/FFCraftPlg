package sashwind.mc.plugin.ffcraft.scheduler;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Universal Scheduler Adapter supporting Folia and standard Bukkit/Spigot platforms.
 */
public final class SchedulerAdapter {

    private static final boolean FOLIA;
    private static final IScheduler SCHEDULER;

    static {
        boolean foliaDetected = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            foliaDetected = true;
        } catch (ClassNotFoundException | LinkageError ignored) {}
        FOLIA = foliaDetected;

        if (FOLIA) {
            SCHEDULER = new FoliaScheduler();
        } else {
            SCHEDULER = new BukkitSchedulerImpl();
        }
    }

    private SchedulerAdapter() {}

    /**
     * Checks if current server environment is running Folia.
     */
    public static boolean isFolia() {
        return FOLIA;
    }

    /**
     * Gets the active scheduler instance.
     */
    public static IScheduler get() {
        return SCHEDULER;
    }

    // ── Global Region ──────────────────────────────────────

    public static Task runGlobal(Plugin plugin, Runnable runnable) {
        return SCHEDULER.runGlobal(plugin, runnable);
    }

    public static Task runGlobalLater(Plugin plugin, Runnable runnable, long delayTicks) {
        return SCHEDULER.runGlobalLater(plugin, runnable, delayTicks);
    }

    public static Task runGlobalTimer(Plugin plugin, Runnable runnable, long initialDelayTicks, long periodTicks) {
        return SCHEDULER.runGlobalTimer(plugin, runnable, initialDelayTicks, periodTicks);
    }

    // ── Asynchronous ───────────────────────────────────────

    public static Task runAsync(Plugin plugin, Runnable runnable) {
        return SCHEDULER.runAsync(plugin, runnable);
    }

    public static Task runAsyncLater(Plugin plugin, Runnable runnable, long delayTicks) {
        return SCHEDULER.runAsyncLater(plugin, runnable, delayTicks);
    }

    public static Task runAsyncTimer(Plugin plugin, Runnable runnable, long initialDelayTicks, long periodTicks) {
        return SCHEDULER.runAsyncTimer(plugin, runnable, initialDelayTicks, periodTicks);
    }

    // ── Entity / Player ────────────────────────────────────

    public static Task runForEntity(Plugin plugin, Entity entity, Runnable runnable) {
        return SCHEDULER.runForEntity(plugin, entity, runnable, null);
    }

    public static Task runForEntity(Plugin plugin, Entity entity, Runnable runnable, Runnable retired) {
        return SCHEDULER.runForEntity(plugin, entity, runnable, retired);
    }

    public static Task runForEntityLater(Plugin plugin, Entity entity, Runnable runnable, long delayTicks) {
        return SCHEDULER.runForEntityLater(plugin, entity, runnable, null, delayTicks);
    }

    public static Task runForEntityLater(Plugin plugin, Entity entity, Runnable runnable, Runnable retired, long delayTicks) {
        return SCHEDULER.runForEntityLater(plugin, entity, runnable, retired, delayTicks);
    }

    public static Task runForEntityTimer(Plugin plugin, Entity entity, Runnable runnable, long initialDelayTicks, long periodTicks) {
        return SCHEDULER.runForEntityTimer(plugin, entity, runnable, null, initialDelayTicks, periodTicks);
    }

    public static Task runForEntityTimer(Plugin plugin, Entity entity, Runnable runnable, Runnable retired, long initialDelayTicks, long periodTicks) {
        return SCHEDULER.runForEntityTimer(plugin, entity, runnable, retired, initialDelayTicks, periodTicks);
    }

    // ── Region / Location ──────────────────────────────────

    public static Task runAtLocation(Plugin plugin, Location location, Runnable runnable) {
        return SCHEDULER.runAtLocation(plugin, location, runnable);
    }

    public static Task runAtLocationLater(Plugin plugin, Location location, Runnable runnable, long delayTicks) {
        return SCHEDULER.runAtLocationLater(plugin, location, runnable, delayTicks);
    }

    public static Task runAtLocationTimer(Plugin plugin, Location location, Runnable runnable, long initialDelayTicks, long periodTicks) {
        return SCHEDULER.runAtLocationTimer(plugin, location, runnable, initialDelayTicks, periodTicks);
    }

    public static Task runAtLocation(Plugin plugin, World world, int chunkX, int chunkZ, Runnable runnable) {
        return SCHEDULER.runAtLocation(plugin, world, chunkX, chunkZ, runnable);
    }

    public static Task runAtLocationLater(Plugin plugin, World world, int chunkX, int chunkZ, Runnable runnable, long delayTicks) {
        return SCHEDULER.runAtLocationLater(plugin, world, chunkX, chunkZ, runnable, delayTicks);
    }

    public static Task runAtLocationTimer(Plugin plugin, World world, int chunkX, int chunkZ, Runnable runnable, long initialDelayTicks, long periodTicks) {
        return SCHEDULER.runAtLocationTimer(plugin, world, chunkX, chunkZ, runnable, initialDelayTicks, periodTicks);
    }

    // ── Cancellation ───────────────────────────────────────

    public static void cancelTasks(Plugin plugin) {
        SCHEDULER.cancelTasks(plugin);
    }
}
