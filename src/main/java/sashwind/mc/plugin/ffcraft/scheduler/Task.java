package sashwind.mc.plugin.ffcraft.scheduler;

/**
 * Unified task representation across Folia and standard Bukkit schedulers.
 */
public interface Task {

    /**
     * Attempts to cancel this task.
     */
    void cancel();

    /**
     * Checks if this task has been cancelled.
     *
     * @return true if cancelled, false otherwise
     */
    boolean isCancelled();
}
