package sashwind.mc.plugin.ffcraft.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;
import sashwind.mc.plugin.ffcraft.model.VideoSource;

/**
 * Fired when a video is added to a video player's playlist.
 */
public class VideoPlaylistAddEvent extends VideoPlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final VideoSource videoSource;
    private final int index;
    private boolean cancelled;

    public VideoPlaylistAddEvent(ServerVideoPlayer videoPlayer, Player actor, VideoSource videoSource, int index) {
        super(videoPlayer, actor);
        this.videoSource = videoSource;
        this.index = index;
    }

    public VideoSource getVideoSource() {
        return videoSource;
    }

    public String getUrl() {
        return videoSource.url();
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
