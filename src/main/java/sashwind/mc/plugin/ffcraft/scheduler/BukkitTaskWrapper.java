package sashwind.mc.plugin.ffcraft.scheduler;

import org.bukkit.scheduler.BukkitTask;

public class BukkitTaskWrapper implements Task {

    private final BukkitTask bukkitTask;

    public BukkitTaskWrapper(BukkitTask bukkitTask) {
        this.bukkitTask = bukkitTask;
    }

    @Override
    public void cancel() {
        if (bukkitTask != null) {
            bukkitTask.cancel();
        }
    }

    @Override
    public boolean isCancelled() {
        return bukkitTask != null && bukkitTask.isCancelled();
    }

    public BukkitTask getBukkitTask() {
        return bukkitTask;
    }
}
