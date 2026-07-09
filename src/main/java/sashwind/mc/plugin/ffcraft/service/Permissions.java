package sashwind.mc.plugin.ffcraft.service;

import org.bukkit.entity.Player;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;

/**
 * Permission system for FFCraft plugin.
 *
 * Admin (ffcraft.admin): OP-level access to create/delete players.
 * Edit: can manage screens, playlist, UV, channel for a specific player.
 *   - The video player is public, OR
 *   - They are an admin, OR
 *   - They are in the editors set
 */
public final class Permissions {

    private Permissions() {}

    public static boolean canAdmin(Player player) {
        if (player == null) return false;
        return player.isOp() || player.hasPermission("ffcraft.admin");
    }

    public static boolean canEdit(Player player, ServerVideoPlayer videoPlayer) {
        if (player == null) return false;
        if (canAdmin(player)) return true;
        if (videoPlayer.isPublic()) return true;
        return videoPlayer.editors().contains(player.getUniqueId());
    }
}
