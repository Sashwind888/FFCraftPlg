package sashwind.mc.plugin.ffcraft.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public class FoliaTaskWrapper implements Task {

    private final ScheduledTask scheduledTask;

    public FoliaTaskWrapper(ScheduledTask scheduledTask) {
        this.scheduledTask = scheduledTask;
    }

    @Override
    public void cancel() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
        }
    }

    @Override
    public boolean isCancelled() {
        return scheduledTask != null && scheduledTask.isCancelled();
    }

    public ScheduledTask getScheduledTask() {
        return scheduledTask;
    }
}
