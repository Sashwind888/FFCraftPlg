package sashwind.mc.plugin.ffcraft.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;
import sashwind.mc.plugin.ffcraft.model.VideoSource;

import java.util.Optional;
import java.util.UUID;

/**
 * Base class for all FFCraft video player events.
 * Prefixed with 'VideoPlayer' to avoid any conflict with Bukkit's Player (org.bukkit.entity.Player).
 */
public abstract class VideoPlayerEvent extends Event {

    protected final ServerVideoPlayer videoPlayer;
    protected final Player actor;

    public VideoPlayerEvent(ServerVideoPlayer videoPlayer, Player actor) {
        this.videoPlayer = videoPlayer;
        this.actor = actor;
    }

    public VideoPlayerEvent(ServerVideoPlayer videoPlayer) {
        this(videoPlayer, null);
    }

    /**
     * Gets the video player involved in this event.
     */
    public ServerVideoPlayer getVideoPlayer() {
        return videoPlayer;
    }

    /**
     * Convenient shortcut for the video player UUID.
     */
    public UUID getPlayerId() {
        return videoPlayer.id();
    }

    /**
     * Gets the player/actor who initiated this event, if any.
     * Empty if triggered by server console, scheduler, or third-party plugin API.
     */
    public Optional<Player> getActor() {
        return Optional.ofNullable(actor);
    }

    /**
     * Convenient method to get the currently playing VideoSource (if any).
     */
    public Optional<VideoSource> getCurrentVideoSource() {
        int idx = videoPlayer.playbackState().currentIndex();
        if (idx >= 0 && idx < videoPlayer.playlist().size()) {
            return Optional.of(videoPlayer.playlist().get(idx));
        }
        return Optional.empty();
    }

    /**
     * Convenient method to get the URL of the currently playing video (if any).
     */
    public Optional<String> getCurrentUrl() {
        return getCurrentVideoSource().map(VideoSource::url);
    }
}
