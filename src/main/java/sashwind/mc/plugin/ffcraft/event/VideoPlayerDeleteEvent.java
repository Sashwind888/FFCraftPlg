package sashwind.mc.plugin.ffcraft.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;

/**
 * Fired before a video player is deleted.
 */
public class VideoPlayerDeleteEvent extends VideoPlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private boolean cancelled;

    public VideoPlayerDeleteEvent(ServerVideoPlayer videoPlayer, Player actor) {
        super(videoPlayer, actor);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
