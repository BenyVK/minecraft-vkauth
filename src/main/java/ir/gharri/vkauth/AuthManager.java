package ir.gharri.vkauth;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class AuthManager {

    private final Vkauth plugin;
    private final ConfigVk cfg;
    private final StorageManager storage;
    private final DailyCounters counters;

    private final Map<UUID, Long> pending = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastReminder = new ConcurrentHashMap<>();
    private final Map<UUID, Account> accounts = new ConcurrentHashMap<>();
    private final Map<UUID, Location> origin = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> attempts = new ConcurrentHashMap<>();
    private final Map<UUID, TextDisplay> notes = new ConcurrentHashMap<>();
    private final Set<UUID> loaded = ConcurrentHashMap.newKeySet();
    private final Set<UUID> busy = ConcurrentHashMap.newKeySet();

    public AuthManager(Vkauth plugin, ConfigVk cfg, StorageManager storage) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.storage = storage;
        this.counters = new DailyCounters(plugin, cfg);
    }

    public boolean isLocked(Player player) {
        return pending.containsKey(player.getUniqueId());
    }


    public void handleJoin(Player player) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        clear(id);
        pending.put(id, now);
        lastReminder.put(id, now);
        origin.put(id, player.getLocation().clone());

        Location waiting = cfg.authLocation();
        if (waiting != null) player.teleport(waiting);

        if (cfg.hidePlayers()) {
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.equals(player)) continue;
                player.hidePlayer(plugin, other);
                if (pending.containsKey(other.getUniqueId())) {
                    other.hidePlayer(plugin, player);
                }
            }
        }

        String name = player.getName();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Account account = null;
            Exception error = null;
            try {
                account = storage.load(id, name);
            } catch (Exception e) {
                error = e;
            }
            final Account found = account;
            final Exception foundError = error;
            Bukkit.getScheduler().runTask(plugin, () -> onLoaded(id, found, foundError));
        });
    }

    private void onLoaded(UUID id, Account account, Exception error) {
        Player player = Bukkit.getPlayer(id);
        if (player == null || !pending.containsKey(id)) return;

        if (error != null) {
            plugin.getLogger().log(Level.SEVERE, "Could not load account of " + player.getName(), error);
            player.kick(cfg.msg("storage-error"));
            return;
        }
        if (account != null) accounts.put(id, account);
        loaded.add(id);

        applyTabName(player, account != null);
        spawnNote(player);
        sendPrompt(player);
    }

    public void handleQuit(Player player) {
        UUID id = player.getUniqueId();
        if (pending.containsKey(id) && cfg.authLocation() != null) {
            Location back = origin.get(id);
            if (back != null) player.teleport(back);
        }
        clear(id);
    }

    public void shutdown() {
        for (Player p : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            UUID id = p.getUniqueId();
            if (!pending.containsKey(id)) continue;
            Location back = origin.get(id);
            if (cfg.authLocation() != null && back != null) p.teleport(back);
            p.playerListName(null);
            showEverybody(p);
        }
        for (UUID id : new ArrayList<>(notes.keySet())) removeNote(id);
    }

    private void clear(UUID id) {
        pending.remove(id);
        lastReminder.remove(id);
        accounts.remove(id);
        origin.remove(id);
        attempts.remove(id);
        loaded.remove(id);
        busy.remove(id);
        removeNote(id);
    }

    private void showEverybody(Player player) {
        if (!cfg.hidePlayers()) return;
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(player)) player.showPlayer(plugin, other);
        }
    }

    /** LOGIN_USER_n / REGISTER_USER_n, n = how many logins / registrations started today. */
    private void applyTabName(Player player, boolean registered) {
        if (!cfg.tabNames()) return;
        int number = registered ? counters.nextLogin() : counters.nextRegister();
        String format = registered ? cfg.tabLoginFormat() : cfg.tabRegisterFormat();
        player.playerListName(cfg.format(format, "number", String.valueOf(number)));
    }

    /** A TextDisplay floating in front of the player, visible only to that player. */
    private void spawnNote(Player player) {
        if (!cfg.text3d()) return;
        String raw = cfg.text3dNote();
        if (raw == null || raw.isBlank()) return;

        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().setY(0);
        if (dir.lengthSquared() < 1.0E-6) dir = new Vector(0, 0, 1);
        dir.normalize().multiply(cfg.text3dDistance());
        Location at = eye.clone().add(dir).add(0, cfg.text3dHeight(), 0);

        Component text = cfg.format(raw, "player", player.getName());
        float scale = cfg.text3dScale();

        TextDisplay display = player.getWorld().spawn(at, TextDisplay.class, d -> {
            d.setVisibleByDefault(false);
            d.setPersistent(false);
            d.text(text);
            d.setBillboard(Display.Billboard.CENTER);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setTransformation(new Transformation(
                    new Vector3f(), new AxisAngle4f(), new Vector3f(scale, scale, scale), new AxisAngle4f()));
        });
        player.showEntity(plugin, display);
        notes.put(player.getUniqueId(), display);
    }

    private void removeNote(UUID id) {
        TextDisplay display = notes.remove(id);
        if (display != null && display.isValid()) display.remove();
    }

    public void register(Player player, String[] args) {
        UUID id = player.getUniqueId();

        if (!pending.containsKey(id)) {
            player.sendMessage(cfg.msg("already-logged-in"));
            return;
        }
        if (!loaded.contains(id) || busy.contains(id)) {
            player.sendMessage(cfg.msg("wait-loading"));
            return;
        }
        if (accounts.containsKey(id)) {
            player.sendMessage(cfg.msg("already-registered"));
            return;
        }
        if (args.length != 2) {
            player.sendMessage(cfg.msg("register-usage"));
            return;
        }
        String password = args[0];
        if (!password.equals(args[1])) {
            player.sendMessage(cfg.msg("password-mismatch"));
            return;
        }
        if (password.length() < cfg.minPassword()) {
            player.sendMessage(cfg.msg("password-too-short", "min", String.valueOf(cfg.minPassword())));
            return;
        }
        if (password.length() > cfg.maxPassword()) {
            player.sendMessage(cfg.msg("password-too-long", "max", String.valueOf(cfg.maxPassword())));
            return;
        }

        busy.add(id);
        String name = player.getName();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            Account account = new Account(PasswordHasher.hash(password), now, now);
            Exception error = null;
            try {
                storage.save(id, name, account);
            } catch (Exception e) {
                error = e;
            }
            final Exception saveError = error;
            Bukkit.getScheduler().runTask(plugin, () -> {
                busy.remove(id);
                Player p = Bukkit.getPlayer(id);
                if (p == null || !pending.containsKey(id)) return;
                if (saveError != null) {
                    plugin.getLogger().log(Level.SEVERE, "Could not save account of " + name, saveError);
                    p.sendMessage(cfg.msg("storage-error"));
                    return;
                }
                accounts.put(id, account);
                authenticate(p, true);
                p.sendMessage(cfg.msg("register-success"));
            });
        });
    }

    public void login(Player player, String[] args) {
        UUID id = player.getUniqueId();

        if (!pending.containsKey(id)) {
            player.sendMessage(cfg.msg("already-logged-in"));
            return;
        }
        if (!loaded.contains(id) || busy.contains(id)) {
            player.sendMessage(cfg.msg("wait-loading"));
            return;
        }
        Account account = accounts.get(id);
        if (account == null) {
            player.sendMessage(cfg.msg("not-registered"));
            return;
        }
        if (args.length != 1) {
            player.sendMessage(cfg.msg("login-usage"));
            return;
        }

        String password = args[0];
        String hash = account.hash();
        long previousLogin = account.lastLogin() > 0 ? account.lastLogin() : account.registered();
        busy.add(id);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean ok = PasswordHasher.verify(password, hash);
            Bukkit.getScheduler().runTask(plugin, () -> {
                busy.remove(id);
                Player p = Bukkit.getPlayer(id);
                if (p == null || !pending.containsKey(id)) return;

                if (ok) {
                    authenticate(p, false);
                    p.sendMessage(cfg.msg("login-success"));
                    p.sendMessage(cfg.msg("last-login", "date", cfg.formatTime(previousLogin)));
                    saveLastLogin(id);
                    return;
                }
                int tries = attempts.merge(id, 1, Integer::sum);
                int max = cfg.maxAttempts();
                if (max > 0 && tries >= max) {
                    p.kick(cfg.msg("too-many-attempts"));
                } else {
                    p.sendMessage(cfg.msg("wrong-password",
                            "attempts", String.valueOf(tries),
                            "max", String.valueOf(max),
                            "left", max > 0 ? String.valueOf(max - tries) : "-"));
                }
            });
        });
    }

    private void saveLastLogin(UUID id) {
        long now = System.currentTimeMillis();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> storage.updateLastLogin(id, now));
    }

    /**
     * @param firstTime true right after /register, false for a normal /login.
     *                  With start_auth_all_replay=false the fixed start position is used only when firstTime is true.
     */
    private void authenticate(Player player, boolean firstTime) {
        UUID id = player.getUniqueId();
        pending.remove(id);
        lastReminder.remove(id);
        attempts.remove(id);
        removeNote(id);
        player.playerListName(null);
        showEverybody(player);
        Location previous = origin.remove(id);

        Location target = null;
        if (cfg.startReplay() || firstTime) {
            target = cfg.startLocation(); // null if start_auth_all is false
        }
        if (target == null && cfg.authLocation() != null) {
            target = previous;
        }
        if (target != null) player.teleport(target);
    }

    public void startTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            int timeout = cfg.timeoutSeconds();
            int interval = cfg.reminderSeconds();

            for (Player p : new ArrayList<>(Bukkit.getOnlinePlayers())) {
                UUID id = p.getUniqueId();
                Long joined = pending.get(id);
                if (joined == null) continue;

                if (timeout > 0 && (now - joined) / 1000 >= timeout) {
                    p.kick(cfg.msg("timeout-kick", "seconds", String.valueOf(timeout)));
                    continue;
                }
                Long last = lastReminder.get(id);
                if (interval > 0 && loaded.contains(id) && last != null && now - last >= interval * 1000L) {
                    lastReminder.put(id, now);
                    sendPrompt(p);
                }
            }
        }, 20L, 20L);
    }

    private void sendPrompt(Player p) {
        String key = accounts.containsKey(p.getUniqueId()) ? "must-login" : "must-register";
        p.sendMessage(cfg.msg(key, "player", p.getName()));
    }
}
