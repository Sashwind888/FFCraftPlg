package sashwind.mc.plugin.ffcraft.service;

import java.util.UUID;
import org.bukkit.entity.Player;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;

/**
 * Fine-grained permission system for FFCraft.
 *
 * Create (independent of admin):
 *   ffcraft.create.public      create a public player
 *   ffcraft.create.private     create a private player
 *   ffcraft.create             both of the above
 *
 * Admin (global, default: op — parent node ffcraft.admin grants all children):
 *   ffcraft.admin.delete          delete a player
 *   ffcraft.admin.setpublic       toggle public/private
 *   ffcraft.admin.reload          re-sync all clients
 *
 * Manage (per-player; owner and admin always have it):
 *   ffcraft.manage.<playerId>     delegate management of one player (rename,
 *                                 screens, playlist, grant/revoke)
 *   ffcraft.manage                blanket: manage every player
 *
 * Control (per-player playback operations; owner/admin/controlUsers always have it):
 *   ffcraft.control.<playerId>    delegate playback control of one player
 *   ffcraft.control               blanket: control every player
 *
 * View   — Public player: everyone can see screen + browse playlist (read-only).
 *          Private player: only Admin / Owner / controlUsers can see.
 *
 * Node checks walk up the parent chain (e.g. ffcraft.create.public →
 * ffcraft.create) and also accept the "&lt;node&gt;.*" form,
 * so granting any ancestor covers all descendants regardless of whether the
 * permission plugin propagates plugin.yml children.
 */
public final class Permissions {

    private static final String ADMIN = "ffcraft.admin";
    private static final String CREATE = "ffcraft.create";
    private static final String CONTROL_PREFIX = "ffcraft.control.";
    private static final String MANAGE_PREFIX = "ffcraft.manage.";

    private Permissions() {}

    // ── Node check ─────────────────────────────────

    /**
     * Checks a permission node, its ancestors (down to the two-segment root
     * such as {@code ffcraft.admin}, never bare {@code ffcraft}), and the     * wildcard form {@code <node>.*} at each level. OP always passes.
     */
    public static boolean canNode(Player player, String node) {
        if (player == null) return false;
        if (player.isOp()) return true;
        String n = node;
        while (n.indexOf('.') >= 0) {
            if (player.hasPermission(n) || player.hasPermission(n + ".*")) return true;
            n = n.substring(0, n.lastIndexOf('.'));
            if (n.indexOf('.') < 0) return false; // stop before bare "ffcraft"
        }
        return false;
    }

    // ── Admin ──────────────────────────────────────

    /** Blanket admin: OP or ffcraft.admin (covers every admin sub-node). */
    public static boolean canAdmin(Player player) {
        return canNode(player, ADMIN);
    }

    public static boolean canCreate(Player player, boolean isPublic) {
        return canNode(player, CREATE + "." + (isPublic ? "public" : "private"));
    }

    public static boolean canDelete(Player player) {
        return canNode(player, ADMIN + ".delete");
    }

    public static boolean canSetPublic(Player player) {
        return canNode(player, ADMIN + ".setpublic");
    }

    public static boolean canReload(Player player) {
        return canNode(player, ADMIN + ".reload");
    }

    // ── Manage (Admin, Owner, or delegated) ────────

    public static boolean canManage(Player player, ServerVideoPlayer vp) {
        if (player == null) return false;
        if (canAdmin(player)) return true;
        if (vp.creator().equals(player.getUniqueId())) return true;
        return canNode(player, MANAGE_PREFIX + vp.id());
    }

    // ── Control (Manage or granted control) ────────

    public static boolean canControl(Player player, ServerVideoPlayer vp) {
        if (player == null) return false;
        if (canManage(player, vp)) return true;
        // Dynamic node ffcraft.control.<playerId> (walks up to ffcraft.control[.*])
        if (canNode(player, CONTROL_PREFIX + vp.id())) return true;
        // Persisted controlUsers set
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
        return CONTROL_PREFIX + playerId;
    }

    public static String managePermission(UUID playerId) {
        return MANAGE_PREFIX + playerId;
    }
}
