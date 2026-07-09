package sashwind.mc.plugin.ffcraft.model;

public record PlaybackState(
    PlaybackStatus status,
    PlaybackMode mode,
    int currentIndex,
    int progressSeconds,
    int volume,
    long lastUpdatedEpochSeconds
) {
    public PlaybackState withStatus(PlaybackStatus newStatus) {
        return new PlaybackState(newStatus, mode, currentIndex, progressSeconds, volume, System.currentTimeMillis() / 1000);
    }

    public PlaybackState withMode(PlaybackMode newMode) {
        return new PlaybackState(status, newMode, currentIndex, progressSeconds, volume, lastUpdatedEpochSeconds);
    }

    public PlaybackState withIndex(int newIndex) {
        return new PlaybackState(status, mode, newIndex, progressSeconds, volume, lastUpdatedEpochSeconds);
    }

    public PlaybackState withProgress(int newProgressSeconds) {
        return new PlaybackState(status, mode, currentIndex, newProgressSeconds, volume, System.currentTimeMillis() / 1000);
    }

    public PlaybackState withVolume(int newVolume) {
        return new PlaybackState(status, mode, currentIndex, progressSeconds, newVolume, lastUpdatedEpochSeconds);
    }

    public PlaybackState tickProgress() {
        return new PlaybackState(status, mode, currentIndex, progressSeconds + 1, volume, lastUpdatedEpochSeconds);
    }

    public static PlaybackState createDefault() {
        return new PlaybackState(PlaybackStatus.STOPPED, PlaybackMode.SEQUENTIAL, 0, 0, 80, System.currentTimeMillis() / 1000);
    }
}
