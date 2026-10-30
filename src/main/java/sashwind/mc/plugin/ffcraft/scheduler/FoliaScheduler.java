package sashwind.mc.plugin.ffcraft.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class FoliaScheduler implements IScheduler {

    @Override
    public Task runGlobal(Plugin plugin, Runnable runnable) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        ScheduledTask task = Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> runnable.run());
        return new FoliaTaskWrapper(task);
    }

    @Override
    public Task runGlobalLater(Plugin plugin, Runnable runnable, long delayTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        long delay = Math.max(1L, delayTicks);
        ScheduledTask task = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), delay);
        return new FoliaTaskWrapper(task);
    }

    @Override
    public Task runGlobalTimer(Plugin plugin, Runnable runnable, long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        long initial = Math.max(1L, initialDelayTicks);
        long period = Math.max(1L, periodTicks);
        ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), initial, period);
        return new FoliaTaskWrapper(task);
    }

    @Override
    public Task runAsync(Plugin plugin, Runnable runnable) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        ScheduledTask task = Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> runnable.run());
        return new FoliaTaskWrapper(task);
    }

    @Override
    public Task runAsyncLater(Plugin plugin, Runnable runnable, long delayTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        long delayMs = Math.max(1L, delayTicks) * 50L;
        ScheduledTask task = Bukkit.getAsyncScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), delayMs, TimeUnit.MILLISECONDS);
        return new FoliaTaskWrapper(task);
    }

    @Override
    public Task runAsyncTimer(Plugin plugin, Runnable runnable, long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        long initialMs = Math.max(1L, initialDelayTicks) * 50L;
        long periodMs = Math.max(1L, periodTicks) * 50L;
        ScheduledTask task = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), initialMs, periodMs, TimeUnit.MILLISECONDS);
        return new FoliaTaskWrapper(task);
    }

    @Override
    public Task runForEntity(Plugin plugin, Entity entity, Runnable runnable, Runnable retired) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        ScheduledTask task = entity.getScheduler().run(plugin, scheduledTask -> runnable.run(), retired);
        return new FoliaTaskWrapper(task);
    }

    @Override
    public Task runForEntityLater(Plugin plugin, Entity entity, Runnable runnable, Runnable retired, long delayTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        long delay = Math.max(1L, delayTicks);
        ScheduledTask task = entity.getScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), retired, delay);
        return new FoliaTaskWrapper(task);
    }

    @Override
    public Task runForEntityTimer(Plugin plugin, Entity entity, Runnable runnable, Runnable retired, long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        long initial = Math.max(1L, initialDelayTicks);
        long period = Math.max(1L, periodTicks);
        ScheduledTask task = entity.getScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), retired, initial, period);
        return new FoliaTaskWrapper(task);
    }

    @Override
    public Task runAtLocation(Plugin plugin, Location location, Runnable runnable) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        ScheduledTask task = Bukkit.getRegionScheduler().run(plugin, location, scheduledTask -> runnable.run());
        return new FoliaTaskWrapper(task);
    }

    @Override
    public Task runAtLocationLater(Plugin plugin, Location location, Runnable runnable, long delayTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        long delay = Math.max(1L, delayTicks);
        ScheduledTask task = Bukkit.getRegionScheduler().runDelayed(plugin, location, scheduledTask -> runnable.run(), delay);
        return new FoliaTaskWrapper(task);
    }

    @Override
    public Task runAtLocationTimer(Plugin plugin, Location location, Runnable runnable, long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        long initial = Math.max(1L, initialDelayTicks);
        long period = Math.max(1L, periodTicks);
        ScheduledTask task = Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, scheduledTask -> runnable.run(), initial, period);
        return new FoliaTaskWrapper(task);
    }

    @Override
    public Task runAtLocation(Plugin plugin, World world, int chunkX, int chunkZ, Runnable runnable) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(world, "world must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        ScheduledTask task = Bukkit.getRegionScheduler().run(plugin, world, chunkX, chunkZ, scheduledTask -> runnable.run());
        return new FoliaTaskWrapper(task);
    }

    @Override
    public Task runAtLocationLater(Plugin plugin, World world, int chunkX, int chunkZ, Runnable runnable, long delayTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(world, "world must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        long delay = Math.max(1L, delayTicks);
        ScheduledTask task = Bukkit.getRegionScheduler().runDelayed(plugin, world, chunkX, chunkZ, scheduledTask -> runnable.run(), delay);
        return new FoliaTaskWrapper(task);
    }

    @Override
    public Task runAtLocationTimer(Plugin plugin, World world, int chunkX, int chunkZ, Runnable runnable, long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(world, "world must not be null");
        Objects.requireNonNull(runnable, "runnable must not be null");
        long initial = Math.max(1L, initialDelayTicks);
        long period = Math.max(1L, periodTicks);
        ScheduledTask task = Bukkit.getRegionScheduler().runAtFixedRate(plugin, world, chunkX, chunkZ, scheduledTask -> runnable.run(), initial, period);
        return new FoliaTaskWrapper(task);
    }

    @Override
    public void cancelTasks(Plugin plugin) {
        if (plugin == null) return;
        try {
            Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
        } catch (Throwable ignored) {}
        try {
            Bukkit.getAsyncScheduler().cancelTasks(plugin);
        } catch (Throwable ignored) {}
    }
}
