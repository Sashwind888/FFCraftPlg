package sashwind.mc.plugin.ffcraft.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;

/**
 * Fired when a video player's visibility (public / private) is toggled.
 */
public class VideoPlayerVisibilityChangeEvent extends VideoPlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final boolean wasPublic;
    private boolean newPublic;
    private boolean cancelled;

    public VideoPlayerVisibilityChangeEvent(ServerVideoPlayer videoPlayer, Player actor, boolean wasPublic, boolean newPublic) {
        super(videoPlayer, actor);
        this.wasPublic = wasPublic;
        this.newPublic = newPublic;
    }

    public boolean wasPublic() {
        return wasPublic;
    }

    public boolean isNewPublic() {
        return newPublic;
    }

    public void setNewPublic(boolean newPublic) {
        this.newPublic = newPublic;
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
