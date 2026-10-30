package sashwind.mc.plugin.ffcraft.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class BukkitSchedulerImpl implements IScheduler {

    private BukkitScheduler scheduler() {
        return Bukkit.getScheduler();
    }

    @Override
    public Task runGlobal(Plugin plugin, Runnable runnable) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        return new BukkitTaskWrapper(scheduler().runTask(plugin, runnable));
    }

    @Override
    public Task runGlobalLater(Plugin plugin, Runnable runnable, long delayTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        return new BukkitTaskWrapper(scheduler().runTaskLater(plugin, runnable, Math.max(0L, delayTicks)));
    }

    @Override
    public Task runGlobalTimer(Plugin plugin, Runnable runnable, long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        return new BukkitTaskWrapper(scheduler().runTaskTimer(plugin, runnable, Math.max(0L, initialDelayTicks), Math.max(1L, periodTicks)));
    }

    @Override
    public Task runAsync(Plugin plugin, Runnable runnable) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        return new BukkitTaskWrapper(scheduler().runTaskAsynchronously(plugin, runnable));
    }

    @Override
    public Task runAsyncLater(Plugin plugin, Runnable runnable, long delayTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        return new BukkitTaskWrapper(scheduler().runTaskLaterAsynchronously(plugin, runnable, Math.max(0L, delayTicks)));
    }

    @Override
    public Task runAsyncTimer(Plugin plugin, Runnable runnable, long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        return new BukkitTaskWrapper(scheduler().runTaskTimerAsynchronously(plugin, runnable, Math.max(0L, initialDelayTicks), Math.max(1L, periodTicks)));
    }

    @Override
    public Task runForEntity(Plugin plugin, Entity entity, Runnable runnable, Runnable retired) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        return new BukkitTaskWrapper(scheduler().runTask(plugin, () -> {
            if (entity == null || entity.isValid()) {
                runnable.run();
            } else if (retired != null) {
                retired.run();
            }
        }));
    }

    @Override
    public Task runForEntityLater(Plugin plugin, Entity entity, Runnable runnable, Runnable retired, long delayTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        return new BukkitTaskWrapper(scheduler().runTaskLater(plugin, () -> {
            if (entity == null || entity.isValid()) {
                runnable.run();
            } else if (retired != null) {
                retired.run();
            }
        }, Math.max(0L, delayTicks)));
    }

    @Override
    public Task runForEntityTimer(Plugin plugin, Entity entity, Runnable runnable, Runnable retired, long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        AtomicReference<Task> taskRef = new AtomicReference<>();
        BukkitTask bt = scheduler().runTaskTimer(plugin, () -> {
            if (entity == null || entity.isValid()) {
                runnable.run();
            } else {
                Task current = taskRef.get();
                if (current != null) {
                    current.cancel();
                }
                if (retired != null) {
                    retired.run();
                }
            }
        }, Math.max(0L, initialDelayTicks), Math.max(1L, periodTicks));
        Task task = new BukkitTaskWrapper(bt);
        taskRef.set(task);
        return task;
    }

    @Override
    public Task runAtLocation(Plugin plugin, Location location, Runnable runnable) {
        return runGlobal(plugin, runnable);
    }

    @Override
    public Task runAtLocationLater(Plugin plugin, Location location, Runnable runnable, long delayTicks) {
        return runGlobalLater(plugin, runnable, delayTicks);
    }

    @Override
    public Task runAtLocationTimer(Plugin plugin, Location location, Runnable runnable, long initialDelayTicks, long periodTicks) {
        return runGlobalTimer(plugin, runnable, initialDelayTicks, periodTicks);
    }

    @Override
    public Task runAtLocation(Plugin plugin, World world, int chunkX, int chunkZ, Runnable runnable) {
        return runGlobal(plugin, runnable);
    }

    @Override
    public Task runAtLocationLater(Plugin plugin, World world, int chunkX, int chunkZ, Runnable runnable, long delayTicks) {
        return runGlobalLater(plugin, runnable, delayTicks);
    }

    @Override
    public Task runAtLocationTimer(Plugin plugin, World world, int chunkX, int chunkZ, Runnable runnable, long initialDelayTicks, long periodTicks) {
        return runGlobalTimer(plugin, runnable, initialDelayTicks, periodTicks);
    }

    @Override
    public void cancelTasks(Plugin plugin) {
        if (plugin != null) {
            scheduler().cancelTasks(plugin);
        }
    }
}
