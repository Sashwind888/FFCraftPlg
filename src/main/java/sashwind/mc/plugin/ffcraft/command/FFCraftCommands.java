package sashwind.mc.plugin.ffcraft.command;

import org.bukkit.ChatColor;
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
 * Commands:
 *   /ffcraft create <name> [public|private]     - Create player (OP)
 *   /ffcraft list                                - List all players
 *   /ffcraft info <uuid>                         - Show player details
 *   /ffcraft rename <uuid> <newName>             - Rename player (OP/edit)
 *   /ffcraft delete <uuid>                       - Delete player (OP)
 *   /ffcraft setpublic <uuid> <true|false>       - Set public status (OP)
 *   /ffcraft reload                              - Re-sync all clients
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

        String subCommand = args[0].toLowerCase();

        try {
            switch (subCommand) {
                case "create" -> handleCreate(sender, args);
                case "list" -> handleList(sender);
                case "info" -> handleInfo(sender, args);
                case "rename" -> handleRename(sender, args);
                case "delete" -> handleDelete(sender, args);
                case "setpublic" -> handleSetPublic(sender, args);
                case "reload" -> handleReload(sender);
                default -> {
                    sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + subCommand);
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

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /ffcraft create <name> [public|private]");
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return;
        }

        String name = args[1];
        boolean isPublic = args.length >= 3 && args[2].equalsIgnoreCase("public");

        service.createPlayer(player, new CreatePlayerRequest(name, isPublic));
        networking.syncAll();
        sender.sendMessage(ChatColor.GREEN + "Video player '" + name + "' created successfully!");
    }

    private void handleList(CommandSender sender) {
        List<ServerVideoPlayer> players = service.players();
        if (players.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No video players found.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== FFCraft Players (" + players.size() + ") ===");
        for (ServerVideoPlayer p : players) {
            String visibility = p.isPublic() ? ChatColor.GREEN + "Public" : ChatColor.RED + "Private";
            String playing = p.playbackState().status().name();
            sender.sendMessage(ChatColor.YELLOW + "  " + p.name() +
                ChatColor.GRAY + " [" + visibility + ChatColor.GRAY + "] " +
                ChatColor.AQUA + playing +
                ChatColor.DARK_GRAY + " (" + p.id().toString().substring(0, 8) + "...)");
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /ffcraft info <uuid>");
            return;
        }

        UUID id;
        try {
            id = UUID.fromString(args[1]);
        } catch (IllegalArgumentException e) {
            String partial = args[1].toLowerCase();
            Optional<ServerVideoPlayer> found = service.players().stream()
                .filter(p -> p.id().toString().toLowerCase().startsWith(partial))
                .findFirst();
            if (found.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Invalid UUID or no player found with partial: " + partial);
                return;
            }
            id = found.get().id();
        }

        UUID finalId = id;
        ServerVideoPlayer p = service.findPlayer(finalId).orElse(null);

        if (p == null) {
            sender.sendMessage(ChatColor.RED + "Video player not found: " + id);
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== " + p.name() + " ===");
        sender.sendMessage(ChatColor.GRAY + "  UUID: " + p.id());
        sender.sendMessage(ChatColor.GRAY + "  Public: " + p.isPublic());
        sender.sendMessage(ChatColor.GRAY + "  Status: " + p.playbackState().status() +
            " | Mode: " + p.playbackState().mode() +
            " | Volume: " + p.playbackState().volume());
        sender.sendMessage(ChatColor.GRAY + "  Progress: " + p.playbackState().progressSeconds() + "s" +
            " | Index: " + p.playbackState().currentIndex());
        sender.sendMessage(ChatColor.GRAY + "  Playlist: " + p.playlist().size() + " video(s)");
        sender.sendMessage(ChatColor.GRAY + "  Screens: " + p.screens().size());
        if (!p.editors().isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "  Editors: " + p.editors().size());
        }
    }

    private void handleRename(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /ffcraft rename <uuid> <newName>");
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return;
        }

        UUID id = UUID.fromString(args[1]);
        String newName = args[2];

        ServerVideoPlayer updated = service.renamePlayer(player, id, newName);
        networking.syncAll();
        sender.sendMessage(ChatColor.GREEN + "Renamed to '" + updated.name() + "'!");
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /ffcraft delete <uuid>");
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return;
        }

        UUID id = UUID.fromString(args[1]);
        service.deletePlayer(player, id);
        networking.syncAll();
        sender.sendMessage(ChatColor.GREEN + "Video player deleted.");
    }

    private void handleSetPublic(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /ffcraft setpublic <uuid> <true|false>");
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return;
        }

        UUID id = UUID.fromString(args[1]);
        boolean isPublic = Boolean.parseBoolean(args[2]);

        ServerVideoPlayer updated = service.setPublic(player, id, isPublic);
        networking.syncAll();
        sender.sendMessage(ChatColor.GREEN + "Set '" + updated.name() + "' to " +
            (updated.isPublic() ? "public" : "private") + ".");
    }

    private void handleReload(CommandSender sender) {
        if (!(sender instanceof Player player) || !Permissions.canAdmin(player)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to reload.");
            return;
        }

        networking.syncAll();
        sender.sendMessage(ChatColor.GREEN + "FFCraft data synced to all clients.");
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== FFCraft Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/ffcraft create <name> [public|private]");
        sender.sendMessage(ChatColor.YELLOW + "/ffcraft list");
        sender.sendMessage(ChatColor.YELLOW + "/ffcraft info <uuid>");
        sender.sendMessage(ChatColor.YELLOW + "/ffcraft rename <uuid> <newName>");
        sender.sendMessage(ChatColor.YELLOW + "/ffcraft delete <uuid>");
        sender.sendMessage(ChatColor.YELLOW + "/ffcraft setpublic <uuid> <true|false>");
        sender.sendMessage(ChatColor.YELLOW + "/ffcraft reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "list", "info", "rename", "delete", "setpublic", "reload").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("info") || sub.equals("rename") || sub.equals("delete") || sub.equals("setpublic")) {
                return service.players().stream()
                    .map(p -> p.id().toString())
                    .filter(uuid -> uuid.startsWith(args[1]))
                    .collect(Collectors.toList());
            }
            if (sub.equals("create")) {
                return Collections.singletonList("<name>");
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setpublic")) {
            return Arrays.asList("true", "false").stream()
                .filter(s -> s.startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
