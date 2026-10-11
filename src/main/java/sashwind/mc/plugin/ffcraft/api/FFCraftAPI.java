package sashwind.mc.plugin.ffcraft.api;

import org.bukkit.entity.Player;
import sashwind.mc.plugin.ffcraft.model.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public programming API for FFCraft.
 * Third-party plugins can access this via FFCraft.getApi() or Bukkit's ServicesManager.
 */
public interface FFCraftAPI {

    // ==================== Player Query & Management ====================

    /**
     * Gets all video players currently loaded on the server.
     */
    List<ServerVideoPlayer> getPlayers();

    /**
     * Finds a video player by its UUID.
     */
    Optional<ServerVideoPlayer> getPlayer(UUID playerId);

    /**
     * Finds a video player by its display name (case-insensitive).
     */
    Optional<ServerVideoPlayer> getPlayerByName(String name);

    /**
     * Creates a new video player.
     *
     * @param name     the name of the player
     * @param isPublic whether this player is visible to all players
     * @param owner    the UUID of the owner (can be null/console UUID)
     * @return the created ServerVideoPlayer
     */
    ServerVideoPlayer createPlayer(String name, boolean isPublic, UUID owner);

    /**
     * Deletes a video player by UUID.
     */
    void deletePlayer(UUID playerId);

    /**
     * Renames an existing video player.
     */
    ServerVideoPlayer renamePlayer(UUID playerId, String newName);

    /**
     * Toggles whether a video player is public or private.
     */
    ServerVideoPlayer setPublic(UUID playerId, boolean isPublic);

    // ==================== Playback Control ====================

    /**
     * Starts or resumes playback for the specified player.
     */
    void play(UUID playerId);

    /**
     * Pauses playback for the specified player.
     */
    void pause(UUID playerId);

    /**
     * Stops playback and resets progress to 0.
     */
    void stop(UUID playerId);

    /**
     * Jumps to the specified second in the currently playing video.
     */
    void seek(UUID playerId, int seconds);

    /**
     * Sets the volume (0 - 100) of the video player.
     */
    void setVolume(UUID playerId, int volume);

    /**
     * Sets the active playlist item index and immediately starts playing it.
     */
    void playIndex(UUID playerId, int index);

    /**
     * Plays the next video in the playlist (if available).
     */
    void next(UUID playerId);

    /**
     * Plays the previous video in the playlist (if available).
     */
    void previous(UUID playerId);

    /**
     * Sets the playback loop/order mode (SEQUENTIAL, LOOP_SINGLE, LOOP_ALL, RANDOM).
     */
    void setPlaybackMode(UUID playerId, PlaybackMode mode);

    /**
     * Sets a custom playback state directly.
     */
    void setPlaybackState(UUID playerId, PlaybackState state);

    // ==================== Playlist Management ====================

    /**
     * Adds a video source to the end of the playlist.
     */
    void addVideo(UUID playerId, VideoSource video);

    /**
     * Adds a video source to the end of the playlist with individual properties.
     */
    default void addVideo(UUID playerId, String url, int targetWidth, int targetHeight, Integer targetFps) {
        addVideo(playerId, new VideoSource(url, targetWidth, targetHeight, targetFps));
    }

    /**
     * Removes a video from the playlist by index.
     */
    void removeVideo(UUID playerId, int index);

    /**
     * Moves a video in the playlist from one index to another.
     */
    void moveVideo(UUID playerId, int fromIndex, int toIndex);

    /**
     * Clears all videos from the playlist.
     */
    void clearPlaylist(UUID playerId);

    // ==================== Screen Management ====================

    /**
     * Creates and attaches a screen to a video player.
     */
    ServerVideoScreen createScreen(CreateScreenRequest request);

    /**
     * Deletes a screen from a video player.
     */
    void deleteScreen(UUID playerId, UUID screenId);

    /**
     * Renames a screen.
     */
    ServerVideoScreen renameScreen(UUID playerId, UUID screenId, String newName);

    /**
     * Updates UV transformation of a screen.
     */
    ServerVideoScreen updateScreenUv(UUID playerId, UUID screenId, UvTransform uvTransform);

    /**
     * Updates curved screen radius.
     */
    ServerVideoScreen updateScreenRadius(UUID playerId, UUID screenId, double radius);

    /**
     * Updates screen color channel states.
     */
    ServerVideoScreen updateScreenChannel(UUID playerId, UUID screenId, ScreenChannelState channelState);

    // ==================== Mesh & Topology Calculation ====================

    /**
     * Computes the complete 3D mesh for the given screen:
     * - Raw data (positions, pitch, yaw)
     * - Decomposed triangle faces, winding order (point indices), and normal orientation
     * - Transformed UV mapping coordinates per vertex
     */
    sashwind.mc.plugin.ffcraft.model.mesh.ComputedScreenMesh computeScreenMesh(ServerVideoScreen screen);

    /**
     * Finds the screen and computes its 3D mesh.
     */
    sashwind.mc.plugin.ffcraft.model.mesh.ComputedScreenMesh computeScreenMesh(UUID playerId, UUID screenId);

    // ==================== Client Sync ====================

    /**
     * Broadcasts full player/screen state sync to all online players.
     */
    void syncAll();

    /**
     * Sends full player/screen state sync to a specific player.
     */
    void syncTo(Player player);
}
