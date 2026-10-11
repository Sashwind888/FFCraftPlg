package sashwind.mc.plugin.ffcraft.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;
import sashwind.mc.plugin.ffcraft.model.VideoSource;

import java.util.Optional;

/**
 * Fired when the active video track index in the playlist changes.
 */
public class VideoPlayerTrackChangeEvent extends VideoPlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int previousIndex;
    private final int newIndex;
    private final VideoSource previousSource;
    private final VideoSource newSource;
    private boolean cancelled;

    public VideoPlayerTrackChangeEvent(ServerVideoPlayer videoPlayer, Player actor, int previousIndex, int newIndex) {
        super(videoPlayer, actor);
        this.previousIndex = previousIndex;
        this.newIndex = newIndex;

        this.previousSource = (previousIndex >= 0 && previousIndex < videoPlayer.playlist().size())
            ? videoPlayer.playlist().get(previousIndex) : null;
        this.newSource = (newIndex >= 0 && newIndex < videoPlayer.playlist().size())
            ? videoPlayer.playlist().get(newIndex) : null;
    }

    public int getPreviousIndex() {
        return previousIndex;
    }

    public int getNewIndex() {
        return newIndex;
    }

    public Optional<VideoSource> getPreviousSource() {
        return Optional.ofNullable(previousSource);
    }

    public Optional<VideoSource> getNewSource() {
        return Optional.ofNullable(newSource);
    }

    public Optional<String> getPreviousUrl() {
        return getPreviousSource().map(VideoSource::url);
    }

    public Optional<String> getNewUrl() {
        return getNewSource().map(VideoSource::url);
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
