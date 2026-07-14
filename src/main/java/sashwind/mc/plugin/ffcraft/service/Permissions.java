package sashwind.mc.plugin.ffcraft.service;

import java.util.UUID;
import org.bukkit.entity.Player;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;

/**
 * Four-tier permission system for FFCraft.
 *
 * Admin  — ffcraft.admin (OP): global management (create/delete any player, toggle public/private, manage all)
 * Manage — Admin OR the player's owner: full control of a specific player (playlist, screens, UV, grant/revoke control)
 * Control — Manage OR ffcraft.control.<playerId> holder: playback operations only (play/pause/stop, prev/next, volume)
 * View   — Public player: everyone can see screen + browse playlist (read-only).
 *          Private player: only Admin / Owner / controlUsers can see.
 */
public final class Permissions {

    private static final String PREFIX = "ffcraft.control.";

    private Permissions() {}

    // ── Admin ──────────────────────────────────────

    public static boolean canAdmin(Player player) {
        if (player == null) return false;
        return player.isOp() || player.hasPermission("ffcraft.admin");
    }

    // ── Manage (Admin or Owner) ────────────────────

    public static boolean canManage(Player player, ServerVideoPlayer vp) {
        if (player == null) return false;
        if (canAdmin(player)) return true;
        return vp.creator().equals(player.getUniqueId());
    }

    // ── Control (Manage or granted control) ────────

    public static boolean canControl(Player player, ServerVideoPlayer vp) {
        if (player == null) return false;
        if (canManage(player, vp)) return true;
        // Check dynamic permission node ffcraft.control.<playerId>
        if (player.hasPermission(PREFIX + vp.id())) return true;
        // Check persisted controlUsers set
        return vp.controlUsers().contains(player.getUniqueId());
    }

    // ── View ───────────────────────────────────────

    /** Whether a player can see this video player in lists and syncs.
     *  Public: everyone. Private: only admin/creator/controlUsers. */
    public static boolean canView(Player player, ServerVideoPlayer vp) {
        if (player == null) return false;
        if (vp.isPublic()) return true;
        return canControl(player, vp);
    }

    // ── Grant / Revoke helpers ─────────────────────

    public static String controlPermission(UUID playerId) {
        return PREFIX + playerId;
    }
}
