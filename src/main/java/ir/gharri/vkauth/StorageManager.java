package ir.gharri.vkauth;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/** Talks to every enabled storage. Reads from the first that has the account, writes to all. */
public final class StorageManager {

    private final Vkauth plugin;
    private final ConfigVk cfg;
    private final List<Storage> storages = new ArrayList<>();

    public StorageManager(Vkauth plugin, ConfigVk cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
    }

    public boolean init() {
        for (String type : cfg.storageTypes()) {
            Storage s;
            if (type.equals("mysql")) {
                s = new MysqlStorage(
                        cfg.str("storage.mysql.host"),
                        cfg.integer("storage.mysql.port"),
                        cfg.str("storage.mysql.database"),
                        cfg.str("storage.mysql.username"),
                        cfg.str("storage.mysql.password"),
                        cfg.str("storage.mysql.table"));
            } else {
                s = new YamlStorage(plugin.getDataFolder());
            }
            try {
                s.init();
                storages.add(s);
                plugin.getLogger().info("Storage enabled: " + s.name());
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not start storage '" + type + "': " + e.getMessage());
            }
        }
        return !storages.isEmpty();
    }

    /** Blocking - call from an async thread. Returns the account or null if it does not exist. */
    public Account load(UUID uuid, String name) throws Exception {
        Exception error = null;
        for (int i = 0; i < storages.size(); i++) {
            Storage s = storages.get(i);
            try {
                Account account = s.getAccount(uuid);
                if (account != null) {
                    for (int j = 0; j < i; j++) {
                        try {
                            storages.get(j).saveAccount(uuid, name, account);
                        } catch (Exception ignored) {
                        }
                    }
                    return account;
                }
            } catch (Exception e) {
                error = e;
                plugin.getLogger().log(Level.WARNING, "Read failed on " + s.name() + ": " + e.getMessage());
            }
        }
        if (error != null) throw error;
        return null;
    }

    /** Blocking - call from an async thread. Succeeds if at least one storage saved. */
    public void save(UUID uuid, String name, Account account) throws Exception {
        int ok = 0;
        Exception last = null;
        for (Storage s : storages) {
            try {
                s.saveAccount(uuid, name, account);
                ok++;
            } catch (Exception e) {
                last = e;
                plugin.getLogger().log(Level.WARNING, "Write failed on " + s.name() + ": " + e.getMessage());
            }
        }
        if (ok == 0 && last != null) throw last;
    }

    /** Blocking - call from an async thread. Stores the last login time in every storage. */
    public void updateLastLogin(UUID uuid, long time) {
        for (Storage s : storages) {
            try {
                s.updateLastLogin(uuid, time);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Last-login update failed on " + s.name() + ": " + e.getMessage());
            }
        }
    }

    public void close() {
        for (Storage s : storages) s.close();
    }
}
