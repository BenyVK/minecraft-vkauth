package ir.gharri.vkauth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private Storage create(String type) {
        if (type.equals("mysql")) {
            return new MysqlStorage(
                    cfg.str("storage.mysql.host"),
                    cfg.integer("storage.mysql.port"),
                    cfg.str("storage.mysql.database"),
                    cfg.str("storage.mysql.username"),
                    cfg.str("storage.mysql.password"),
                    cfg.str("storage.mysql.table"));
        }
        return new YamlStorage(plugin.getDataFolder());
    }

    public boolean init() {
        List<String> types = cfg.storageTypes();
        for (String type : types) {
            Storage s = create(type);
            try {
                s.init();
                storages.add(s);
                plugin.getLogger().info("Storage enabled: " + s.name());
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not start storage '" + type + "': " + e.getMessage());
            }
        }
        if (storages.isEmpty()) return false;

        if (cfg.migrate()) {
            try {
                migrate(types, storages.size() == types.size());
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Migration failed: " + e.getMessage());
            }
        }
        return true;
    }

    /**
     * 1) Accounts of a storage that is NOT enabled any more are copied into every enabled storage
     *    and the old storage is deleted.
     * 2) When several storages are enabled they are synced (missing accounts are copied).
     */
    private void migrate(List<String> enabled, boolean allStarted) {
        for (String type : List.of("yaml", "mysql")) {
            if (enabled.contains(type)) continue;

            Storage old = create(type);
            List<StoredAccount> accounts;
            try {
                accounts = old.getAll();
            } catch (Exception e) {
                plugin.getLogger().info("Migration: could not read old " + type + " storage (" + e.getMessage()
                        + "), skipped.");
                continue;
            }
            if (accounts.isEmpty()) continue;

            plugin.getLogger().info("Migration: found " + accounts.size() + " account(s) in old " + type
                    + " storage, moving them...");
            boolean allOk = true;
            for (Storage target : storages) {
                if (!copyMissing(accounts, target)) allOk = false;
            }

            if (allOk && allStarted && cfg.deleteOldAfterMigrate()) {
                try {
                    old.deleteAll();
                    plugin.getLogger().info("Migration: old " + type + " storage deleted.");
                } catch (Exception e) {
                    plugin.getLogger().warning("Migration: could not delete old " + type + " storage: "
                            + e.getMessage());
                }
            } else if (!allOk || !allStarted) {
                plugin.getLogger().warning("Migration: old " + type
                        + " storage was NOT deleted because the transfer was not fully successful.");
            }
        }

        if (storages.size() > 1) {
            Map<UUID, StoredAccount> merged = new LinkedHashMap<>();
            for (Storage s : storages) {
                try {
                    for (StoredAccount a : s.getAll()) {
                        merged.merge(a.uuid(), a,
                                (x, y) -> y.account().lastLogin() > x.account().lastLogin() ? y : x);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Sync: could not read " + s.name() + ": " + e.getMessage());
                }
            }
            List<StoredAccount> all = new ArrayList<>(merged.values());
            for (Storage s : storages) copyMissing(all, s);
        }
    }

    /** Copies accounts that the target does not have yet. Returns false if anything failed. */
    private boolean copyMissing(List<StoredAccount> source, Storage target) {
        Set<UUID> have = new HashSet<>();
        try {
            for (StoredAccount a : target.getAll()) have.add(a.uuid());
        } catch (Exception e) {
            plugin.getLogger().warning("Migration: could not read " + target.name() + ": " + e.getMessage());
            return false;
        }

        boolean ok = true;
        int copied = 0;
        for (StoredAccount a : source) {
            if (have.contains(a.uuid())) continue;
            try {
                target.saveAccount(a.uuid(), a.name(), a.account());
                copied++;
            } catch (Exception e) {
                ok = false;
                plugin.getLogger().warning("Migration: could not save " + a.uuid() + " to " + target.name()
                        + ": " + e.getMessage());
            }
        }
        if (copied > 0) {
            plugin.getLogger().info("Migration: " + copied + " account(s) copied to " + target.name());
        }
        return ok;
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
