package sashwind.mc.plugin.ffcraft.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;

/**
 * Fired when items in a playlist are reordered.
 */
public class VideoPlaylistReorderEvent extends VideoPlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int fromIndex;
    private final int toIndex;
    private boolean cancelled;

    public VideoPlaylistReorderEvent(ServerVideoPlayer videoPlayer, Player actor, int fromIndex, int toIndex) {
        super(videoPlayer, actor);
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    public int getFromIndex() {
        return fromIndex;
    }

    public int getToIndex() {
        return toIndex;
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
