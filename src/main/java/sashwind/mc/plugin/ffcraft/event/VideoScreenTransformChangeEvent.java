package sashwind.mc.plugin.ffcraft.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import sashwind.mc.plugin.ffcraft.model.ScreenChannelState;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;
import sashwind.mc.plugin.ffcraft.model.ServerVideoScreen;
import sashwind.mc.plugin.ffcraft.model.UvTransform;

/**
 * Fired when a screen's UV, radius, or channel state is modified.
 */
public class VideoScreenTransformChangeEvent extends VideoPlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ServerVideoScreen screen;
    private final UvTransform newUv;
    private final Double newRadius;
    private final ScreenChannelState newChannelState;
    private boolean cancelled;

    public VideoScreenTransformChangeEvent(ServerVideoPlayer videoPlayer, Player actor, ServerVideoScreen screen,
                                           UvTransform newUv, Double newRadius, ScreenChannelState newChannelState) {
        super(videoPlayer, actor);
        this.screen = screen;
        this.newUv = newUv;
        this.newRadius = newRadius;
        this.newChannelState = newChannelState;
    }

    public ServerVideoScreen getScreen() {
        return screen;
    }

    public UvTransform getNewUv() {
        return newUv;
    }

    public Double getNewRadius() {
        return newRadius;
    }

    public ScreenChannelState getNewChannelState() {
        return newChannelState;
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
