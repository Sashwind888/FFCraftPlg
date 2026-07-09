package sashwind.mc.plugin.ffcraft.model;

public record ScreenChannelState(boolean leftEnabled, boolean rightEnabled) {
    public static ScreenChannelState createDefault() {
        return new ScreenChannelState(true, true);
    }
}
