package sashwind.mc.plugin.ffcraft.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;
import sashwind.mc.plugin.ffcraft.model.ServerVideoScreen;

/**
 * Fired before a screen is deleted from a video player.
 */
public class VideoScreenDeleteEvent extends VideoPlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ServerVideoScreen screen;
    private boolean cancelled;

    public VideoScreenDeleteEvent(ServerVideoPlayer videoPlayer, Player actor, ServerVideoScreen screen) {
        super(videoPlayer, actor);
        this.screen = screen;
    }

    public ServerVideoScreen getScreen() {
        return screen;
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
