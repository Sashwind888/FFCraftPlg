package sashwind.mc.plugin.ffcraft.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;

import java.util.UUID;

/**
 * Fired when a video player is created.
 */
public class VideoPlayerCreateEvent extends VideoPlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String playerName;
    private final boolean isPublic;
    private final UUID ownerId;
    private boolean cancelled;

    public VideoPlayerCreateEvent(ServerVideoPlayer videoPlayer, Player actor, String playerName, boolean isPublic, UUID ownerId) {
        super(videoPlayer, actor);
        this.playerName = playerName;
        this.isPublic = isPublic;
        this.ownerId = ownerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public UUID getOwnerId() {
        return ownerId;
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
