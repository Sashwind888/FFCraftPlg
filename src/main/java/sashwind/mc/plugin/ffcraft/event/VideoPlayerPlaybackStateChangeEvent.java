package sashwind.mc.plugin.ffcraft.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import sashwind.mc.plugin.ffcraft.model.PlaybackState;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;

/**
 * Fired when a video player's playback state (status, mode, index, progress, volume) changes.
 * Cancellable: cancelling will prevent the playback state transition.
 */
public class VideoPlayerPlaybackStateChangeEvent extends VideoPlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final PlaybackState oldState;
    private PlaybackState newState;
    private boolean cancelled;

    public VideoPlayerPlaybackStateChangeEvent(ServerVideoPlayer videoPlayer, Player actor, PlaybackState oldState, PlaybackState newState) {
        super(videoPlayer, actor);
        this.oldState = oldState;
        this.newState = newState;
    }

    public PlaybackState getOldState() {
        return oldState;
    }

    public PlaybackState getNewState() {
        return newState;
    }

    public void setNewState(PlaybackState newState) {
        this.newState = newState;
    }

    public boolean isStatusChanged() {
        return oldState.status() != newState.status();
    }

    public boolean isModeChanged() {
        return oldState.mode() != newState.mode();
    }

    public boolean isIndexChanged() {
        return oldState.currentIndex() != newState.currentIndex();
    }

    public boolean isVolumeChanged() {
        return oldState.volume() != newState.volume();
    }

    public boolean isSeek() {
        return oldState.progressSeconds() != newState.progressSeconds();
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
