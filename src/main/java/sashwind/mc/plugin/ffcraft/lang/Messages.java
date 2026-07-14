package sashwind.mc.plugin.ffcraft.lang;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

/**
 * File-system based i18n for FFCraft.
 *
 * Directory layout (in plugins/FFCraft/):
 *   messages.properties          ← main language (Chinese), editable by server admin
 *   lang/
 *     messages_en_US.properties  ← English pack for players
 *     messages_ja_JP.properties  ← (future) Japanese pack
 *
 * Default files are copied from JAR on first run.
 * Players get the language matching their client locale;
 * falls back to messages.properties (Chinese) if no match.
 */
public final class Messages {

    private static Path dataFolder;
    private static Path langDir;
    private static final Map<String, Properties> cache = new HashMap<>();

    private Messages() {}

    /**
     * Scan JAR lang/ folder and copy all .properties files if missing, then load.
     */
    public static void init(JavaPlugin plugin) {
        dataFolder = plugin.getDataFolder().toPath();
        langDir   = dataFolder.resolve("lang");
        try { Files.createDirectories(langDir); }
        catch (IOException e) { plugin.getLogger().log(Level.WARNING, "Failed to create lang directory", e); return; }

        extractLangFiles(plugin);
        loadAll();
    }

    /** Scan the plugin location for lang/*.properties and copy them out. */
    private static void extractLangFiles(JavaPlugin plugin) {
        try {
            URL jarUrl = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
            File source = new File(jarUrl.toURI());

            if (source.isFile() && source.getName().endsWith(".jar")) {
                // Production: read from JAR
                try (JarFile jar = new JarFile(source)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.startsWith("lang/") && name.endsWith(".properties") && !entry.isDirectory()) {
                            copyEntry(jar, entry, name.substring("lang/".length()));
                        }
                    }
                }
            } else if (source.isDirectory()) {
                // Development (IDE): read from classes/lang/
                File langSource = new File(source, "lang");
                if (langSource.isDirectory()) {
                    File[] files = langSource.listFiles((d, n) -> n.endsWith(".properties"));
                    if (files != null) {
                        for (File f : files) {
                            copyFile(f, f.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to extract lang files", e);
        }
    }

    private static void copyEntry(JarFile jar, JarEntry entry, String fileName) throws IOException {
        // messages.properties → plugins/FFCraft/messages.properties (root)
        if ("messages.properties".equals(fileName)) {
            Path target = dataFolder.resolve(fileName);
            if (!Files.exists(target)) {
                try (InputStream in = jar.getInputStream(entry)) { Files.copy(in, target); }
            }
        }
        // Everything → plugins/FFCraft/lang/
        Path langTarget = langDir.resolve(fileName);
        if (!Files.exists(langTarget)) {
            try (InputStream in = jar.getInputStream(entry)) { Files.copy(in, langTarget); }
        }
    }

    private static void copyFile(File source, String fileName) throws IOException {
        if ("messages.properties".equals(fileName)) {
            Path target = dataFolder.resolve(fileName);
            if (!Files.exists(target)) Files.copy(source.toPath(), target);
        }
        Path langTarget = langDir.resolve(fileName);
        if (!Files.exists(langTarget)) Files.copy(source.toPath(), langTarget);
    }

    /** Reload all language files from disk. */
    public static void reload() {
        cache.clear();
        loadAll();
    }

    // ── public API ───────────────────────────────────

    public static String get(String key, CommandSender sender, Object... args) {
        return format(bundleFor(localeOf(sender)), key, args);
    }

    /** Server-side / console — always uses root messages.properties. */
    public static String get(String key, Object... args) {
        return format(cache.getOrDefault("messages", new Properties()), key, args);
    }

    // ── internals ────────────────────────────────────

    private static void loadAll() {
        // 1. Main language file: plugins/FFCraft/messages.properties
        Path main = dataFolder.resolve("messages.properties");
        cache.put("messages", loadProps(main));

        // 2. Language packs: plugins/FFCraft/lang/*.properties
        if (Files.isDirectory(langDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(langDir, "*.properties")) {
                for (Path file : stream) {
                    String name = file.getFileName().toString().replace(".properties", "");
                    cache.put(name, loadProps(file));
                }
            } catch (IOException ignored) {}
        }
    }

    private static Properties loadProps(Path file) {
        Properties p = new Properties();
        if (!Files.exists(file)) return p;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException ignored) {}
        return p;
    }

    private static Properties bundleFor(Locale locale) {
        // 1. messages_zh_CN (exact locale match from lang/)
        Properties p = cache.get("messages_" + locale.toString());
        if (notEmpty(p)) return p;

        // 2. messages_zh (language match from lang/)
        p = cache.get("messages_" + locale.getLanguage());
        if (notEmpty(p)) return p;

        // 3. Fallback: main messages.properties
        p = cache.get("messages");
        if (notEmpty(p)) return p;

        return new Properties();
    }

    private static boolean notEmpty(Properties p) {
        return p != null && !p.isEmpty();
    }

    private static String format(Properties p, String key, Object... args) {
        String pattern = p.getProperty(key);
        return pattern != null ? MessageFormat.format(pattern, args) : "!" + key + "!";
    }

    private static Locale localeOf(CommandSender sender) {
        if (sender instanceof Player player) {
            String loc = player.getLocale();
            if (loc != null && loc.contains("_")) {
                String[] parts = loc.split("_", 2);
                return new Locale(parts[0], parts[1]);
            }
            return loc != null ? new Locale(loc) : Locale.getDefault();
        }
        return Locale.getDefault();
    }
}
