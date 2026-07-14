package sashwind.mc.plugin.ffcraft.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import sashwind.mc.plugin.ffcraft.model.CreatePlayerRequest;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;
import sashwind.mc.plugin.ffcraft.network.Networking;
import sashwind.mc.plugin.ffcraft.service.Permissions;
import sashwind.mc.plugin.ffcraft.service.VideoPlayerService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /ffcraft command implementation.
 *
 *   Admin only:  create, delete, setpublic
 *   Manage:      rename, addvideo, removevideo, movevideo, grant, revoke
 *   All:         list, info, reload
 */
public class FFCraftCommands implements CommandExecutor, TabCompleter {

    private final VideoPlayerService service;
    private final Networking networking;

    public FFCraftCommands(VideoPlayerService service, Networking networking) {
        this.service = service;
        this.networking = networking;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        try {
            switch (sub) {
                case "create"   -> handleCreate(sender, args);
                case "list"     -> handleList(sender);
                case "info"     -> handleInfo(sender, args);
                case "rename"   -> handleRename(sender, args);
                case "delete"   -> handleDelete(sender, args);
                case "setpublic"-> handleSetPublic(sender, args);
                case "grant"    -> handleGrant(sender, args);
                case "revoke"   -> handleRevoke(sender, args);
                case "reload"   -> handleReload(sender);
                default -> {
                    sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + sub);
                    sendUsage(sender);
                }
            }
        } catch (SecurityException e) {
            sender.sendMessage(ChatColor.RED + "Permission denied: " + e.getMessage());
        } catch (NoSuchElementException e) {
            sender.sendMessage(ChatColor.RED + "Not found: " + e.getMessage());
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
        }
        return true;
    }

    // ── Handlers ──────────────────────────────────────

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Usage: /ffcraft create <name> [public|private]"); return; }
        if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + "Player only."); return; }

        boolean isPublic = args.length >= 3 && args[2].equalsIgnoreCase("public");
        ServerVideoPlayer p = service.createPlayer(player, new CreatePlayerRequest(args[1], isPublic));
        networking.syncAll();
        sender.sendMessage(ChatColor.GREEN + "Created '" + p.name() + "' (owner: you).");
    }

    private void handleList(CommandSender sender) {
        List<ServerVideoPlayer> players = service.players();
        if (players.isEmpty()) { sender.sendMessage(ChatColor.YELLOW + "No video players."); return; }

        sender.sendMessage(ChatColor.GOLD + "=== FFCraft Players (" + players.size() + ") ===");
        for (ServerVideoPlayer p : players) {
            String vis = p.isPublic() ? ChatColor.GREEN + "Public" : ChatColor.RED + "Private";
            String owner = "?";
            if (!p.editors().isEmpty()) {
                owner = Optional.ofNullable(Bukkit.getOfflinePlayer(p.editors().iterator().next()).getName()).orElse("?");
            }
            sender.sendMessage(ChatColor.YELLOW + "  " + p.name()
                + ChatColor.GRAY + " [" + vis + ChatColor.GRAY + "] "
                + ChatColor.AQUA + p.playbackState().status().name()
                + ChatColor.DARK_GRAY + " owner:" + owner
                + ChatColor.DARK_GRAY + " (" + p.id().toString().substring(0, 8) + "...)");
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Usage: /ffcraft info <uuid>"); return; }

        UUID id = resolveUuid(args[1]);
        if (id == null) return;

        ServerVideoPlayer p = service.findPlayer(id).orElse(null);
        if (p == null) { sender.sendMessage(ChatColor.RED + "Not found: " + id); return; }

        String owner = "?";
        if (!p.editors().isEmpty()) {
            owner = Optional.ofNullable(Bukkit.getOfflinePlayer(p.editors().iterator().next()).getName()).orElse("?");
        }

        sender.sendMessage(ChatColor.GOLD + "=== " + p.name() + " ===");
        sender.sendMessage(ChatColor.GRAY + "  UUID: " + p.id());
        sender.sendMessage(ChatColor.GRAY + "  Owner: " + owner);
        sender.sendMessage(ChatColor.GRAY + "  Public: " + p.isPublic());
        sender.sendMessage(ChatColor.GRAY + "  Status: " + p.playbackState().status()
            + " | Mode: " + p.playbackState().mode()
            + " | Volume: " + p.playbackState().volume());
        sender.sendMessage(ChatColor.GRAY + "  Progress: " + p.playbackState().progressSeconds() + "s"
            + " | Index: " + p.playbackState().currentIndex());
        sender.sendMessage(ChatColor.GRAY + "  Playlist: " + p.playlist().size() + " video(s)"
            + " | Screens: " + p.screens().size());
        sender.sendMessage(ChatColor.GRAY + "  Editors: " + p.editors().size()
            + " | Control users: " + p.controlUsers().size());
        if (!p.controlUsers().isEmpty()) {
            for (UUID u : p.controlUsers()) {
                String name = Bukkit.getOfflinePlayer(u).getName();
                sender.sendMessage(ChatColor.DARK_GRAY + "    - " + (name != null ? name : u.toString().substring(0, 8)));
            }
        }
    }

    private void handleRename(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage(ChatColor.RED + "Usage: /ffcraft rename <uuid> <newName>"); return; }
        if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + "Player only."); return; }

        ServerVideoPlayer p = service.renamePlayer(player, UUID.fromString(args[1]), args[2]);
        networking.syncAll();
        sender.sendMessage(ChatColor.GREEN + "Renamed to '" + p.name() + "'.");
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Usage: /ffcraft delete <uuid>"); return; }
        if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + "Player only."); return; }

        service.deletePlayer(player, UUID.fromString(args[1]));
        networking.syncAll();
        sender.sendMessage(ChatColor.GREEN + "Deleted.");
    }

    private void handleSetPublic(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage(ChatColor.RED + "Usage: /ffcraft setpublic <uuid> <true|false>"); return; }
        if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + "Player only."); return; }

        ServerVideoPlayer p = service.setPublic(player, UUID.fromString(args[1]), Boolean.parseBoolean(args[2]));
        networking.syncAll();
        sender.sendMessage(ChatColor.GREEN + "'" + p.name() + "' is now " + (p.isPublic() ? "public" : "private") + ".");
    }

    private void handleGrant(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage(ChatColor.RED + "Usage: /ffcraft grant <playerId> <playerName>"); return; }
        if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + "Player only."); return; }

        UUID playerId = UUID.fromString(args[1]);
        UUID target = Bukkit.getOfflinePlayer(args[2]).getUniqueId();

        ServerVideoPlayer p = service.grantControl(player, playerId, target);
        networking.syncAll();
        String name = Bukkit.getOfflinePlayer(target).getName();
        sender.sendMessage(ChatColor.GREEN + "Granted control on '" + p.name() + "' to " + (name != null ? name : args[2]) + ".");
    }

    private void handleRevoke(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage(ChatColor.RED + "Usage: /ffcraft revoke <playerId> <playerName>"); return; }
        if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + "Player only."); return; }

        UUID playerId = UUID.fromString(args[1]);
        UUID target = Bukkit.getOfflinePlayer(args[2]).getUniqueId();

        ServerVideoPlayer p = service.revokeControl(player, playerId, target);
        networking.syncAll();
        String name = Bukkit.getOfflinePlayer(target).getName();
        sender.sendMessage(ChatColor.GREEN + "Revoked control on '" + p.name() + "' from " + (name != null ? name : args[2]) + ".");
    }

    private void handleReload(CommandSender sender) {
        networking.syncAll();
        sender.sendMessage(ChatColor.GREEN + "FFCraft synced to all clients.");
    }

    // ── Helpers ───────────────────────────────────────

    private UUID resolveUuid(String input) {
        try { return UUID.fromString(input); } catch (IllegalArgumentException ignored) {}
        String partial = input.toLowerCase();
        Optional<ServerVideoPlayer> found = service.players().stream()
            .filter(p -> p.id().toString().toLowerCase().startsWith(partial)).findFirst();
        return found.map(ServerVideoPlayer::id).orElse(null);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== FFCraft Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/ffcraft create <name> [public|private]" + ChatColor.GRAY + "  — requires admin");
        sender.sendMessage(ChatColor.YELLOW + "/ffcraft list" + ChatColor.GRAY + "                             — list all players");
        sender.sendMessage(ChatColor.YELLOW + "/ffcraft info <uuid>" + ChatColor.GRAY + "                        — show details");
        sender.sendMessage(ChatColor.YELLOW + "/ffcraft rename <uuid> <newName>" + ChatColor.GRAY + "           — requires manage");
        sender.sendMessage(ChatColor.YELLOW + "/ffcraft delete <uuid>" + ChatColor.GRAY + "                      — requires admin");
        sender.sendMessage(ChatColor.YELLOW + "/ffcraft setpublic <uuid> <true|false>" + ChatColor.GRAY + "     — requires admin");
        sender.sendMessage(ChatColor.YELLOW + "/ffcraft grant <playerId> <playerName>" + ChatColor.GRAY + "    — requires manage");
        sender.sendMessage(ChatColor.YELLOW + "/ffcraft revoke <playerId> <playerName>" + ChatColor.GRAY + "   — requires manage");
        sender.sendMessage(ChatColor.YELLOW + "/ffcraft reload" + ChatColor.GRAY + "                           — re-sync all clients");
    }

    // ── Tab complete ──────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("create", "list", "info", "rename", "delete", "setpublic", "grant", "revoke", "reload").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (List.of("info", "rename", "delete", "setpublic", "grant", "revoke").contains(sub)) {
                return service.players().stream().map(p -> p.id().toString())
                    .filter(u -> u.startsWith(args[1])).collect(Collectors.toList());
            }
            if (sub.equals("create")) return List.of("<name>");
        }
        if (args.length == 3 && List.of("grant", "revoke").contains(args[0].toLowerCase())) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setpublic")) {
            return List.of("true", "false");
        }
        return List.of();
    }
}
