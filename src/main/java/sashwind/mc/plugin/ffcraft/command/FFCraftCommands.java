package sashwind.mc.plugin.ffcraft.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import sashwind.mc.plugin.ffcraft.lang.Messages;
import sashwind.mc.plugin.ffcraft.model.CreatePlayerRequest;
import sashwind.mc.plugin.ffcraft.model.ServerVideoPlayer;
import sashwind.mc.plugin.ffcraft.network.Networking;
import sashwind.mc.plugin.ffcraft.service.Permissions;
import sashwind.mc.plugin.ffcraft.service.VideoPlayerService;

import java.util.*;
import java.util.stream.Collectors;

public class FFCraftCommands implements CommandExecutor, TabCompleter {

    private final VideoPlayerService service;
    private final Networking networking;

    public FFCraftCommands(VideoPlayerService service, Networking networking) {
        this.service = service;
        this.networking = networking;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

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
                    sender.sendMessage(red("cmd.unknown_sub", sender, sub));
                    sendHelp(sender);
                }
            }
        } catch (SecurityException e) {
            sender.sendMessage(red("cmd.permission_denied", sender, e.getMessage()));
        } catch (NoSuchElementException e) {
            sender.sendMessage(red("cmd.not_found", sender, e.getMessage()));
        } catch (Exception e) {
            sender.sendMessage(red("cmd.error", sender, e.getMessage()));
        }
        return true;
    }

    // ── Handlers ──────────────────────────────────────

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(red("cmd.create.usage", sender)); return; }
        if (!(sender instanceof Player player)) { sender.sendMessage(red("cmd.player_only", sender)); return; }

        boolean isPublic = args.length >= 3 && args[2].equalsIgnoreCase("public");
        ServerVideoPlayer p = service.createPlayer(player, new CreatePlayerRequest(args[1], isPublic));
        networking.syncAll();
        sender.sendMessage(green("cmd.create.success", sender, p.name()));
    }

    private void handleList(CommandSender sender) {
        // Filter: hide private players the sender cannot view
        List<ServerVideoPlayer> visible = service.players().stream()
            .filter(p -> !(sender instanceof Player player) || Permissions.canView(player, p))
            .toList();
        if (visible.isEmpty()) { sender.sendMessage(yellow("cmd.list.empty", sender)); return; }

        sender.sendMessage(gold("cmd.list.header", sender, visible.size()));
        for (ServerVideoPlayer p : visible) {
            String vis = p.isPublic()
                ? ChatColor.GREEN + msg("word.public", sender)
                : ChatColor.RED   + msg("word.private", sender);
            String owner = Optional.ofNullable(Bukkit.getOfflinePlayer(p.creator()).getName())
                .orElse(msg("word.unknown", sender));
            sender.sendMessage(ChatColor.YELLOW + "  " + p.name()
                + ChatColor.GRAY   + " [" + vis + ChatColor.GRAY + "] "
                + ChatColor.AQUA   + p.playbackState().status().name()
                + ChatColor.DARK_GRAY + " owner:" + owner
                + ChatColor.DARK_GRAY + " (" + p.id().toString().substring(0, 8) + "...)");
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(red("cmd.info.usage", sender)); return; }

        UUID id = resolveUuid(args[1]);
        if (id == null) { sender.sendMessage(red("cmd.info.not_found", sender, args[1])); return; }

        ServerVideoPlayer p = service.findPlayer(id).orElse(null);
        if (p == null) { sender.sendMessage(red("cmd.info.not_found", sender, id)); return; }

        // Hide private players from unauthorized viewers
        if (sender instanceof Player player && !Permissions.canView(player, p)) {
            sender.sendMessage(red("cmd.not_found", sender, id)); return;
        }

        String owner = Optional.ofNullable(Bukkit.getOfflinePlayer(p.creator()).getName())
            .orElse(msg("word.unknown", sender));

        sender.sendMessage(gold("cmd.info.header", sender, p.name()));
        sender.sendMessage(gray("cmd.info.uuid", sender, p.id()));
        sender.sendMessage(gray("cmd.info.owner", sender, owner));
        sender.sendMessage(gray("cmd.info.public", sender, p.isPublic()));
        sender.sendMessage(gray("cmd.info.status", sender,
            p.playbackState().status(), p.playbackState().mode(), p.playbackState().volume()));
        sender.sendMessage(gray("cmd.info.progress", sender,
            p.playbackState().progressSeconds(), p.playbackState().currentIndex()));
        sender.sendMessage(gray("cmd.info.playlist", sender,
            p.playlist().size(), p.screens().size()));
        sender.sendMessage(gray("cmd.info.editors", sender,
            1, p.controlUsers().size()));
        for (UUID u : p.controlUsers()) {
            String name = Bukkit.getOfflinePlayer(u).getName();
            sender.sendMessage(ChatColor.DARK_GRAY + msg("cmd.info.control_user", sender,
                name != null ? name : u.toString().substring(0, 8)));
        }
    }

    private void handleRename(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage(red("cmd.rename.usage", sender)); return; }
        if (!(sender instanceof Player player)) { sender.sendMessage(red("cmd.player_only", sender)); return; }

        ServerVideoPlayer p = service.renamePlayer(player, UUID.fromString(args[1]), args[2]);
        networking.syncAll();
        sender.sendMessage(green("cmd.rename.success", sender, p.name()));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(red("cmd.delete.usage", sender)); return; }
        if (!(sender instanceof Player player)) { sender.sendMessage(red("cmd.player_only", sender)); return; }

        service.deletePlayer(player, UUID.fromString(args[1]));
        networking.syncAll();
        sender.sendMessage(green("cmd.delete.success", sender));
    }

    private void handleSetPublic(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage(red("cmd.setpublic.usage", sender)); return; }
        if (!(sender instanceof Player player)) { sender.sendMessage(red("cmd.player_only", sender)); return; }

        ServerVideoPlayer p = service.setPublic(player, UUID.fromString(args[1]), Boolean.parseBoolean(args[2]));
        networking.syncAll();
        String state = p.isPublic() ? msg("word.public", sender) : msg("word.private", sender);
        sender.sendMessage(green("cmd.setpublic.success", sender, p.name(), state));
    }

    private void handleGrant(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage(red("cmd.grant.usage", sender)); return; }
        if (!(sender instanceof Player player)) { sender.sendMessage(red("cmd.player_only", sender)); return; }

        UUID playerId = UUID.fromString(args[1]);
        UUID target   = Bukkit.getOfflinePlayer(args[2]).getUniqueId();
        try {
            ServerVideoPlayer p = service.grantControl(player, playerId, target);
            networking.syncAll();
            String name = Optional.ofNullable(Bukkit.getOfflinePlayer(target).getName()).orElse(args[2]);
            sender.sendMessage(green("cmd.grant.success", sender, p.name(), name));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(yellow("cmd.error", sender, e.getMessage()));
        }
    }

    private void handleRevoke(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage(red("cmd.revoke.usage", sender)); return; }
        if (!(sender instanceof Player player)) { sender.sendMessage(red("cmd.player_only", sender)); return; }

        UUID playerId = UUID.fromString(args[1]);
        UUID target   = Bukkit.getOfflinePlayer(args[2]).getUniqueId();
        try {
            ServerVideoPlayer p = service.revokeControl(player, playerId, target);
            networking.syncAll();
            String name = Optional.ofNullable(Bukkit.getOfflinePlayer(target).getName()).orElse(args[2]);
            sender.sendMessage(green("cmd.revoke.success", sender, p.name(), name));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(yellow("cmd.error", sender, e.getMessage()));
        }
    }

    private void handleReload(CommandSender sender) {
        if (sender instanceof Player player && !Permissions.canReload(player)) {
            sender.sendMessage(red("cmd.reload.no_perm", sender)); return;
        }
        networking.syncAll();
        sender.sendMessage(green("cmd.reload.success", sender));
    }

    // ── Helpers ───────────────────────────────────────

    private UUID resolveUuid(String input) {
        try { return UUID.fromString(input); } catch (IllegalArgumentException ignored) {}
        String partial = input.toLowerCase();
        return service.players().stream()
            .filter(p -> p.id().toString().toLowerCase().startsWith(partial))
            .map(ServerVideoPlayer::id).findFirst().orElse(null);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(gold("cmd.help.header", sender));
        sender.sendMessage(yellow("cmd.help.create", sender));
        sender.sendMessage(yellow("cmd.help.list", sender));
        sender.sendMessage(yellow("cmd.help.info", sender));
        sender.sendMessage(yellow("cmd.help.rename", sender));
        sender.sendMessage(yellow("cmd.help.delete", sender));
        sender.sendMessage(yellow("cmd.help.setpublic", sender));
        sender.sendMessage(yellow("cmd.help.grant", sender));
        sender.sendMessage(yellow("cmd.help.revoke", sender));
        sender.sendMessage(yellow("cmd.help.reload", sender));
    }

    // ── Shorthand wrappers ────────────────────────────

    private static String msg(String key, CommandSender sender, Object... args) {
        return Messages.get(key, sender, args);
    }
    private static String red(String key, CommandSender sender, Object... args) {
        return ChatColor.RED + msg(key, sender, args);
    }
    private static String green(String key, CommandSender sender, Object... args) {
        return ChatColor.GREEN + msg(key, sender, args);
    }
    private static String yellow(String key, CommandSender sender, Object... args) {
        return ChatColor.YELLOW + msg(key, sender, args);
    }
    private static String gold(String key, CommandSender sender, Object... args) {
        return ChatColor.GOLD + msg(key, sender, args);
    }
    private static String gray(String key, CommandSender sender, Object... args) {
        return ChatColor.GRAY + msg(key, sender, args);
    }

    // ── Tab complete ──────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("create","list","info","rename","delete","setpublic","grant","revoke","reload").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (List.of("info","rename","delete","setpublic","grant","revoke").contains(sub)) {
                return service.players().stream().map(p -> p.id().toString())
                    .filter(u -> u.startsWith(args[1])).collect(Collectors.toList());
            }
            if (sub.equals("create")) return List.of("<name>");
        }
        if (args.length == 3 && List.of("grant","revoke").contains(args[0].toLowerCase())) {
            UUID playerId = resolveUuid(args[1]);
            if (playerId == null) return List.of();
            ServerVideoPlayer vp = service.findPlayer(playerId).orElse(null);
            if (vp == null) return List.of();

            Set<UUID> controlSet = vp.controlUsers();
            UUID creator = vp.creator();

            if (args[0].equalsIgnoreCase("revoke")) {
                // Only players who currently have control
                return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> controlSet.contains(p.getUniqueId())
                        || Permissions.canNode(p, Permissions.controlPermission(vp.id())))
                    .filter(p -> !p.getUniqueId().equals(creator))
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
            } else {
                // grant: only players who DON'T have control yet
                return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !controlSet.contains(p.getUniqueId()))
                    .filter(p -> !p.getUniqueId().equals(creator))
                    .filter(p -> !Permissions.canNode(p, Permissions.controlPermission(vp.id())))
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setpublic")) return List.of("true","false");
        return List.of();
    }
}
