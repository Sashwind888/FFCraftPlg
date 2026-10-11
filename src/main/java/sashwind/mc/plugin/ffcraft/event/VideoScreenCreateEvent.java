package sashwind.mc.plugin.ffcraft.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import sashwind.mc.plugin.ffcraft.model.ScreenVertex;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;
import sashwind.mc.plugin.ffcraft.model.ServerVideoScreen;

import java.util.List;

/**
 * Fired when a screen is added to a video player.
 */
public class VideoScreenCreateEvent extends VideoPlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ServerVideoScreen screen;
    private boolean cancelled;

    public VideoScreenCreateEvent(ServerVideoPlayer videoPlayer, Player actor, ServerVideoScreen screen) {
        super(videoPlayer, actor);
        this.screen = screen;
    }

    public ServerVideoScreen getScreen() {
        return screen;
    }

    public String getScreenName() {
        return screen.name();
    }

    public int getScreenType() {
        return screen.screenType();
    }

    public String getDimension() {
        return screen.dimension();
    }

    public List<ScreenVertex> getVertices() {
        return screen.vertices();
    }

    public double getRadius() {
        return screen.radius();
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
