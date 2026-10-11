package sashwind.mc.plugin.ffcraft.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;
import sashwind.mc.plugin.ffcraft.model.VideoSource;

/**
 * Fired before a video is removed from a video player's playlist.
 */
public class VideoPlaylistRemoveEvent extends VideoPlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final VideoSource removedSource;
    private final int index;
    private boolean cancelled;

    public VideoPlaylistRemoveEvent(ServerVideoPlayer videoPlayer, Player actor, VideoSource removedSource, int index) {
        super(videoPlayer, actor);
        this.removedSource = removedSource;
        this.index = index;
    }

    public VideoSource getRemovedSource() {
        return removedSource;
    }

    public String getRemovedUrl() {
        return removedSource.url();
    }

    public int getIndex() {
        return index;
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
