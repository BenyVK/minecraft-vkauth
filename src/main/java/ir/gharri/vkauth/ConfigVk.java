package ir.gharri.vkauth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Reads configvk.yml (settings + every message shown to players). */
public final class ConfigVk {

    public record Pos(double x, double y, double z, float yaw) {
        public Location toLocation(World world) {
            return new Location(world, x, y, z, yaw, 0f);
        }
    }

    private final Vkauth plugin;
    private YamlConfiguration cfg;
    private Pos authPos;
    private Pos startPos;

    public ConfigVk(Vkauth plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        File file = new File(plugin.getDataFolder(), "configvk.yml");
        if (!file.exists()) plugin.saveResource("configvk.yml", false);
        cfg = YamlConfiguration.loadConfiguration(file);

        try (InputStream in = plugin.getResource("configvk.yml")) {
            if (in != null) {
                cfg.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (IOException ignored) {
        }

        authPos = parsePos("pos-auth-start");
        startPos = parsePos("start_auth_pos");
    }

    private Pos parsePos(String key) {
        String raw = cfg.getString(key, "");
        if (raw == null || raw.isBlank()) return null;
        String[] p = raw.split(",");
        if (p.length < 3) {
            plugin.getLogger().warning("configvk.yml: '" + key + "' must look like X,Y,Z,A");
            return null;
        }
        try {
            double x = Double.parseDouble(p[0].trim());
            double y = Double.parseDouble(p[1].trim());
            double z = Double.parseDouble(p[2].trim());
            float yaw = p.length > 3 ? Float.parseFloat(p[3].trim()) : 0f;
            return new Pos(x, y, z, yaw);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("configvk.yml: '" + key + "' has an invalid number");
            return null;
        }
    }

    private World world(String key) {
        World w = Bukkit.getWorld(cfg.getString(key, "world"));
        return w != null ? w : Bukkit.getWorlds().get(0);
    }

    /** Login waiting room, or null when auth-map is false / invalid. */
    public Location authLocation() {
        if (!cfg.getBoolean("auth-map") || authPos == null) return null;
        return authPos.toLocation(world("pos-auth-world"));
    }

    /** Fixed spawn after login/register, or null when start_auth_all is false / invalid. */
    public Location startLocation() {
        if (!cfg.getBoolean("start_auth_all") || startPos == null) return null;
        return startPos.toLocation(world("start_auth_world"));
    }

    /** true = spawn at start pos on every login, false = only after the first registration. */
    public boolean startReplay() {
        return cfg.getBoolean("start_auth_all_replay", true);
    }

    public List<String> storageTypes() {
        Set<String> out = new LinkedHashSet<>();
        for (String s : cfg.getString("storage.type", "yaml").split("[,;\\s]+")) {
            s = s.trim().toLowerCase(Locale.ROOT);
            if (s.equals("yaml") || s.equals("mysql")) out.add(s);
        }
        if (out.isEmpty()) out.add("yaml");
        return new ArrayList<>(out);
    }

    public boolean migrate() { return cfg.getBoolean("storage.migrate", true); }
    public boolean deleteOldAfterMigrate() { return cfg.getBoolean("storage.delete-old-after-migrate", true); }

    public String str(String path) {
        return cfg.getString(path, "");
    }

    public int integer(String path) {
        return cfg.getInt(path);
    }

    public int minPassword() { return cfg.getInt("security.min-password-length", 6); }
    public int maxPassword() { return cfg.getInt("security.max-password-length", 32); }
    public int maxAttempts() { return cfg.getInt("security.max-login-attempts", 5); }
    public int timeoutSeconds() { return cfg.getInt("security.login-timeout-seconds", 60); }
    public int reminderSeconds() { return cfg.getInt("security.reminder-interval-seconds", 10); }

    /** Players that are not logged in do not see other players. */
    public boolean hidePlayers() { return cfg.getBoolean("auth_hide_players", false); }

    public boolean text3d() { return cfg.getBoolean("text3d", false); }
    public String text3dNote() { return cfg.getString("text3d_note", ""); }
    public float text3dScale() { return (float) Math.max(0.1, cfg.getDouble("text3d_scale", 1.5)); }
    public double text3dDistance() { return Math.max(0.5, cfg.getDouble("text3d_distance", 2.5)); }
    public double text3dHeight() { return cfg.getDouble("text3d_height", 0.3); }

    public boolean tabNames() { return cfg.getBoolean("tab_names", true); }
    public String tabLoginFormat() { return cfg.getString("tab_login_format", "LOGIN_USER_{number}"); }
    public String tabRegisterFormat() { return cfg.getString("tab_register_format", "REGISTER_USER_{number}"); }

    public ZoneId zone() {
        String tz = cfg.getString("time-zone", "");
        if (tz == null || tz.isBlank()) return ZoneId.systemDefault();
        try {
            return ZoneId.of(tz.trim());
        } catch (DateTimeException e) {
            plugin.getLogger().warning("configvk.yml: invalid time-zone '" + tz + "', using the server default");
            return ZoneId.systemDefault();
        }
    }

    /** Formats epoch millis with last-login-format; 0 or less gives messages.last-login-none. */
    public String formatTime(long millis) {
        if (millis <= 0) return cfg.getString("messages.last-login-none", "-");
        DateTimeFormatter fmt;
        try {
            fmt = DateTimeFormatter.ofPattern(cfg.getString("last-login-format", "yyyy-MM-dd HH:mm:ss"));
        } catch (IllegalArgumentException e) {
            fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        }
        return fmt.withZone(zone()).format(Instant.ofEpochMilli(millis));
    }

    /** Colors (&) + {prefix} + placeholders -> Component. */
    public Component format(String text, String... pairs) {
        text = text.replace("{prefix}", cfg.getString("messages.prefix", ""));
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            text = text.replace("{" + pairs[i] + "}", pairs[i + 1]);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    /** msg("key", "placeholder", "value", ...) */
    public Component msg(String key, String... pairs) {
        return format(cfg.getString("messages." + key, "&c[vkauth] missing message: " + key), pairs);
    }
}
